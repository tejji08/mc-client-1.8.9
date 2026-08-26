package dev.mcclient.launcher.mojang;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * Talks directly to Mojang's own manifest endpoints — no third-party mirror,
 * nothing that could be a telemetry relay.
 */
public final class VersionManifest {

    private static final String MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    private final HttpClient http;

    public VersionManifest(HttpClient http) {
        this.http = http;
    }

    /** Returns the versionId's own manifest URL (a per-version JSON with the actual download links), if it exists. */
    public Optional<String> findVersionManifestUrl(String versionId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(MANIFEST_URL)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch version manifest: HTTP " + response.statusCode());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        for (var element : root.getAsJsonArray("versions")) {
            JsonObject version = element.getAsJsonObject();
            if (version.get("id").getAsString().equals(versionId)) {
                return Optional.of(version.get("url").getAsString());
            }
        }
        return Optional.empty();
    }

    /** Fetches and parses the per-version manifest (client download URL, libraries, asset index, etc). */
    public JsonObject fetchVersionDetails(String versionManifestUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(versionManifestUrl)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch version details: HTTP " + response.statusCode());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
