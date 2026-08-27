package dev.mcclient.bedwars;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Position and scale, stored beside the game. Nothing is fetched remotely. */
public final class BedwarsConfig {

    /** Negative x means "anchor to the right edge", which is the sane default for this panel. */
    public int x = -1;
    public int y = 60;
    public float scale = 1.0f;

    private final File file;

    public BedwarsConfig(File gameDir) {
        this.file = new File(new File(gameDir, "config"), "bedwars-hud.properties");
    }

    public void load() {
        if (!file.isFile()) {
            save();
            return;
        }
        Properties props = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            props.load(in);
            x = intOr(props, "x", x);
            y = intOr(props, "y", y);
            scale = floatOr(props, "scale", scale);
        } catch (IOException e) {
            System.out.println("[bedwars-hud] could not read config, using defaults: " + e.getMessage());
        } finally {
            close(in);
        }
        if (scale < 0.25f) {
            scale = 0.25f;
        }
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("x", String.valueOf(x));
        props.setProperty("y", String.valueOf(y));
        props.setProperty("scale", String.valueOf(scale));
        FileOutputStream out = null;
        try {
            File parent = file.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
                return;
            }
            out = new FileOutputStream(file);
            props.store(out, "mc-client bedwars-hud -- x=-1 anchors the panel to the right edge");
        } catch (IOException e) {
            System.out.println("[bedwars-hud] could not write config: " + e.getMessage());
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

    private static int intOr(Properties p, String key, int fallback) {
        try {
            return Integer.parseInt(p.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float floatOr(Properties p, String key, float fallback) {
        try {
            return Float.parseFloat(p.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
