package dev.mcclient.core;

/** A named set of choices, e.g. which screen corner a HUD anchors to. */
public final class OptionSetting extends Setting {

    private final String[] options;
    private int index;

    public OptionSetting(String key, String name, String[] options, int initial) {
        super(key, name);
        this.options = options;
        this.index = clamp(initial);
    }

    public int index() {
        return index;
    }

    public String value() {
        return options[index];
    }

    @Override
    public String valueLabel() {
        return options[index];
    }

    @Override
    public void cycle() {
        index = (index + 1) % options.length;
    }

    @Override
    public String serialise() {
        return options[index];
    }

    @Override
    public void deserialise(String raw) {
        String wanted = raw.trim();
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(wanted)) {
                index = i;
                return;
            }
        }
        // Unknown value in a hand-edited file: keep the default rather than failing.
    }

    private int clamp(int i) {
        if (i < 0) {
            return 0;
        }
        return i >= options.length ? options.length - 1 : i;
    }
}
