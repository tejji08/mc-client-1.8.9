package dev.mcclient.launcher;

import java.io.IOException;
import java.nio.file.Files;
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

    /**
     * Like {@link Files#createDirectories}, but tolerant of a path that is a symlink or junction to
     * an existing directory. {@code createDirectories} throws {@link java.nio.file.FileAlreadyExistsException}
     * in that case (JDK-8130464), which would otherwise break anyone who links their assets or
     * versions folder to share it between launchers -- a common enough setup to be worth handling.
     */
    public static void ensureDirectory(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            Files.createDirectories(dir);
        }
    }
}
