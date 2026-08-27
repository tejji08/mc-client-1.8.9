package dev.mcclient.bedwars;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

/** Entry point. Holds the single HUD instance the mixin renders through. */
public final class BedwarsMod implements ModInitializer {

    private static BedwarsHud hud;

    @Override
    public void onInitialize() {
        System.out.println("[bedwars-hud] loaded");
    }

    /** Built lazily -- the config lives in the game directory, which mod init runs too early to know. */
    public static BedwarsHud hud() {
        if (hud == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return null;
            }
            BedwarsConfig config = new BedwarsConfig(client.runDirectory);
            config.load();
            hud = new BedwarsHud(config);
        }
        return hud;
    }
}
