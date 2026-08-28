package dev.mcclient.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * The one place that knows every module, and the single properties file they all persist to.
 *
 * <p>One file rather than one per module: the menu writes on every click, and a single flush is
 * both simpler and harder to leave half-written.
 */
public final class ModuleRegistry {

    private static final List<Module> MODULES = new ArrayList<Module>();
    private static File configFile;

    private ModuleRegistry() {}

    public static void register(Module module) {
        MODULES.add(module);
    }

    public static List<Module> modules() {
        return Collections.unmodifiableList(MODULES);
    }

    public static Module byId(String id) {
        for (int i = 0; i < MODULES.size(); i++) {
            if (MODULES.get(i).id().equals(id)) {
                return MODULES.get(i);
            }
        }
        return null;
    }

    /**
     * Re-reads the config file from disk, discarding unsaved in-memory state. Useful after editing
     * the properties by hand, or to undo a session of fiddling without restarting the game.
     */
    public static void reload() {
        if (configFile != null) {
            applyFrom(configFile);
        }
    }

    /** Points the registry at the game directory and restores saved values. */
    public static void load(File gameDir) {
        configFile = new File(new File(gameDir, "config"), "mcclient.properties");
        if (!configFile.isFile()) {
            save();
            return;
        }
        applyFrom(configFile);
    }

    private static void applyFrom(File file) {
        if (!file.isFile()) {
            return;
        }
        Properties props = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            props.load(in);
        } catch (IOException e) {
            System.out.println("[mcclient] could not read config, using defaults: " + e.getMessage());
            return;
        } finally {
            close(in);
        }

        for (int i = 0; i < MODULES.size(); i++) {
            Module module = MODULES.get(i);
            apply(props, module, module.enabledSetting());
            List<Setting> settings = module.settings();
            for (int j = 0; j < settings.size(); j++) {
                apply(props, module, settings.get(j));
            }
        }
    }

    private static void apply(Properties props, Module module, Setting setting) {
        String raw = props.getProperty(module.id() + "." + setting.key());
        if (raw != null) {
            setting.deserialise(raw);
        }
    }

    public static void save() {
        if (configFile == null) {
            return;
        }
        Properties props = new Properties();
        for (int i = 0; i < MODULES.size(); i++) {
            Module module = MODULES.get(i);
            props.setProperty(module.id() + ".enabled", module.enabledSetting().serialise());
            List<Setting> settings = module.settings();
            for (int j = 0; j < settings.size(); j++) {
                Setting setting = settings.get(j);
                props.setProperty(module.id() + "." + setting.key(), setting.serialise());
            }
        }
        FileOutputStream out = null;
        try {
            File parent = configFile.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
                return;
            }
            out = new FileOutputStream(configFile);
            props.store(out, "mc-client modules -- edited in-game with Right Shift");
        } catch (IOException e) {
            System.out.println("[mcclient] could not write config: " + e.getMessage());
        } finally {
            close(out);
        }
    }

    private static void close(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                // Nothing useful to do on close failure here.
            }
        }
    }
}
