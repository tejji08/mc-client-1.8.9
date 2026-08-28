package dev.mcclient.core;

/**
 * One tweakable value on a module.
 *
 * <p>Everything is expressed as "show me a label, and cycle to the next value when clicked".
 * 1.8.9's GUI toolkit only really has buttons -- no sliders, no checkboxes -- so building the menu
 * out of cycling buttons keeps it native-looking instead of hand-rolling widgets that would sit
 * badly next to the vanilla screens.
 */
public abstract class Setting {

    private final String key;
    private final String name;

    protected Setting(String key, String name) {
        this.key = key;
        this.name = name;
    }

    /** Identifier used in the properties file. Stable; changing it orphans the saved value. */
    public String key() {
        return key;
    }

    public String name() {
        return name;
    }

    /** Current value, formatted for a button face. */
    public abstract String valueLabel();

    /** Advance to the next value, wrapping at the end. */
    public abstract void cycle();

    /** Serialised form for the config file. */
    public abstract String serialise();

    /** Restore from the config file. Must tolerate junk without throwing. */
    public abstract void deserialise(String raw);
}
