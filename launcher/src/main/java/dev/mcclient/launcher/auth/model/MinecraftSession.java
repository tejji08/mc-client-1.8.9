package dev.mcclient.launcher.auth.model;

/** Everything the vanilla client's launch args need to run online. */
public record MinecraftSession(String accessToken, String uuid, String username, long expiresAtEpochSeconds) {
    public boolean isExpired() {
        return System.currentTimeMillis() / 1000 >= expiresAtEpochSeconds;
    }
}
