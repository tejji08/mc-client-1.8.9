package dev.mcclient.keystrokes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Plain properties file in the game directory. No GUI editor yet, and no defaults fetched from
 * anywhere -- the launcher's no-remote-config rule applies to mods too.
 */
public final class KeystrokesConfig {

    public int x = 6;
    public int y = 90;
    public float scale = 1.0f;
    public boolean showCps = true;
    public boolean showSpacebar = true;

    private final File file;

    public KeystrokesConfig(File gameDir) {
        this.file = new File(new File(gameDir, "config"), "keystrokes.properties");
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
            showCps = boolOr(props, "showCps", showCps);
            showSpacebar = boolOr(props, "showSpacebar", showSpacebar);
        } catch (IOException e) {
            System.out.println("[keystrokes] could not read config, using defaults: " + e.getMessage());
        } finally {
            close(in);
        }
        // A zero or negative scale would silently render nothing at all.
        if (scale < 0.25f) {
            scale = 0.25f;
        }
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("x", String.valueOf(x));
        props.setProperty("y", String.valueOf(y));
        props.setProperty("scale", String.valueOf(scale));
        props.setProperty("showCps", String.valueOf(showCps));
        props.setProperty("showSpacebar", String.valueOf(showSpacebar));
        FileOutputStream out = null;
        try {
            File parent = file.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
                return;
            }
            out = new FileOutputStream(file);
            props.store(out, "mc-client keystrokes -- x/y are HUD position in scaled pixels");
        } catch (IOException e) {
            System.out.println("[keystrokes] could not write config: " + e.getMessage());
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

    private static boolean boolOr(Properties p, String key, boolean fallback) {
        String v = p.getProperty(key);
        return v == null ? fallback : Boolean.parseBoolean(v.trim());
    }
}
