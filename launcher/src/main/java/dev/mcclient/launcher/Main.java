package dev.mcclient.launcher;

import com.google.gson.JsonObject;
import dev.mcclient.launcher.auth.SessionResolver;
import dev.mcclient.launcher.auth.model.MinecraftSession;
import dev.mcclient.launcher.mojang.GameDownloader;
import dev.mcclient.launcher.mojang.VersionManifest;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Milestone 2: real Microsoft sign-in. Downloads vanilla 1.8.9 straight from Mojang,
 * verifies everything by sha1, signs in via the device-code flow (or reuses a cached
 * session), and launches the real game. Falls back to offline/dev mode if no Azure
 * client ID is configured. Legacy Fabric comes next.
 */
public final class Main {

    private static final String TARGET_VERSION = "1.8.9";

    public static void main(String[] args) throws Exception {
        HttpClient http = HttpClient.newHttpClient();
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

        MinecraftSession session = new SessionResolver(http).resolve();

        System.out.println("Launching...");
        launch(versionDetails, clientJar, libraryClasspath, nativesDir, session);
    }

    private static void launch(JsonObject versionDetails, Path clientJar, List<Path> libraryClasspath, Path nativesDir, MinecraftSession session) throws Exception {
        String assetIndexId = versionDetails.getAsJsonObject("assetIndex").get("id").getAsString();
        Path gameDir = LauncherPaths.root().resolve("game");
        Files.createDirectories(gameDir);

        List<Path> fullClasspath = new ArrayList<>(libraryClasspath);
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
        command.add("net.minecraft.client.main.Main");
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

    private static String resolveJavaBinary() {
        String javaHome = System.getProperty("java.home");
        String exe = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(javaHome, "bin", exe).toString();
    }
}
