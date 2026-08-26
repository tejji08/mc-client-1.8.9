package dev.mcclient.launcher.auth.model;

public record MicrosoftTokens(String accessToken, String refreshToken, long expiresAtEpochSeconds) {
    public boolean isExpired() {
        return System.currentTimeMillis() / 1000 >= expiresAtEpochSeconds;
    }
}
