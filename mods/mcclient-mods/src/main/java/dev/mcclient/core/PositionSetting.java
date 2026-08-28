package dev.mcclient.core;

/**
 * Where a HUD panel sits, stored as a fraction of the screen so it stays put across resolutions
 * and GUI scales -- absolute pixels would drift the moment either changed.
 *
 * <p>Not clickable in the settings menu: it is dragged in the HUD editor, and the button that
 * carries it opens that editor.
 */
public final class PositionSetting extends Setting {

    private float x;
    private float y;

    // Bounds of the last drawn frame, in real screen pixels, so the editor can hit-test and drag.
    private int lastX;
    private int lastY;
    private int lastWidth;
    private int lastHeight;
    private boolean drawnThisFrame;

    public PositionSetting(String key, String name, float x, float y) {
        super(key, name);
        this.x = clamp(x);
        this.y = clamp(y);
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public void set(float x, float y) {
        this.x = clamp(x);
        this.y = clamp(y);
    }

    public void recordBounds(int screenX, int screenY, int width, int height) {
        this.lastX = screenX;
        this.lastY = screenY;
        this.lastWidth = width;
        this.lastHeight = height;
        this.drawnThisFrame = true;
    }

    public boolean wasDrawn() {
        return drawnThisFrame;
    }

    public void clearDrawn() {
        drawnThisFrame = false;
    }

    public int lastX() {
        return lastX;
    }

    public int lastY() {
        return lastY;
    }

    public int lastWidth() {
        return lastWidth;
    }

    public int lastHeight() {
        return lastHeight;
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= lastX && mouseX <= lastX + lastWidth
                && mouseY >= lastY && mouseY <= lastY + lastHeight;
    }

    @Override
    public String valueLabel() {
        return "drag in editor";
    }

    @Override
    public void cycle() {
        // Handled by the menu, which opens the HUD editor instead.
    }

    @Override
    public String serialise() {
        return x + "," + y;
    }

    @Override
    public void deserialise(String raw) {
        String[] parts = raw.split(",");
        if (parts.length != 2) {
            return;
        }
        try {
            x = clamp(Float.parseFloat(parts[0].trim()));
            y = clamp(Float.parseFloat(parts[1].trim()));
        } catch (NumberFormatException e) {
            // Keep the default rather than failing on a hand-edited file.
        }
    }

    private static float clamp(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        return value > 1.0f ? 1.0f : value;
    }
}
