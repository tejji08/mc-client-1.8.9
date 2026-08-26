package dev.mcclient.launcher.auth;

/**
 * TODO (next milestone): Microsoft OAuth device-code flow, direct to Microsoft/Xbox/Mojang APIs.
 * Flow will be: device code -> poll for MS token -> Xbox Live token -> XSTS token -> Minecraft
 * services token -> profile (uuid/username). No third-party auth relay, ever.
 *
 * For now the launcher runs in offline/dev mode (no auth) to prove the download+launch pipeline.
 */
public final class MicrosoftAuth {
    private MicrosoftAuth() {}
}
