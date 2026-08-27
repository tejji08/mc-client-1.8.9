package dev.mcclient.launcher;

import com.google.gson.JsonObject;
import dev.mcclient.launcher.auth.SessionResolver;
import dev.mcclient.launcher.auth.model.MinecraftSession;
import dev.mcclient.launcher.fabric.FabricProfile;
import dev.mcclient.launcher.mojang.GameDownloader;
import dev.mcclient.launcher.mojang.VersionManifest;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Milestone 3: Legacy Fabric mod loading. Downloads vanilla 1.8.9 straight from Mojang,
 * verifies everything by sha1, resolves the Legacy Fabric loader profile, signs in via
 * the device-code flow (or reuses a cached session), and boots through Fabric's Knot
 * client entrypoint so mods/ gets scanned. Falls back to offline/dev mode if no Azure
 * client ID is configured.
 */
public final class Main {

    private static final String TARGET_VERSION = "1.8.9";

    public static void main(String[] args) throws Exception {
        HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        VersionManifest manifest = new VersionManifest(http);
        GameDownloader downloader = new GameDownloader(http);

        System.out.println("Resolving " + TARGET_VERSION + " from Mojang's manifest...");
        String versionUrl = manifest.findVersionManifestUrl(TARGET_VERSION)
                .orElseThrow(() -> new IllegalStateException("Version " + TARGET_VERSION + " not found in manifest"));
        JsonObject versionDetails = manifest.fetchVersionDetails(versionUrl);

        Path versionDir = LauncherPaths.versions().resolve(TARGET_VERSION);
        Files.createDirectories(versionDir);
        Path nativesDir = versionDir.resolve("natives");

        System.out.println("Downloading client jar...");
        Path clientJar = downloader.downloadClientJar(versionDetails, versionDir);

        System.out.println("Downloading libraries...");
        List<Path> libraryClasspath = downloader.downloadLibraries(versionDetails, LauncherPaths.libraries());

        System.out.println("Downloading + extracting natives...");
        downloader.downloadAndExtractNatives(versionDetails, LauncherPaths.libraries(), nativesDir);

        System.out.println("Downloading assets (this is the slow one, first run only)...");
        downloader.downloadAssets(versionDetails, LauncherPaths.assets());

        System.out.println("Resolving Legacy Fabric loader...");
        FabricProfile.Resolved fabric = new FabricProfile(http).resolve(TARGET_VERSION, LauncherPaths.libraries(), nativesDir);
        System.out.println("Fabric main class: " + fabric.mainClass());

        MinecraftSession session = new SessionResolver(http).resolve();

        System.out.println("Launching...");
        launch(versionDetails, clientJar, libraryClasspath, nativesDir, session, fabric);
    }

    private static void launch(JsonObject versionDetails, Path clientJar, List<Path> libraryClasspath, Path nativesDir, MinecraftSession session, FabricProfile.Resolved fabric) throws Exception {
        String assetIndexId = versionDetails.getAsJsonObject("assetIndex").get("id").getAsString();
        Path gameDir = LauncherPaths.root().resolve("game");
        Path modsDir = gameDir.resolve("mods");
        Files.createDirectories(gameDir);
        Files.createDirectories(modsDir);
        syncDevMods(modsDir);

        // Fabric supplies its own patched LWJGL etc. -- drop the vanilla copies of anything it replaces
        // so the JVM doesn't load two different builds of the same class off the classpath.
        List<Path> filteredVanilla = libraryClasspath.stream()
                .filter(p -> fabric.replacedArtifacts().stream().noneMatch(ga -> p.toString().replace('\\', '/').contains("/" + ga.split(":")[0].replace('.', '/') + "/" + ga.split(":")[1] + "/")))
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
        command.add("-Djava.library.path=" + nativesDir);
        command.add("-cp");
        command.add(classpath);
        command.add(fabric.mainClass());
        command.add("--username"); command.add(session.username());
        command.add("--version"); command.add("1.8.9");
        command.add("--gameDir"); command.add(gameDir.toString());
        command.add("--assetsDir"); command.add(LauncherPaths.assets().toString());
        command.add("--assetIndex"); command.add(assetIndexId);
        command.add("--uuid"); command.add(session.uuid());
        command.add("--accessToken"); command.add(session.accessToken());
        command.add("--userType"); command.add(session.accessToken().equals("0") ? "legacy" : "msa");
        command.add("--width"); command.add("925");
        command.add("--height"); command.add("530");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(gameDir.toFile());
        pb.inheritIO();
        Process process = pb.start();
        process.waitFor();
    }

    /** Dev convenience: copies any built mods/*&#47;build/libs/*.jar into the game's mods folder, so `./gradlew :launcher:run` always launches with whatever's freshly built. */
    private static void syncDevMods(Path modsDir) throws IOException {
        Path modsSourceRoot = Path.of("..", "mods");
        if (!Files.isDirectory(modsSourceRoot)) {
            return;
        }
        try (var modProjects = Files.list(modsSourceRoot)) {
            for (Path modProject : (Iterable<Path>) modProjects::iterator) {
                Path libs = modProject.resolve("build").resolve("libs");
                if (!Files.isDirectory(libs)) {
                    continue;
                }
                try (var jars = Files.list(libs)) {
                    for (Path jar : (Iterable<Path>) jars::iterator) {
                        if (jar.toString().endsWith(".jar")) {
                            Files.copy(jar, modsDir.resolve(jar.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Synced dev mod: " + jar.getFileName());
                        }
                    }
                }
            }
        }
    }

    private static String resolveJavaBinary() {
        String javaHome = System.getProperty("java.home");
        String exe = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(javaHome, "bin", exe).toString();
    }
}
