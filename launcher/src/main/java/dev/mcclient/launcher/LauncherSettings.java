package dev.mcclient.launcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** User-tweakable launch options, persisted next to everything else in the launcher root. */
public final class LauncherSettings {

    private final Path file;

    private int memoryMb = 2048;
    private int width = 925;
    private int height = 530;

    public LauncherSettings() {
        this(LauncherPaths.root().resolve("settings.json"));
    }

    LauncherSettings(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("memoryMb")) {
                memoryMb = root.get("memoryMb").getAsInt();
            }
            if (root.has("width")) {
                width = root.get("width").getAsInt();
            }
            if (root.has("height")) {
                height = root.get("height").getAsInt();
            }
        } catch (IOException | RuntimeException ignored) {
            // Corrupt settings just fall back to defaults rather than blocking launch.
        }
    }

    public void save() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("memoryMb", memoryMb);
        root.addProperty("width", width);
        root.addProperty("height", height);
        Files.createDirectories(file.getParent());
        Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
    }

    public int memoryMb() {
        return memoryMb;
    }

    public void setMemoryMb(int memoryMb) {
        this.memoryMb = Math.max(512, memoryMb);
    }

    public int width() {
        return width;
    }

    public void setWidth(int width) {
        this.width = Math.max(320, width);
    }

    public int height() {
        return height;
    }

    public void setHeight(int height) {
        this.height = Math.max(240, height);
    }
}
