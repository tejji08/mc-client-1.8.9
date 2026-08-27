package dev.mcclient.launcher.auth;

import dev.mcclient.launcher.auth.model.MicrosoftTokens;
import dev.mcclient.launcher.auth.model.MinecraftSession;

import java.net.http.HttpClient;
import java.util.UUID;

/**
 * Decides how to get a playable session: cached and still valid -> reuse;
 * cached but expired -> silent refresh; nothing cached -> full device-code sign-in.
 * Falls back to an offline/dev session if no Azure client ID is configured at all.
 */
public final class SessionResolver {

    private final HttpClient http;
    private final TokenCache cache = new TokenCache();

    public SessionResolver(HttpClient http) {
        this.http = http;
    }

    public MinecraftSession resolve() throws Exception {
        String clientId = Config.clientId();
        if (clientId == null) {
            System.out.println("No Azure client ID configured (see auth/Config.java) — running offline/dev mode.");
            return offlineSession();
        }

        DeviceCodeAuth deviceCodeAuth = new DeviceCodeAuth(http, clientId);
        MinecraftAuth minecraftAuth = new MinecraftAuth(http);

        MinecraftSession cached = cache.session();
        if (cached != null && !cached.isExpired()) {
            System.out.println("Reusing cached session for " + cached.username());
            return cached;
        }

        String refreshToken = cache.refreshToken();
        MicrosoftTokens msTokens;
        if (refreshToken != null) {
            System.out.println("Refreshing Microsoft sign-in silently...");
            msTokens = deviceCodeAuth.refresh(refreshToken);
        } else {
            System.out.println("Sign-in required.");
            msTokens = deviceCodeAuth.authenticate();
        }

        // Persist the refresh token before the Minecraft leg. That leg can fail for reasons
        // unrelated to the Microsoft sign-in itself (notably a 403 while the Azure app is still
        // waiting on Minecraft API allowlist approval) -- without this, a perfectly good refresh
        // token gets thrown away and the next attempt needs another interactive device-code round.
        cache.save(msTokens.refreshToken(), null);

        MinecraftSession session = minecraftAuth.authenticate(msTokens);
        cache.save(msTokens.refreshToken(), session);
        System.out.println("Signed in as " + session.username());
        return session;
    }

    private MinecraftSession offlineSession() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        long farFuture = System.currentTimeMillis() / 1000 + 3600;
        return new MinecraftSession("0", uuid, "dev", farFuture);
    }
}
