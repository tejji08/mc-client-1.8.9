package dev.mcclient.core;

/** On/off. */
public final class BooleanSetting extends Setting {

    private boolean value;

    public BooleanSetting(String key, String name, boolean initial) {
        super(key, name);
        this.value = initial;
    }

    public boolean get() {
        return value;
    }

    public void set(boolean value) {
        this.value = value;
    }

    @Override
    public String valueLabel() {
        return value ? "ON" : "OFF";
    }

    @Override
    public void cycle() {
        value = !value;
    }

    @Override
    public String serialise() {
        return String.valueOf(value);
    }

    @Override
    public void deserialise(String raw) {
        value = Boolean.parseBoolean(raw.trim());
    }
}
