package dev.mcclient.launcher.auth;

/**
 * Azure AD app (public client) registration used for the device-code flow.
 * You need your own — this project can't ship one:
 *
 *   1. https://portal.azure.com -> Azure Active Directory -> App registrations -> New registration
 *   2. Name it whatever, "Supported account types" = "Personal Microsoft accounts only"
 *   3. No redirect URI needed
 *   4. After creation: Authentication -> Advanced settings -> "Allow public client flows" = Yes
 *   5. Copy the Application (client) ID from the Overview page
 *
 * Then either set env var MC_CLIENT_AZURE_APP_ID, or drop the id (nothing else) into
 * %APPDATA%\mc-client-1.8.9\azure-client-id.txt
 */
public final class Config {

    private Config() {}

    public static String clientId() {
        String fromEnv = System.getenv("MC_CLIENT_AZURE_APP_ID");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        var file = dev.mcclient.launcher.LauncherPaths.root().resolve("azure-client-id.txt");
        if (java.nio.file.Files.exists(file)) {
            try {
                String content = java.nio.file.Files.readString(file).trim();
                if (!content.isBlank()) {
                    return content;
                }
            } catch (java.io.IOException ignored) {
                // fall through to null
            }
        }
        return null;
    }
}
