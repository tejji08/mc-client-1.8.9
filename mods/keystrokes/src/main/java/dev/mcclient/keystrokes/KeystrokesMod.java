package dev.mcclient.keystrokes;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

/** Entry point. Holds the single HUD instance the mixin renders through. */
public final class KeystrokesMod implements ModInitializer {

    private static KeystrokesHud hud;

    @Override
    public void onInitialize() {
        System.out.println("[keystrokes] loaded");
    }

    /**
     * Built lazily: the config lives in the game directory, which isn't resolvable until the
     * client exists, and mod init runs before that.
     */
    public static KeystrokesHud hud() {
        if (hud == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return null;
            }
            KeystrokesConfig config = new KeystrokesConfig(client.runDirectory);
            config.load();
            hud = new KeystrokesHud(config);
        }
        return hud;
    }
}
