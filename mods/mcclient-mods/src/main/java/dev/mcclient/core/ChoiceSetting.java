package dev.mcclient.core;

/**
 * A small fixed set of numeric values, cycled through. Used for things like HUD scale, where a
 * handful of sensible steps beats a slider we would have to draw ourselves.
 */
public final class ChoiceSetting extends Setting {

    private final float[] values;
    private final String suffix;
    private int index;

    public ChoiceSetting(String key, String name, float[] values, float initial, String suffix) {
        super(key, name);
        this.values = values;
        this.suffix = suffix;
        this.index = nearest(initial);
    }

    public float get() {
        return values[index];
    }

    @Override
    public String valueLabel() {
        return trim(values[index]) + suffix;
    }

    @Override
    public void cycle() {
        index = (index + 1) % values.length;
    }

    @Override
    public String serialise() {
        return String.valueOf(values[index]);
    }

    @Override
    public void deserialise(String raw) {
        try {
            index = nearest(Float.parseFloat(raw.trim()));
        } catch (NumberFormatException e) {
            // Keep whatever we started with rather than blowing up on a hand-edited file.
        }
    }

    /** Snap to the closest offered value, so a hand-edited config still lands somewhere valid. */
    private int nearest(float target) {
        int best = 0;
        float bestDistance = Math.abs(values[0] - target);
        for (int i = 1; i < values.length; i++) {
            float distance = Math.abs(values[i] - target);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private static String trim(float value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }
}
