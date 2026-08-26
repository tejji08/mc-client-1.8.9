package dev.mcclient.launcher.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mcclient.launcher.auth.model.MicrosoftTokens;
import dev.mcclient.launcher.auth.model.MinecraftSession;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Full chain: Microsoft token -> Xbox Live -> XSTS -> Minecraft token -> profile.
 * Orchestrates DeviceCodeAuth + XboxAuth; TokenCache handles not repeating this every launch.
 */
public final class MinecraftAuth {

    private final HttpClient http;
    private final XboxAuth xboxAuth;

    public MinecraftAuth(HttpClient http) {
        this.http = http;
        this.xboxAuth = new XboxAuth(http);
    }

    /** Runs Xbox Live -> XSTS -> Minecraft login -> profile fetch from an already-valid Microsoft access token. */
    public MinecraftSession authenticate(MicrosoftTokens msTokens) throws IOException, InterruptedException {
        XboxAuth.Result xbl = xboxAuth.authenticateWithXboxLive(msTokens.accessToken());
        XboxAuth.Result xsts = xboxAuth.authenticateWithXsts(xbl.token());

        JsonObject loginResponse = loginWithXbox(xsts.userHashSalt(), xsts.token());
        String mcAccessToken = loginResponse.get("access_token").getAsString();
        long expiresAt = System.currentTimeMillis() / 1000 + loginResponse.get("expires_in").getAsLong();

        JsonObject profile = fetchProfile(mcAccessToken);
        if (profile.has("error")) {
            throw new IOException("No Minecraft profile on this account (does it own the game?): "
                    + profile.get("errorMessage").getAsString());
        }
        String uuid = profile.get("id").getAsString();
        String username = profile.get("name").getAsString();

        return new MinecraftSession(mcAccessToken, uuid, username, expiresAt);
    }

    private JsonObject loginWithXbox(String uhs, String xstsToken) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Minecraft login failed: HTTP " + response.statusCode() + " " + response.body());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private JsonObject fetchProfile(String mcAccessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + mcAccessToken)
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
