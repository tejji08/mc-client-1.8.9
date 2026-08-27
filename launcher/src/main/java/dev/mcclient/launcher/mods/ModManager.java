package dev.mcclient.launcher.mods;

import dev.mcclient.launcher.LauncherPaths;
import dev.mcclient.launcher.Progress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Owns the mod bundle: what is available, what is installed, what verified, and what actually gets
 * handed to the game.
 *
 * <p>The rule the whole design turns on: <strong>a jar whose sha256 does not match the manifest
 * never reaches the game directory.</strong> It is checked on download and re-checked on every
 * launch, because "verified once, months ago" is not the same claim as "verified now".
 */
public final class ModManager {

    static final String DEV_CATEGORY = "dev";
    private static final String DEV_PREFIX = "dev-";

    private final HttpClient http;
    private final ModManifest manifest;
    private final ModState state;
    private final Path store;

    public ModManager(HttpClient http) throws IOException {
        this(http, ModManifest.load(), new ModState(), LauncherPaths.mods());
    }

    ModManager(HttpClient http, ModManifest manifest, ModState state, Path store) {
        this.http = http;
        this.manifest = manifest;
        this.state = state;
        this.store = store;
    }

    public ModManifest manifest() {
        return manifest;
    }

    /** Every catalog mod resolved against disk, followed by any locally built dev jars. */
    public List<InstalledMod> resolveAll() {
        List<InstalledMod> result = new ArrayList<>();
        for (ModEntry entry : manifest.mods()) {
            result.add(resolve(entry));
        }
        result.addAll(discoverDevMods());
        return result;
    }

    public InstalledMod resolve(ModEntry entry) {
        Path jar = store.resolve(entry.fileName());
        boolean enabled = state.isEnabled(entry);
        if (!Files.exists(jar)) {
            return new InstalledMod(entry, ModStatus.NOT_INSTALLED, null, enabled);
        }
        try {
            String actual = Hashing.sha256(jar);
            ModStatus status = Hashing.matches(entry.sha256(), actual) ? ModStatus.VERIFIED : ModStatus.CORRUPT;
            return new InstalledMod(entry, status, jar, enabled);
        } catch (IOException e) {
            return new InstalledMod(entry, ModStatus.CORRUPT, jar, enabled);
        }
    }

    public void setEnabled(ModEntry entry, boolean enabled) throws IOException {
        state.setEnabled(entry, enabled);
    }

