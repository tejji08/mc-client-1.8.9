package dev.mcclient.launcher;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Where the launcher stores game files. Everything lives under one root, nothing touches the vanilla launcher's own folder. */
public final class LauncherPaths {

    private LauncherPaths() {}

    public static Path root() {
        String appData = System.getenv("APPDATA");
        Path base = (appData != null)
                ? Paths.get(appData)
                : Paths.get(System.getProperty("user.home"), "AppData", "Roaming");
        return base.resolve("mc-client-1.8.9");
    }

    public static Path versions() {
        return root().resolve("versions");
    }

    public static Path libraries() {
        return root().resolve("libraries");
    }

    public static Path assets() {
        return root().resolve("assets");
    }

    public static Path mods() {
        return root().resolve("mods");
    }

    public static Path cache() {
        return root().resolve("cache");
    }
}
