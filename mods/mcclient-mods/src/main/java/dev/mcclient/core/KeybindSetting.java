package dev.mcclient.core;

import org.lwjgl.input.Keyboard;

/**
 * A bindable key.
 *
 * <p>Unlike every other setting, this one cannot be cycled -- stepping through a hundred key codes
 * a click at a time would be useless. The menu instead puts it into capture mode and assigns the
 * next key pressed, which is how every client that has ever had a keybind screen does it.
 */
public final class KeybindSetting extends Setting {

    public static final int NONE = 0;

    private int keyCode;
    private boolean capturing;

    public KeybindSetting(String key, String name, int defaultCode) {
        super(key, name);
        this.keyCode = defaultCode;
    }

    public int keyCode() {
        return keyCode;
    }

    public void set(int keyCode) {
        this.keyCode = keyCode;
        this.capturing = false;
    }

    public boolean isBound() {
        return keyCode != NONE;
    }

    public boolean isCapturing() {
        return capturing;
    }

    public void beginCapture() {
        capturing = true;
    }

    public void cancelCapture() {
        capturing = false;
    }

    /** True while the bound key is physically held. */
    public boolean isDown() {
        if (!isBound()) {
            return false;
        }
        try {
            return Keyboard.isKeyDown(keyCode);
        } catch (RuntimeException e) {
            // Keyboard not initialised (headless/test); treat as not pressed.
            return false;
        }
    }

    @Override
    public String valueLabel() {
        if (capturing) {
            return "> press a key <";
        }
        return isBound() ? keyName(keyCode) : "None";
    }

    /** Capture is driven by the menu, so a click must not quietly change the binding. */
    @Override
    public void cycle() {
        beginCapture();
    }

    @Override
    public String serialise() {
        return String.valueOf(keyCode);
    }

    @Override
    public void deserialise(String raw) {
        try {
            keyCode = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            keyCode = NONE;
        }
    }

    public static String keyName(int code) {
        if (code == NONE) {
            return "None";
        }
        try {
            String name = Keyboard.getKeyName(code);
            return name == null || name.isEmpty() ? ("Key " + code) : name;
        } catch (RuntimeException e) {
            return "Key " + code;
        }
    }
}
