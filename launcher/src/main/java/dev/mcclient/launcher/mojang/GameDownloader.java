package dev.mcclient.launcher.mojang;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Downloads the vanilla client jar, its libraries, and the asset index/objects
 * straight from Mojang's CDN, verifying sha1 on everything. No mirrors.
 */
public final class GameDownloader {

    private final HttpClient http;

    public GameDownloader(HttpClient http) {
        this.http = http;
    }

    /** Downloads the client jar described by version.json's downloads.client, into targetDir. Returns the jar path. */
    public Path downloadClientJar(JsonObject versionDetails, Path targetDir) throws IOException, InterruptedException {
        JsonObject client = versionDetails.getAsJsonObject("downloads").getAsJsonObject("client");
        String url = client.get("url").getAsString();
        String sha1 = client.get("sha1").getAsString();
        String versionId = versionDetails.get("id").getAsString();

        Path dest = targetDir.resolve(versionId + ".jar");
        downloadVerified(url, sha1, dest);
        return dest;
    }

    /** Downloads every library applicable to this OS, laid out in the standard m2-style library path. Returns the classpath entries. */
    public java.util.List<Path> downloadLibraries(JsonObject versionDetails, Path librariesDir) throws IOException, InterruptedException {
        java.util.List<Path> classpath = new java.util.ArrayList<>();
        for (JsonElement el : versionDetails.getAsJsonArray("libraries")) {
            JsonObject lib = el.getAsJsonObject();
            if (!ruleAllowsCurrentOs(lib)) {
                continue;
            }
            if (!lib.has("downloads") || !lib.getAsJsonObject("downloads").has("artifact")) {
                continue;
            }
            JsonObject artifact = lib.getAsJsonObject("downloads").getAsJsonObject("artifact");
            String path = artifact.get("path").getAsString();
            String url = artifact.get("url").getAsString();
            String sha1 = artifact.get("sha1").getAsString();

            Path dest = librariesDir.resolve(path);
            downloadVerified(url, sha1, dest);
            classpath.add(dest);
        }
        return classpath;
    }

    /** Downloads the natives jar for this OS (if any) and unpacks it (skipping META-INF) into nativesDir. */
    public void downloadAndExtractNatives(JsonObject versionDetails, Path librariesDir, Path nativesDir) throws IOException, InterruptedException {
        Files.createDirectories(nativesDir);
        String currentOs = currentOsName();

        for (JsonElement el : versionDetails.getAsJsonArray("libraries")) {
            JsonObject lib = el.getAsJsonObject();
            if (!lib.has("natives") || !ruleAllowsCurrentOs(lib)) {
                continue;
            }
            JsonObject natives = lib.getAsJsonObject("natives");
            if (!natives.has(currentOs)) {
                continue;
            }
            String classifierKey = natives.get(currentOs).getAsString();
            JsonObject classifiers = lib.getAsJsonObject("downloads").getAsJsonObject("classifiers");
            if (!classifiers.has(classifierKey)) {
                continue;
            }
            JsonObject artifact = classifiers.getAsJsonObject(classifierKey);
            String path = artifact.get("path").getAsString();
            String url = artifact.get("url").getAsString();
            String sha1 = artifact.get("sha1").getAsString();

            Path jarPath = librariesDir.resolve(path);
            downloadVerified(url, sha1, jarPath);
            extractNativesJar(jarPath, nativesDir);
        }
    }

    private void extractNativesJar(Path jarPath, Path nativesDir) throws IOException {
        try (var zip = new java.util.zip.ZipFile(jarPath.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().startsWith("META-INF/")) {
                    continue;
                }
                Path dest = nativesDir.resolve(entry.getName());
                if (Files.exists(dest)) {
                    continue; // static per version; also avoids clobbering a DLL a running instance still has open
                }
                Files.createDirectories(dest.getParent());
                try (var in = zip.getInputStream(entry)) {
                    Files.copy(in, dest);
                }
            }
        }
    }

    /** Downloads the asset index and every referenced object into the standard objects/xx/hash layout. */
    public void downloadAssets(JsonObject versionDetails, Path assetsDir) throws IOException, InterruptedException {
        JsonObject assetIndexRef = versionDetails.getAsJsonObject("assetIndex");
        String indexUrl = assetIndexRef.get("url").getAsString();
        String indexId = assetIndexRef.get("id").getAsString();

        Path indexDir = assetsDir.resolve("indexes");
        Files.createDirectories(indexDir);
        Path indexPath = indexDir.resolve(indexId + ".json");
        downloadVerified(indexUrl, assetIndexRef.get("sha1").getAsString(), indexPath);

        JsonObject index = JsonParser.parseString(Files.readString(indexPath)).getAsJsonObject();
        JsonObject objects = index.getAsJsonObject("objects");
        Path objectsDir = assetsDir.resolve("objects");

        for (String key : objects.keySet()) {
            JsonObject obj = objects.getAsJsonObject(key);
            String hash = obj.get("hash").getAsString();
            String prefix = hash.substring(0, 2);
            Path dest = objectsDir.resolve(prefix).resolve(hash);
            if (Files.exists(dest)) {
                continue; // asset objects are content-addressed, safe to skip if present
            }
            String url = "https://resources.download.minecraft.net/" + prefix + "/" + hash;
            downloadVerified(url, hash, dest);
        }
    }

    private boolean ruleAllowsCurrentOs(JsonObject lib) {
        if (!lib.has("rules")) {
            return true;
        }
        String currentOs = currentOsName();
        boolean allowed = false;
        for (JsonElement el : lib.getAsJsonArray("rules")) {
            JsonObject rule = el.getAsJsonObject();
            String action = rule.get("action").getAsString();
            boolean matches = true;
            if (rule.has("os")) {
                String ruleOs = rule.getAsJsonObject("os").get("name").getAsString();
                matches = ruleOs.equals(currentOs);
            }
            if (matches) {
                allowed = action.equals("allow");
            }
        }
        return allowed;
    }

    private String currentOsName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        return "linux";
    }

    private void downloadVerified(String url, String expectedSha1, Path dest) throws IOException, InterruptedException {
        if (Files.exists(dest) && sha1Of(dest).equalsIgnoreCase(expectedSha1)) {
            return;
        }
        Files.createDirectories(dest.getParent());

        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Download failed (" + response.statusCode() + "): " + url);
        }

        Path tmp = dest.resolveSibling(dest.getFileName() + ".tmp");
        Files.write(tmp, response.body());

        String actualSha1 = sha1Of(tmp);
        if (!actualSha1.equalsIgnoreCase(expectedSha1)) {
            Files.deleteIfExists(tmp);
            throw new IOException("sha1 mismatch for " + url + " (expected " + expectedSha1 + ", got " + actualSha1 + ")");
        }
        Files.move(tmp, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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
