package dev.mcclient.launcher.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mcclient.launcher.auth.model.MicrosoftTokens;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Microsoft identity platform device-code flow. Talks directly to
 * login.microsoftonline.com — no third-party auth relay, ever.
 */
public final class DeviceCodeAuth {

    private static final String DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String SCOPE = "XboxLive.signin offline_access";

    private final HttpClient http;
    private final String clientId;

    public DeviceCodeAuth(HttpClient http, String clientId) {
        this.http = http;
        this.clientId = clientId;
    }

    /** Runs the full device-code flow, printing the instructions, and blocks until sign-in completes. */
    public MicrosoftTokens authenticate() throws IOException, InterruptedException {
        return authenticate(SignInPrompt.CONSOLE);
    }

    /**
     * Runs the full device-code flow, handing the sign-in instructions to {@code prompt}, and
     * blocks until the user completes it.
     */
    public MicrosoftTokens authenticate(SignInPrompt prompt) throws IOException, InterruptedException {
        JsonObject deviceCodeResponse = requestDeviceCode();

        String deviceCode = deviceCodeResponse.get("device_code").getAsString();
        String userCode = deviceCodeResponse.get("user_code").getAsString();
        String verificationUri = deviceCodeResponse.get("verification_uri").getAsString();
        int intervalSeconds = deviceCodeResponse.has("interval") ? deviceCodeResponse.get("interval").getAsInt() : 5;
        int expiresInSeconds = deviceCodeResponse.get("expires_in").getAsInt();

        prompt.show(verificationUri, userCode);

        try {
            return poll(deviceCode, intervalSeconds, expiresInSeconds);
        } finally {
            prompt.dismiss();
        }
    }

    private MicrosoftTokens poll(String deviceCode, int intervalSeconds, int expiresInSeconds)
            throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + expiresInSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(intervalSeconds * 1000L);

            JsonObject tokenResponse = pollToken(deviceCode);
            if (tokenResponse.has("error")) {
                String error = tokenResponse.get("error").getAsString();
                switch (error) {
                    case "authorization_pending" -> { /* keep polling */ }
                    case "slow_down" -> intervalSeconds += 5;
                    case "authorization_declined" -> throw new IOException("Sign-in was declined");
                    case "expired_token" -> throw new IOException("Device code expired, restart sign-in");
                    default -> throw new IOException("Device code auth failed: " + error);
                }
                continue;
            }

            String accessToken = tokenResponse.get("access_token").getAsString();
            String refreshToken = tokenResponse.get("refresh_token").getAsString();
            long expiresAt = System.currentTimeMillis() / 1000 + tokenResponse.get("expires_in").getAsLong();
            return new MicrosoftTokens(accessToken, refreshToken, expiresAt);
        }
        throw new IOException("Timed out waiting for sign-in");
    }

    /** Exchanges a refresh token for a new access token, silently — no browser prompt needed. */
    public MicrosoftTokens refresh(String refreshToken) throws IOException, InterruptedException {
        Map<String, String> form = Map.of(
                "client_id", clientId,
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "scope", SCOPE
        );
        JsonObject response = postForm(TOKEN_URL, form);
        if (response.has("error")) {
            throw new IOException("Token refresh failed: " + response.get("error").getAsString());
        }
        String accessToken = response.get("access_token").getAsString();
        String newRefreshToken = response.has("refresh_token") ? response.get("refresh_token").getAsString() : refreshToken;
        long expiresAt = System.currentTimeMillis() / 1000 + response.get("expires_in").getAsLong();
        return new MicrosoftTokens(accessToken, newRefreshToken, expiresAt);
    }

    private JsonObject requestDeviceCode() throws IOException, InterruptedException {
        Map<String, String> form = Map.of("client_id", clientId, "scope", SCOPE);
        return postForm(DEVICE_CODE_URL, form);
    }

    private JsonObject pollToken(String deviceCode) throws IOException, InterruptedException {
        Map<String, String> form = Map.of(
                "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                "client_id", clientId,
                "device_code", deviceCode
        );
        return postForm(TOKEN_URL, form);
    }

    private JsonObject postForm(String url, Map<String, String> form) throws IOException, InterruptedException {
        String body = form.entrySet().stream()
                .map(e -> java.net.URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + java.net.URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
