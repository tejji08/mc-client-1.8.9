package dev.mcclient.launcher.mods;

import java.nio.file.Path;

/**
 * A manifest entry resolved against what's actually on disk right now.
 *
 * @param jar   where the jar lives, or null when nothing is installed
 * @param enabled the user's on/off choice, independent of whether it verified
 */
public record InstalledMod(ModEntry entry, ModStatus status, Path jar, boolean enabled) {

    /** Only mods that both verified and are switched on may reach the game. */
    public boolean willLaunch() {
        return enabled && status.isLaunchable();
    }

    public String id() {
        return entry.id();
    }

    public String name() {
        return entry.name();
    }
}
