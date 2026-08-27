package dev.mcclient.launcher;

import com.google.gson.JsonObject;
import dev.mcclient.launcher.auth.model.MinecraftSession;
import dev.mcclient.launcher.fabric.FabricProfile;
import dev.mcclient.launcher.mods.InstalledMod;
import dev.mcclient.launcher.mods.ModManager;
import dev.mcclient.launcher.mojang.GameDownloader;
import dev.mcclient.launcher.mojang.VersionManifest;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The whole launch pipeline in one place: vanilla download, Legacy Fabric resolution, mod
 * verification, and process start. Reports through {@link Progress} so the console launcher and
 * the GUI can both drive it without either owning it.
 */
public final class GameLauncher {

    public static final String TARGET_VERSION = "1.8.9";

    private final HttpClient http;
    private final ModManager mods;

    public GameLauncher(HttpClient http, ModManager mods) {
        this.http = http;
        this.mods = mods;
    }

    /**
     * Prepares everything and starts the game.
     *
     * @return the running game process, so a caller can wait on it or watch it exit
     */
    public Process launch(MinecraftSession session, LauncherSettings settings, Progress progress)
            throws Exception {
        VersionManifest manifest = new VersionManifest(http);
        GameDownloader downloader = new GameDownloader(http);

        progress.status("Resolving " + TARGET_VERSION + " from Mojang's manifest...");
        String versionUrl = manifest.findVersionManifestUrl(TARGET_VERSION)
                .orElseThrow(() -> new IllegalStateException("Version " + TARGET_VERSION + " not found in manifest"));
        JsonObject versionDetails = manifest.fetchVersionDetails(versionUrl);

        Path versionDir = LauncherPaths.versions().resolve(TARGET_VERSION);
        LauncherPaths.ensureDirectory(versionDir);
        Path nativesDir = versionDir.resolve("natives");

        progress.status("Downloading client jar...");
        Path clientJar = downloader.downloadClientJar(versionDetails, versionDir);

        progress.status("Downloading libraries...");
        List<Path> libraryClasspath = downloader.downloadLibraries(versionDetails, LauncherPaths.libraries());

        progress.status("Downloading + extracting natives...");
        downloader.downloadAndExtractNatives(versionDetails, LauncherPaths.libraries(), nativesDir);

        progress.status("Downloading assets (slow on first run only)...");
        downloader.downloadAssets(versionDetails, LauncherPaths.assets());

        progress.status("Resolving Legacy Fabric loader...");
        FabricProfile.Resolved fabric = new FabricProfile(http).resolve(TARGET_VERSION, LauncherPaths.libraries(), nativesDir);

        progress.status("Checking mods...");
        mods.installMissing(progress);

        Path gameDir = LauncherPaths.root().resolve("game");
        LauncherPaths.ensureDirectory(gameDir);
        List<InstalledMod> loaded = mods.syncToGameDir(gameDir.resolve("mods"), progress);
        for (InstalledMod mod : loaded) {
            if (mod.willLaunch()) {
                progress.status("  + " + mod.name() + " " + mod.entry().displayVersion() + " [" + mod.status().label() + "]");
            }
        }

        progress.status("Launching...");
        return start(versionDetails, clientJar, libraryClasspath, nativesDir, session, fabric, settings, gameDir);
    }

    private Process start(JsonObject versionDetails, Path clientJar, List<Path> libraryClasspath, Path nativesDir,
                          MinecraftSession session, FabricProfile.Resolved fabric, LauncherSettings settings,
                          Path gameDir) throws IOException {
        String assetIndexId = versionDetails.getAsJsonObject("assetIndex").get("id").getAsString();

        // Fabric supplies its own patched LWJGL etc. -- drop the vanilla copies of anything it
        // replaces so the JVM doesn't load two different builds of the same class.
        List<Path> filteredVanilla = libraryClasspath.stream()
                .filter(p -> fabric.replacedArtifacts().stream().noneMatch(ga -> {
                    String[] parts = ga.split(":");
                    String needle = "/" + parts[0].replace('.', '/') + "/" + parts[1] + "/";
                    return p.toString().replace('\\', '/').contains(needle);
                }))
                .toList();

        List<Path> fullClasspath = new ArrayList<>(fabric.classpath());
        fullClasspath.addAll(filteredVanilla);
        fullClasspath.add(clientJar);
        String classpath = fullClasspath.stream()
                .map(Path::toString)
                .reduce((a, b) -> a + System.getProperty("path.separator", ";") + b)
                .orElseThrow();

        List<String> command = new ArrayList<>();
        command.add(resolveJavaBinary());
        command.add("-Xmx" + settings.memoryMb() + "M");
        command.add("-Djava.library.path=" + nativesDir);
        command.add("-cp");
        command.add(classpath);
        command.add(fabric.mainClass());
        command.add("--username"); command.add(session.username());
        command.add("--version"); command.add(TARGET_VERSION);
        command.add("--gameDir"); command.add(gameDir.toString());
        command.add("--assetsDir"); command.add(LauncherPaths.assets().toString());
        command.add("--assetIndex"); command.add(assetIndexId);
        command.add("--uuid"); command.add(session.uuid());
        command.add("--accessToken"); command.add(session.accessToken());
        command.add("--userType"); command.add(session.accessToken().equals("0") ? "legacy" : "msa");
        command.add("--width"); command.add(String.valueOf(settings.width()));
        command.add("--height"); command.add(String.valueOf(settings.height()));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(gameDir.toFile());
        pb.inheritIO();
        return pb.start();
    }

    private static String resolveJavaBinary() {
        String javaHome = System.getProperty("java.home");
        String exe = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(javaHome, "bin", exe).toString();
    }
}
