package dev.mcclient.launcher.mods;

/** Where a mod stands relative to the manifest's pinned hash. */
public enum ModStatus {

    /** Listed in the manifest, no jar on disk yet. */
    NOT_INSTALLED("Not installed"),

    /** On disk and the sha256 matches the manifest exactly. */
    VERIFIED("Verified"),

    /** On disk but the sha256 does NOT match. Treated as hostile -- never launched. */
    CORRUPT("Hash mismatch"),

    /** A jar built locally from mods/ that by definition can't be hash-pinned. */
    LOCAL_DEV("Local build");

    private final String label;

    ModStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Whether a mod in this state is allowed to reach the game directory. */
    public boolean isLaunchable() {
        return this == VERIFIED || this == LOCAL_DEV;
    }
}
