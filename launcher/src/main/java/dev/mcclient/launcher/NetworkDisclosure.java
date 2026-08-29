package dev.mcclient.launcher;

import java.util.List;

/**
 * Every host this client is capable of contacting, and why.
 *
 * <p>The privacy claim in the README is a sentence you have to take on faith. This is the same
 * claim in a form you can audit: the full list, kept next to the code that makes the calls, so a
 * new endpoint has to be added here to be added at all.
 *
 * <p>Compiled by hand from the URL literals in this source tree. If you want to check it rather
 * than trust it: {@code grep -rhoE '"https?://[^"]+"' launcher/src/main mods/*&#47;src/main}.
 */
public final class NetworkDisclosure {

    private NetworkDisclosure() {}

    /** One host the client can talk to. */
    public static final class Endpoint {

        private final String host;
        private final String purpose;
        private final String when;
        private final boolean sendsIdentity;

        Endpoint(String host, String purpose, String when, boolean sendsIdentity) {
            this.host = host;
            this.purpose = purpose;
            this.when = when;
            this.sendsIdentity = sendsIdentity;
        }

        public String host() {
            return host;
        }

        public String purpose() {
            return purpose;
        }

        /** What triggers the call, so "never, unless you sign in" is visible as such. */
        public String when() {
            return when;
        }

        /** True where the request necessarily carries who you are. */
        public boolean sendsIdentity() {
            return sendsIdentity;
        }
    }

    public static List<Endpoint> endpoints() {
        return List.of(
                new Endpoint("login.microsoftonline.com",
                        "Microsoft sign-in (device code, token refresh)",
                        "Only when you sign in, or refresh an expired session", true),
                new Endpoint("user.auth.xboxlive.com",
                        "Xbox Live authentication leg",
                        "Only during sign-in", true),
                new Endpoint("xsts.auth.xboxlive.com",
                        "Xbox XSTS authorisation leg",
                        "Only during sign-in", true),
                new Endpoint("api.minecraftservices.com",
                        "Exchanges the Xbox token for a Minecraft session, and reads your profile",
                        "Only during sign-in", true),
                new Endpoint("launchermeta.mojang.com",
                        "Mojang's version manifest -- which files 1.8.9 is made of",
                        "On launch", false),
                new Endpoint("libraries.minecraft.net / resources.download.minecraft.net",
                        "The game's own libraries and assets, at URLs the manifest names",
                        "First launch, then only what is missing", false),
                new Endpoint("meta.legacyfabric.net",
                        "Legacy Fabric loader profile",
                        "On launch", false),
                new Endpoint("maven.legacyfabric.net",
                        "Legacy Fabric loader libraries, at URLs the profile names",
                        "First launch, then only what is missing", false),
                new Endpoint("cdn.modrinth.com",
                        "Downloads the bundled mods, each pinned to a sha256 in the manifest",
                        "Only when a bundled mod is missing or fails verification", false));
    }

    /**
     * Things the client does not do. Stated as flatly as the list above, because the absence is
     * the point -- and because these are the specific behaviours the commercial clients ship.
     */
    public static List<String> neverDoes() {
        return List.of(
                "No analytics or telemetry SDK is bundled, so there is nothing to report usage to.",
                "No account, hardware, or usage data is sent anywhere. The endpoints above are the whole list.",
                "The mod manifest ships inside the jar and is never fetched from a server -- a remote "
                        + "config fetch would be a phone-home by another name.",
                "There is no cosmetics or capes service, which is the usual reason a client needs to "
                        + "know who you are while you play.",
                "There is no Discord Rich Presence integration, which is the other one.",
                "Your session token is stored locally in auth-cache.json and is sent only to Mojang "
                        + "and Microsoft, in the sign-in legs listed above.",
                "The client never contacts a server belonging to this project. There isn't one.");
    }
}
