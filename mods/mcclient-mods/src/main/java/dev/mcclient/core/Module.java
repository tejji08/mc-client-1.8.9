package dev.mcclient.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One client feature the player can turn on, off, and configure.
 *
 * <p>Modules live in a single jar on purpose. A settings registry split across several jars would
 * mean several copies of the static state, so the menu could only ever see its own -- which is why
 * real clients ship one artefact with modules inside rather than a pile of separate mods.
 */
public abstract class Module {

    private final String id;
    private final String name;
    private final String description;
    private final List<Setting> settings = new ArrayList<Setting>();
    private final BooleanSetting enabled;
    private final KeybindSetting toggleKey;

    protected Module(String id, String name, String description, boolean enabledByDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = new BooleanSetting("enabled", "Enabled", enabledByDefault);
        // Every module gets a toggle key. Added last so it sits at the bottom of the settings list.
        this.toggleKey = new KeybindSetting("toggleKey", "Toggle key", KeybindSetting.NONE);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean on) {
        boolean was = enabled.get();
        enabled.set(on);
        if (was != on) {
            onToggled(on);
        }
    }

    public BooleanSetting enabledSetting() {
        return enabled;
    }

    public KeybindSetting toggleKey() {
        return toggleKey;
    }

    /** False for the pseudo-module that only holds client-wide keybinds. */
    public boolean canDisable() {
        return true;
    }

    /** Settings other than the enable toggle, in menu order, with the toggle key last. */
    public List<Setting> settings() {
        List<Setting> all = new ArrayList<Setting>(settings);
        all.add(toggleKey);
        return Collections.unmodifiableList(all);
    }

    protected <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    /** Hook for modules that need to act the moment they're switched (fullbright restoring gamma). */
    protected void onToggled(boolean on) {
        // Most modules are simply read each frame and need nothing here.
    }

    /** Called after config load, once the client and its options exist. */
    public void onClientReady() {
        // Optional.
    }
}
