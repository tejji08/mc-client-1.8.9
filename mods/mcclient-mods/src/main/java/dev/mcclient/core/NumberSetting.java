package dev.mcclient.core;

/**
 * A continuous numeric setting, rendered as a real slider.
 *
 * <p>The earlier scale/zoom settings offered four fixed steps because cycling buttons were the only
 * widget on hand. A slider is what these actually want -- "a bit bigger" is a legitimate thing to
 * ask of a HUD, and four presets cannot answer it.
 */
public final class NumberSetting extends Setting {

    private final float min;
    private final float max;
    private final float step;
    private final String suffix;
    private final int decimals;

    private float value;

    public NumberSetting(String key, String name, float min, float max, float step, float initial,
                         String suffix, int decimals) {
        super(key, name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.decimals = decimals;
        this.value = clamp(initial);
    }

    public float get() {
        return value;
    }

    public int getInt() {
        return Math.round(value);
    }

    public void set(float raw) {
        // Snap to the step so a dragged slider lands on clean values rather than 1.0374827.
        float snapped = Math.round((raw - min) / step) * step + min;
        value = clamp(snapped);
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    /** Slider position, 0..1. */
    public float progress() {
        if (max - min <= 0.0f) {
            return 0.0f;
        }
        return (value - min) / (max - min);
    }

    public void setProgress(float progress) {
        set(min + progress * (max - min));
    }

    @Override
    public String valueLabel() {
        return format(value) + suffix;
    }

    /** Stepping one notch, so the setting still works from a keyboard or a click. */
    @Override
    public void cycle() {
        float next = value + step;
        value = next > max ? min : clamp(next);
    }

    @Override
    public String serialise() {
        return String.valueOf(value);
    }

    @Override
    public void deserialise(String raw) {
        try {
            value = clamp(Float.parseFloat(raw.trim()));
        } catch (NumberFormatException e) {
            // Keep the default rather than failing on a hand-edited file.
        }
    }

    private String format(float v) {
        if (decimals <= 0) {
            return String.valueOf(Math.round(v));
        }
        return String.format("%." + decimals + "f", Float.valueOf(v));
    }

    private float clamp(float v) {
        if (v < min) {
            return min;
        }
        return v > max ? max : v;
    }
}
