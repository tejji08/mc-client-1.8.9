package dev.mcclient.launcher.fabric;

import dev.mcclient.launcher.LauncherPaths;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Pulls a Legacy Fabric loader profile (library list + Knot main class) for a given
 * Minecraft version off meta.legacyfabric.net, downloads its libraries, and reports
 * a classpath + main class ready to merge with the vanilla launch.
 *
 * meta.legacyfabric.net mirrors FabricMC's own fabric-meta API shape:
 *   /v2/versions/loader/{game_version}            -> compatible loader builds
 *   /v2/versions/loader/{game_version}/{loader}/profile/json -> launcher profile
 */
public final class FabricProfile {

    private static final String META_BASE = "https://meta.legacyfabric.net/v2/versions/loader/";

    private final HttpClient http;

    public FabricProfile(HttpClient http) {
        this.http = http;
    }

    public record Resolved(String mainClass, List<Path> classpath, List<String> replacedArtifacts) {}

    /** Picks the newest stable loader build for gameVersion, fetches its profile, downloads its libraries. */
    public Resolved resolve(String gameVersion, Path librariesDir, Path nativesDir) throws IOException, InterruptedException {
        String loaderVersion = fetchLatestStableLoaderVersion(gameVersion);
        JsonObject profile = fetchProfileJson(gameVersion, loaderVersion);

        String mainClass = profile.get("mainClass").getAsString();
        List<Path> classpath = new ArrayList<>();
        List<String> replacedArtifacts = new ArrayList<>();

        for (JsonElement el : profile.getAsJsonArray("libraries")) {
            JsonObject lib = el.getAsJsonObject();
            String name = lib.get("name").getAsString();
            String baseUrl = lib.get("url").getAsString();

            String[] parts = name.split(":");
            String group = parts[0];
            String artifact = parts[1];
            String version = parts[2];

            // group:artifact identity, without version -- used to strip the vanilla equivalent (e.g. old lwjgl) off the classpath
            replacedArtifacts.add(group + ":" + artifact);

            boolean isNativesOnly = lib.has("natives");
            String classifier = null;
            if (isNativesOnly) {
                JsonObject natives = lib.getAsJsonObject("natives");
                String currentOs = currentOsName();
                if (!natives.has(currentOs)) {
                    continue; // no native artifact for this OS, e.g. skip -- nothing to put on the classpath
                }
                classifier = natives.get(currentOs).getAsString();
            }
            String fileName = classifier == null
                    ? artifact + "-" + version + ".jar"
                    : artifact + "-" + version + "-" + classifier + ".jar";
            String mavenPath = group.replace('.', '/') + "/" + artifact + "/" + version + "/" + fileName;

            Path dest = librariesDir.resolve(mavenPath);
            String sha1 = lib.has("sha1") ? lib.get("sha1").getAsString() : null;
            downloadMaybeVerified(baseUrl + mavenPath, sha1, dest);

            if (isNativesOnly) {
                extractNatives(dest, nativesDir);
            } else {
                classpath.add(dest);
            }
        }

        return new Resolved(mainClass, classpath, replacedArtifacts);
    }

    private String fetchLatestStableLoaderVersion(String gameVersion) throws IOException, InterruptedException {
        String url = META_BASE + urlEncode(gameVersion);
        JsonArray loaders = fetchJsonArray(url);
        for (JsonElement el : loaders) {
            JsonObject entry = el.getAsJsonObject().getAsJsonObject("loader");
            if (entry.get("stable").getAsBoolean()) {
                return entry.get("version").getAsString();
            }
        }
        if (loaders.size() == 0) {
            throw new IOException("No Legacy Fabric loader builds available for " + gameVersion);
        }
        return loaders.get(0).getAsJsonObject().getAsJsonObject("loader").get("version").getAsString();
    }

    private JsonObject fetchProfileJson(String gameVersion, String loaderVersion) throws IOException, InterruptedException {
        String url = META_BASE + urlEncode(gameVersion) + "/" + urlEncode(loaderVersion) + "/profile/json";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Fabric profile fetch failed (" + response.statusCode() + "): " + url);
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private JsonArray fetchJsonArray(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Fabric meta fetch failed (" + response.statusCode() + "): " + url);
        }
        return JsonParser.parseString(response.body()).getAsJsonArray();
    }

    private void extractNatives(Path jarPath, Path nativesDir) throws IOException {
        LauncherPaths.ensureDirectory(nativesDir);
        try (var zip = new java.util.zip.ZipFile(jarPath.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().startsWith("META-INF/")) {
                    continue;
                }
                Path dest = nativesDir.resolve(Path.of(entry.getName()).getFileName().toString());
                if (Files.exists(dest)) {
                    continue;
                }
                try (var in = zip.getInputStream(entry)) {
                    Files.copy(in, dest);
                }
            }
        }
    }

    private String currentOsName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        return "linux";
    }

    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private void downloadMaybeVerified(String url, String expectedSha1, Path dest) throws IOException, InterruptedException {
        if (Files.exists(dest) && (expectedSha1 == null || sha1Of(dest).equalsIgnoreCase(expectedSha1))) {
            return;
        }
        LauncherPaths.ensureDirectory(dest.getParent());

        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Download failed (" + response.statusCode() + "): " + url);
        }

        Path tmp = dest.resolveSibling(dest.getFileName() + ".tmp");
        Files.write(tmp, response.body());

        if (expectedSha1 != null) {
            String actualSha1 = sha1Of(tmp);
            if (!actualSha1.equalsIgnoreCase(expectedSha1)) {
                Files.deleteIfExists(tmp);
                throw new IOException("sha1 mismatch for " + url + " (expected " + expectedSha1 + ", got " + actualSha1 + ")");
            }
        }
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    private String sha1Of(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(Files.readAllBytes(file));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }
}
