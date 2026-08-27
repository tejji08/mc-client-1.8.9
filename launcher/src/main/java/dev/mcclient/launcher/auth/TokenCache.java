package dev.mcclient.launcher.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.mcclient.launcher.LauncherPaths;
import dev.mcclient.launcher.auth.model.MinecraftSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local-only cache of the Microsoft refresh token + last Minecraft session, so a
 * re-launch doesn't need a fresh device-code sign-in every time. Never leaves this machine.
 */
public final class TokenCache {

    private record CacheData(String msRefreshToken, MinecraftSession session) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;

    public TokenCache() {
        this.file = LauncherPaths.root().resolve("auth-cache.json");
    }

    public java.util.Optional<CacheData> load() {
        if (!Files.exists(file)) {
            return java.util.Optional.empty();
        }
        try {
            String json = Files.readString(file);
            return java.util.Optional.of(GSON.fromJson(json, CacheData.class));
        } catch (IOException e) {
            return java.util.Optional.empty();
        }
    }

    public void save(String msRefreshToken, MinecraftSession session) throws IOException {
        LauncherPaths.ensureDirectory(file.getParent());
        Files.writeString(file, GSON.toJson(new CacheData(msRefreshToken, session)));
    }

    public String refreshToken() {
        return load().map(CacheData::msRefreshToken).orElse(null);
    }

    public MinecraftSession session() {
        return load().map(CacheData::session).orElse(null);
    }
}
