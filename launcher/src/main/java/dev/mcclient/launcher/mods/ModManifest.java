package dev.mcclient.launcher.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mcclient.launcher.LauncherPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The curated mod catalog. Ships <em>with</em> the launcher as a bundled resource and is never
 * fetched from a server -- ARCHITECTURE.md commits to no remote config fetch, so a shipped build's
 * mod set can't be changed out from under the user. Only the jars themselves are downloaded, each
 * pinned to a sha256 recorded here.
 *
 * <p>A user-supplied manifest at {@code %APPDATA%/mc-client-1.8.9/mods-manifest.json} takes
 * precedence, so adding your own pinned mods doesn't require rebuilding.
 */
public final class ModManifest {

    private static final String BUNDLED_RESOURCE = "/mods-manifest.json";
    private static final int SUPPORTED_SCHEMA = 1;

    private final String minecraftVersion;
    private final String loader;
    private final List<ModEntry> mods;
    private final String origin;

    private ModManifest(String minecraftVersion, String loader, List<ModEntry> mods, String origin) {
        this.minecraftVersion = minecraftVersion;
        this.loader = loader;
        this.mods = List.copyOf(mods);
        this.origin = origin;
    }

    /** User override if present, otherwise the manifest bundled into the launcher jar. */
    public static ModManifest load() throws IOException {
        Path override = LauncherPaths.root().resolve("mods-manifest.json");
        if (Files.exists(override)) {
            return parse(Files.readString(override, StandardCharsets.UTF_8), override.toString());
        }
        try (InputStream in = ModManifest.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (in == null) {
                // Not fatal: a launcher with no catalog still runs local dev mods fine.
                return new ModManifest("1.8.9", "legacy-fabric", List.of(), "(none)");
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return parse(json, "bundled");
        }
    }

    static ModManifest parse(String json, String origin) throws IOException {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("mod manifest at " + origin + " is not valid JSON", e);
        }

        int schema = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : SUPPORTED_SCHEMA;
        if (schema != SUPPORTED_SCHEMA) {
            throw new IOException("mod manifest schemaVersion " + schema + " unsupported (expected " + SUPPORTED_SCHEMA + ")");
        }

        String mcVersion = root.has("minecraftVersion") ? root.get("minecraftVersion").getAsString() : "1.8.9";
        String loader = root.has("loader") ? root.get("loader").getAsString() : "legacy-fabric";

        List<ModEntry> entries = new ArrayList<>();
        Map<String, String> seen = new LinkedHashMap<>();
        JsonArray array = root.has("mods") ? root.getAsJsonArray("mods") : new JsonArray();
        for (JsonElement element : array) {
            JsonObject o = element.getAsJsonObject();
            ModEntry entry;
            try {
                entry = new ModEntry(
                        str(o, "id", null),
                        str(o, "name", str(o, "id", null)),
                        str(o, "version", ""),
                        str(o, "description", ""),
                        str(o, "category", "utility"),
                        str(o, "url", null),
                        str(o, "sha256", null),
                        o.has("sizeBytes") ? o.get("sizeBytes").getAsLong() : -1L,
                        str(o, "sourceUrl", ""),
                        str(o, "license", "unknown"),
                        o.has("required") && o.get("required").getAsBoolean(),
                        !o.has("enabledByDefault") || o.get("enabledByDefault").getAsBoolean());
            } catch (IllegalArgumentException e) {
                throw new IOException("bad entry in mod manifest at " + origin + ": " + e.getMessage(), e);
            }
            if (seen.put(entry.id(), entry.id()) != null) {
                throw new IOException("duplicate mod id '" + entry.id() + "' in manifest at " + origin);
            }
            entries.add(entry);
        }
        return new ModManifest(mcVersion, loader, entries, origin);
    }

    private static String str(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }

    public List<ModEntry> mods() {
        return mods;
    }

    public Optional<ModEntry> byId(String id) {
        return mods.stream().filter(m -> m.id().equals(id)).findFirst();
    }

    public String minecraftVersion() {
        return minecraftVersion;
    }

    public String loader() {
        return loader;
    }

    /** Where this manifest came from -- shown in the GUI so the active catalog is never a mystery. */
    public String origin() {
        return origin;
    }
}
