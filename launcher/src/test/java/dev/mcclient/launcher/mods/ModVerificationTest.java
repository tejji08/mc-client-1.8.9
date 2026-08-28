package dev.mcclient.launcher.mods;

import dev.mcclient.launcher.Progress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The security property this whole project rests on:
 * <strong>a jar whose sha256 does not match the manifest never reaches the game directory.</strong>
 *
 * <p>Verified once by hand against a real download, which proved nothing after the fact. These are
 * the regression tests that keep it true.
 */
class ModVerificationTest {

    private static final String JAR_BODY = "pretend this is a mod jar";
    /** sha256 of JAR_BODY, computed by the same code the launcher trusts at runtime. */
    private static String hashOf(Path file) throws IOException {
        return Hashing.sha256(file);
    }

    private ModManager managerFor(Path store, ModEntry entry, Path stateFile) throws IOException {
        ModManifest manifest = ModManifest.parse(manifestJson(entry), "test");
        return new ModManager(HttpClient.newHttpClient(), manifest, new ModState(stateFile), store);
    }

    private String manifestJson(ModEntry entry) {
        return "{\"schemaVersion\":1,\"minecraftVersion\":\"1.8.9\",\"loader\":\"legacy-fabric\",\"mods\":["
                + "{\"id\":\"" + entry.id() + "\",\"name\":\"" + entry.name() + "\",\"version\":\"" + entry.version()
                + "\",\"description\":\"d\",\"category\":\"utility\",\"url\":\"https://example.invalid/x.jar\","
                + "\"sha256\":\"" + entry.sha256() + "\",\"sizeBytes\":1,\"sourceUrl\":\"\",\"license\":\"MIT\","
                + "\"required\":false,\"enabledByDefault\":true}]}";
    }

    private ModEntry entryWith(String sha256) {
        return new ModEntry("testmod", "Test Mod", "1.0", "d", "utility",
                "https://example.invalid/x.jar", sha256, 1L, "", "MIT", false, true);
    }

    @Test
    void anUnpinnedModCannotEvenBeConstructed(@TempDir Path dir) {
        // The pinned bundle is the entire security story; an entry without a hash is a hole in it.
        assertThrows(IllegalArgumentException.class, () -> entryWith(null));
        assertThrows(IllegalArgumentException.class, () -> entryWith(""));
        assertThrows(IllegalArgumentException.class, () -> entryWith("not-a-hash"));
        assertThrows(IllegalArgumentException.class, () -> entryWith("abc123"), "too short to be sha256");
    }

    @Test
    void aMatchingJarVerifies(@TempDir Path dir) throws IOException {
        Path store = dir.resolve("store");
        Files.createDirectories(store);
        Path jar = store.resolve("testmod-1.0.jar");
        Files.writeString(jar, JAR_BODY, StandardCharsets.UTF_8);

        ModEntry entry = entryWith(hashOf(jar));
        ModManager manager = managerFor(store, entry, dir.resolve("state.json"));

        assertEquals(ModStatus.VERIFIED, manager.resolve(entry).status());
    }

    @Test
    void aTamperedJarIsReportedCorrupt(@TempDir Path dir) throws IOException {
        Path store = dir.resolve("store");
        Files.createDirectories(store);
        Path jar = store.resolve("testmod-1.0.jar");
        Files.writeString(jar, JAR_BODY, StandardCharsets.UTF_8);
        ModEntry entry = entryWith(hashOf(jar));
        ModManager manager = managerFor(store, entry, dir.resolve("state.json"));

        Files.writeString(jar, JAR_BODY + " but altered", StandardCharsets.UTF_8);

        assertEquals(ModStatus.CORRUPT, manager.resolve(entry).status(),
                "re-checked on every launch, not trusted from the last one");
    }

    @Test
    void aTamperedJarNeverReachesTheGameDirectory(@TempDir Path dir) throws IOException {
        Path store = dir.resolve("store");
        Files.createDirectories(store);
        Path jar = store.resolve("testmod-1.0.jar");
        Files.writeString(jar, JAR_BODY, StandardCharsets.UTF_8);
        ModEntry entry = entryWith(hashOf(jar));
        ModManager manager = managerFor(store, entry, dir.resolve("state.json"));

        Path gameMods = dir.resolve("game").resolve("mods");
        manager.syncToGameDir(gameMods, Progress.SILENT);
        assertTrue(Files.exists(gameMods.resolve("testmod-1.0.jar")), "a good jar is installed");

        Files.writeString(jar, "malicious payload", StandardCharsets.UTF_8);
        manager.syncToGameDir(gameMods, Progress.SILENT);

        assertFalse(Files.exists(gameMods.resolve("testmod-1.0.jar")),
                "the tampered jar must be removed, not merely left unrefreshed");
    }

    @Test
    void filesTheUserDroppedInAreLeftAlone(@TempDir Path dir) throws IOException {
        Path store = dir.resolve("store");
        Files.createDirectories(store);
        Path jar = store.resolve("testmod-1.0.jar");
        Files.writeString(jar, JAR_BODY, StandardCharsets.UTF_8);
        ModEntry entry = entryWith(hashOf(jar));
        ModManager manager = managerFor(store, entry, dir.resolve("state.json"));

        Path gameMods = dir.resolve("game").resolve("mods");
        Files.createDirectories(gameMods);
        Path handDropped = gameMods.resolve("someone-elses-mod.jar");
        Files.writeString(handDropped, "not ours", StandardCharsets.UTF_8);

        manager.syncToGameDir(gameMods, Progress.SILENT);

        assertTrue(Files.exists(handDropped), "the launcher only cleans up jars it placed itself");
    }

    @Test
    void aDisabledModIsRemovedFromTheGameDirectory(@TempDir Path dir) throws IOException {
        Path store = dir.resolve("store");
        Files.createDirectories(store);
        Path jar = store.resolve("testmod-1.0.jar");
        Files.writeString(jar, JAR_BODY, StandardCharsets.UTF_8);
        ModEntry entry = entryWith(hashOf(jar));
        ModManager manager = managerFor(store, entry, dir.resolve("state.json"));

        Path gameMods = dir.resolve("game").resolve("mods");
        manager.syncToGameDir(gameMods, Progress.SILENT);
        assertTrue(Files.exists(gameMods.resolve("testmod-1.0.jar")));

        manager.setEnabled(entry, false);
        manager.syncToGameDir(gameMods, Progress.SILENT);

        assertFalse(Files.exists(gameMods.resolve("testmod-1.0.jar")));
    }

    @Test
    void duplicateIdsAreRejected() {
        ModEntry entry = entryWith("a".repeat(64));
        String json = manifestJson(entry).replace("\"mods\":[", "\"mods\":[" + oneEntry(entry) + ",");

        assertThrows(IOException.class, () -> ModManifest.parse(json, "test"),
                "two entries with one id would make which jar wins ambiguous");
    }

    private String oneEntry(ModEntry entry) {
        return "{\"id\":\"" + entry.id() + "\",\"name\":\"x\",\"version\":\"1.0\",\"description\":\"d\","
                + "\"category\":\"utility\",\"url\":\"https://example.invalid/x.jar\",\"sha256\":\""
                + entry.sha256() + "\",\"sizeBytes\":1,\"sourceUrl\":\"\",\"license\":\"MIT\","
                + "\"required\":false,\"enabledByDefault\":true}";
    }
}
