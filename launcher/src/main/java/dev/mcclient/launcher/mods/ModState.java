package dev.mcclient.launcher.mods;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mcclient.launcher.LauncherPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which mods the user has switched on. Stored as explicit per-id choices rather than a flat list,
 * so a mod added to the manifest later still honours its {@code enabledByDefault} instead of
 * silently arriving switched off.
 */
public final class ModState {

    private final Path file;
    private final Map<String, Boolean> choices = new LinkedHashMap<>();

    public ModState() {
        this(LauncherPaths.root().resolve("mod-state.json"));
    }

    ModState(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String id : root.keySet()) {
                choices.put(id, root.get(id).getAsBoolean());
            }
        } catch (IOException | RuntimeException ignored) {
            // A corrupt state file just means "no explicit choices yet" -- defaults take over.
        }
    }

    public boolean isEnabled(ModEntry entry) {
        if (entry.required()) {
            return true;
        }
        return choices.getOrDefault(entry.id(), entry.enabledByDefault());
    }

    public void setEnabled(ModEntry entry, boolean enabled) throws IOException {
        if (entry.required() && !enabled) {
            throw new IllegalArgumentException("'" + entry.id() + "' is marked required and can't be disabled");
        }
        choices.put(entry.id(), enabled);
        save();
    }

    private void save() throws IOException {
        JsonObject root = new JsonObject();
        choices.forEach(root::addProperty);
        Files.createDirectories(file.getParent());
        Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
    }
}
