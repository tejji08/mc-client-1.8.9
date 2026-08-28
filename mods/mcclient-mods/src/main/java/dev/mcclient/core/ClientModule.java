package dev.mcclient.core;

/**
 * Holds the client-wide keybinds -- the settings menu and the HUD editor.
 *
 * <p>A module rather than a special case so the menu can render it with the machinery it already
 * has. It cannot be disabled: turning off the key that opens the menu would strand the player with
 * no way back in.
 */
public final class ClientModule extends Module {

    /** LWJGL keycodes: Right Shift, and Right Control for the editor. */
    private static final int RIGHT_SHIFT = 54;
    private static final int RIGHT_CONTROL = 157;

    private final KeybindSetting menuKey;
    private final KeybindSetting editorKey;

    public ClientModule() {
        super("client", "Client", "Keys for the settings menu and HUD editor.", true);
        menuKey = add(new KeybindSetting("menuKey", "Settings menu", RIGHT_SHIFT));
        editorKey = add(new KeybindSetting("editorKey", "HUD editor", RIGHT_CONTROL));
    }

    public KeybindSetting menuKey() {
        return menuKey;
    }

    public KeybindSetting editorKey() {
        return editorKey;
    }

    @Override
    public boolean canDisable() {
        return false;
    }
}
