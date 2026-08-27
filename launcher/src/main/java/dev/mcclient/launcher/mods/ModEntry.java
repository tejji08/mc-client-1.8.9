package dev.mcclient.launcher.mods;

/**
 * One pinned mod in the curated bundle. Everything needed to fetch it and prove it wasn't
 * tampered with in transit lives here -- notably {@code sha256}, which is checked on install
 * and again before every launch.
 *
 * @param sourceUrl where the source lives, so the bundle stays auditable (see ARCHITECTURE.md)
 */
public record ModEntry(
        String id,
        String name,
        String version,
        String description,
        String category,
        String url,
        String sha256,
        long sizeBytes,
        String sourceUrl,
        String license,
        boolean required,
        boolean enabledByDefault) {

    public ModEntry {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("mod entry needs an id");
        }
        // A pinned bundle is the entire security story here -- an unpinned entry is a hole in it.
        if (sha256 == null || !sha256.matches("(?i)[0-9a-f]{64}")) {
            throw new IllegalArgumentException("mod '" + id + "' needs a 64-char sha256 (got: " + sha256 + ")");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("mod '" + id + "' needs a download url");
        }
    }

    /** Filename this mod is stored under, both in the cache and in the game's mods folder. */
    public String fileName() {
        return id + "-" + version + ".jar";
    }

    public String displayVersion() {
        return version == null || version.isBlank() ? "?" : version;
    }
}
