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
 * Named HUD layouts.
 *
 * <p>Sixteen modules with eight settings each is a lot of state to be stuck with one arrangement
 * of: a dense read-everything layout suits Bed Wars, and a clean one suits everything else.
 * Profiles let both exist rather than forcing a re-drag every time.
 *
 * <p>Each profile is its own properties file under {@code config/mcclient/}, with the active name
 * in {@code active.txt}. Plain files on purpose -- a profile is then something you can copy to a
 * friend, or keep in version control, without the client needing to be involved.
 */
public final class ProfileStore {

    public static final String DEFAULT_NAME = "default";

    private static File root;
    private static String active = DEFAULT_NAME;

    private ProfileStore() {}

    /** Points the store at the game directory, migrating any pre-profile config it finds. */
    public static void init(File gameDir) {
        File config = new File(gameDir, "config");
        root = new File(config, "mcclient");
        if (!root.isDirectory() && !root.mkdirs()) {
            return;
        }
        File legacy = new File(config, "mcclient.properties");
        File defaultProfile = fileFor(DEFAULT_NAME);
        if (legacy.isFile() && !defaultProfile.isFile()) {
            // Carry an existing single-config setup forward rather than silently resetting it.
            copy(legacy, defaultProfile);
        }
        active = readActiveName();
    }

    public static boolean ready() {
        return root != null;
    }

    public static String active() {
        return active;
    }

    /** Profile names, sorted, always including the default. */
    public static List<String> names() {
        List<String> names = new ArrayList<String>();
        if (root != null) {
            File[] files = root.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    String name = files[i].getName();
                    if (name.endsWith(".properties")) {
                        names.add(name.substring(0, name.length() - ".properties".length()));
                    }
                }
            }
        }
        if (!names.contains(DEFAULT_NAME)) {
            names.add(DEFAULT_NAME);
        }
        Collections.sort(names);
        return names;
    }

    public static File activeFile() {
        return fileFor(active);
    }

    public static File fileFor(String name) {
        return new File(root, sanitise(name) + ".properties");
    }

    /** Switches profiles. The caller saves the outgoing one first and loads the incoming one after. */
    public static void setActive(String name) {
        active = sanitise(name);
        writeActiveName();
    }

    public static boolean exists(String name) {
        return fileFor(name).isFile();
    }

    public static void delete(String name) {
        if (DEFAULT_NAME.equals(sanitise(name))) {
            // Deleting the last profile would leave nothing to fall back to.
            return;
        }
        File file = fileFor(name);
        if (file.isFile() && !file.delete()) {
            System.out.println("[mcclient] could not delete profile " + name);
        }
    }

    /**
     * Strips anything that would escape the profiles folder or upset the filesystem. A profile
     * name reaches this from a text box, so it cannot be trusted to be a filename.
     */
    public static String sanitise(String name) {
        if (name == null) {
            return DEFAULT_NAME;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < name.length() && out.length() < 32; i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ' ') {
                out.append(c);
            }
        }
        String cleaned = out.toString().trim();
        return cleaned.isEmpty() ? DEFAULT_NAME : cleaned;
    }

    private static String readActiveName() {
        File marker = new File(root, "active.txt");
        if (!marker.isFile()) {
            return DEFAULT_NAME;
        }
        Properties props = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(marker);
            props.load(in);
            return sanitise(props.getProperty("active", DEFAULT_NAME));
        } catch (IOException e) {
            return DEFAULT_NAME;
        } finally {
            close(in);
        }
    }

    private static void writeActiveName() {
        if (root == null) {
            return;
        }
        Properties props = new Properties();
        props.setProperty("active", active);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(root, "active.txt"));
            props.store(out, "mc-client -- which HUD profile is in use");
        } catch (IOException e) {
            System.out.println("[mcclient] could not record active profile: " + e.getMessage());
        } finally {
            close(out);
        }
    }

    private static void copy(File from, File to) {
        Properties props = new Properties();
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(from);
            props.load(in);
            out = new FileOutputStream(to);
            props.store(out, "mc-client modules");
        } catch (IOException e) {
            System.out.println("[mcclient] could not migrate old config: " + e.getMessage());
        } finally {
            close(in);
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