    /**
     * Downloads a mod and installs it only if the hash matches. A mismatch leaves the store
     * untouched and throws -- a jar that failed verification is never written into place, so a bad
     * download cannot linger and get picked up later.
     */
    public void install(ModEntry entry, Progress progress) throws IOException, InterruptedException {
        progress.status("Downloading " + entry.name() + " " + entry.displayVersion() + "...");
        Files.createDirectories(store);
        Path temp = Files.createTempFile(store, entry.id(), ".partial");
        try {
            download(entry.url(), temp, entry.sizeBytes(), progress);

            String actual = Hashing.sha256(temp);
            if (!Hashing.matches(entry.sha256(), actual)) {
                throw new IOException("Refusing to install " + entry.id()
                        + ": sha256 mismatch."
                        + "\n  expected " + entry.sha256()
                        + "\n  got      " + actual);
            }
            progress.status("Verified " + entry.name() + " (sha256 ok)");
            Files.move(temp, store.resolve(entry.fileName()), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private void download(String url, Path dest, long expectedSize, Progress progress)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Download failed (HTTP " + response.statusCode() + "): " + url);
        }
        long total = response.headers().firstValueAsLong("content-length").orElse(expectedSize);
        long done = 0;
        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(dest)) {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                done += read;
                progress.bytes(done, total);
            }
        }
    }

    /** Installs every enabled catalog mod that is not already verified on disk. */
    public void installMissing(Progress progress) throws IOException, InterruptedException {
        for (InstalledMod mod : resolveAll()) {
            if (DEV_CATEGORY.equals(mod.entry().category()) || !mod.enabled()) {
                continue;
            }
            if (mod.status() == ModStatus.NOT_INSTALLED || mod.status() == ModStatus.CORRUPT) {
                if (mod.status() == ModStatus.CORRUPT) {
                    progress.status("Re-downloading " + mod.name() + " -- failed verification");
                }
                install(mod.entry(), progress);
            }
        }
    }

    /**
     * Makes the game's mods folder exactly match what should be running: every launchable mod
     * copied in, and anything the launcher previously placed there but no longer wants removed.
     * Files the user dropped in by hand are left alone.
     */
    public List<InstalledMod> syncToGameDir(Path gameModsDir, Progress progress) throws IOException {
        Files.createDirectories(gameModsDir);
        List<InstalledMod> resolved = resolveAll();

        Set<String> wanted = new HashSet<>();
        for (InstalledMod mod : resolved) {
            if (mod.willLaunch() && mod.jar() != null) {
                wanted.add(mod.entry().fileName());
            }
        }

        // Drop stale managed jars (disabled, removed from the manifest, or newly failing verification).
        Set<String> managed = new HashSet<>();
        for (ModEntry entry : manifest.mods()) {
            managed.add(entry.fileName());
        }
        try (var existing = Files.list(gameModsDir)) {
            for (Path path : (Iterable<Path>) existing::iterator) {
                String fileName = path.getFileName().toString();
                boolean isOurs = managed.contains(fileName) || fileName.startsWith(DEV_PREFIX);
                if (isOurs && !wanted.contains(fileName)) {
                    Files.deleteIfExists(path);
                }
            }
        }

        int copied = 0;
        for (InstalledMod mod : resolved) {
            if (!mod.willLaunch() || mod.jar() == null) {
                if (mod.status() == ModStatus.CORRUPT) {
                    progress.status("BLOCKED: " + mod.name() + " failed sha256 verification -- not loading it");
                }
                continue;
            }
            Files.copy(mod.jar(), gameModsDir.resolve(mod.entry().fileName()), StandardCopyOption.REPLACE_EXISTING);
            copied++;
        }
        progress.status("Mods ready: " + copied + " loaded");
        return resolved;
    }

    /**
     * Jars built out of this repo's own {@code mods/} subprojects. These cannot be pinned -- they
     * change on every build -- so they are self-hashed and flagged LOCAL_DEV rather than pretending
     * to carry the same guarantee as the curated bundle.
     */
    private List<InstalledMod> discoverDevMods() {
        List<InstalledMod> devMods = new ArrayList<>();
        Path modsSourceRoot = Path.of("..", "mods");
        if (!Files.isDirectory(modsSourceRoot)) {
            return devMods;
        }
        try (var projects = Files.list(modsSourceRoot)) {
            for (Path project : (Iterable<Path>) projects::iterator) {
                Path libs = project.resolve("build").resolve("libs");
                if (!Files.isDirectory(libs)) {
                    continue;
                }
                try (var jars = Files.list(libs)) {
                    for (Path jar : (Iterable<Path>) jars::iterator) {
                        if (jar.toString().endsWith(".jar")) {
                            devMods.add(toDevMod(jar));
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            // No dev mods is a perfectly normal state.
        }
        return devMods;
    }

    private InstalledMod toDevMod(Path jar) throws IOException {
        String base = jar.getFileName().toString().replaceFirst("\\.jar$", "");
        ModEntry entry = new ModEntry(
                DEV_PREFIX + base,
                base,
                "local",
                "Built from source in mods/ -- not hash-pinned.",
                DEV_CATEGORY,
                jar.toUri().toString(),
                Hashing.sha256(jar),
                Files.size(jar),
                "",
                "local",
                false,
                true);
        return new InstalledMod(entry, ModStatus.LOCAL_DEV, jar, true);
    }
}
