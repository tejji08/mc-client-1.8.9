package dev.mcclient.core;

/**
 * A text colour, chosen from Minecraft's own sixteen chat colours.
 *
 * <p>A free RGB picker would need a colour wheel widget that 1.8.9 does not have, and the vanilla
 * palette is what the rest of the game is drawn in anyway -- a HUD in those colours looks like it
 * belongs. Stored as ARGB so a hand-edited config can still set anything.
 */
public final class ColorSetting extends Setting {

    private static final String[] NAMES = {
        "White", "Light Gray", "Gray", "Black",
        "Red", "Dark Red", "Gold", "Yellow",
        "Green", "Dark Green", "Aqua", "Dark Aqua",
        "Blue", "Dark Blue", "Pink", "Purple"
    };

    private static final int[] VALUES = {
        0xFFFFFFFF, 0xFFAAAAAA, 0xFF555555, 0xFF000000,
        0xFFFF5555, 0xFFAA0000, 0xFFFFAA00, 0xFFFFFF55,
        0xFF55FF55, 0xFF00AA00, 0xFF55FFFF, 0xFF00AAAA,
        0xFF5555FF, 0xFF0000AA, 0xFFFF55FF, 0xFFAA00AA
    };

    private int argb;

    public ColorSetting(String key, String name, int initialArgb) {
        super(key, name);
        this.argb = initialArgb;
    }

    public int get() {
        return argb;
    }

    @Override
    public String valueLabel() {
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i] == argb) {
                return NAMES[i];
            }
        }
        return String.format("#%06X", Integer.valueOf(argb & 0xFFFFFF));
    }

    @Override
    public void cycle() {
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i] == argb) {
                argb = VALUES[(i + 1) % VALUES.length];
                return;
            }
        }
        // A hand-edited colour is not in the palette; step to the start rather than sticking.
        argb = VALUES[0];
    }

    @Override
    public String serialise() {
        return String.format("#%08X", Integer.valueOf(argb));
    }

    @Override
    public void deserialise(String raw) {
        String text = raw.trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        try {
            long parsed = Long.parseLong(text, 16);
            // A six-digit value means no alpha was given; assume fully opaque.
            argb = text.length() <= 6 ? (int) (0xFF000000L | parsed) : (int) parsed;
        } catch (NumberFormatException e) {
            // Keep the default rather than failing on a hand-edited file.
        }
    }
}
