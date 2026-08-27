package dev.mcclient.launcher.auth;

import dev.mcclient.launcher.auth.model.MicrosoftTokens;
import dev.mcclient.launcher.auth.model.MinecraftSession;

import java.net.http.HttpClient;

/**
 * Standalone probe for whether this Azure app has cleared Minecraft API allowlist review
 * (https://aka.ms/mce-reviewappid). Microsoft exposes no status endpoint for that review, so the
 * only real check is attempting the login_with_xbox exchange and reading the result.
 *
 * <p>Runs headless off the cached refresh token when one exists, so it's safe to schedule. Exit
 * code 0 means approved, 2 means still pending, 1 means it failed for some other reason (including
 * needing an interactive sign-in to seed the cache).
 */
public final class ApprovalCheck {

    private static final int APPROVED = 0;
    private static final int FAILED = 1;
    private static final int PENDING = 2;

    public static void main(String[] args) {
        System.exit(run());
    }

    private static int run() {
        String clientId = Config.clientId();
        if (clientId == null) {
            System.out.println("UNKNOWN - no Azure client ID configured (see auth/Config.java).");
            return FAILED;
        }

        HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        TokenCache cache = new TokenCache();
        DeviceCodeAuth deviceCodeAuth = new DeviceCodeAuth(http, clientId);

        String refreshToken = cache.refreshToken();
        if (refreshToken == null) {
            System.out.println("UNKNOWN - no cached refresh token; run the launcher once and sign in to seed it.");
            return FAILED;
        }

        MicrosoftTokens msTokens;
        try {
            msTokens = deviceCodeAuth.refresh(refreshToken);
            cache.save(msTokens.refreshToken(), cache.session());
        } catch (Exception e) {
            System.out.println("UNKNOWN - Microsoft token refresh failed: " + e.getMessage());
            return FAILED;
        }

        try {
            MinecraftSession session = new MinecraftAuth(http).authenticate(msTokens);
            cache.save(msTokens.refreshToken(), session);
            System.out.println("APPROVED - signed in as " + session.username() + ". The app has cleared review.");
            return APPROVED;
        } catch (Exception e) {
            String message = String.valueOf(e.getMessage());
            if (message.contains("Invalid app registration")) {
                System.out.println("PENDING - still awaiting Minecraft API allowlist approval.");
                return PENDING;
            }
            System.out.println("UNKNOWN - unexpected failure: " + message);
            return FAILED;
        }
    }
}
