package dev.mcclient;

import dev.mcclient.bedwars.BedwarsModule;
import dev.mcclient.bedwars.GeneratorTimersModule;
import dev.mcclient.bedwars.MatchStatsModule;
import dev.mcclient.core.ClientModule;
import dev.mcclient.core.HudModule;
import dev.mcclient.core.Module;
import dev.mcclient.core.ModuleRegistry;
import dev.mcclient.fullbright.FullbrightModule;
import dev.mcclient.hud.PotionHudModule;
import dev.mcclient.hud.ResourceMonitorModule;
import dev.mcclient.hud.TrackerModules;
import dev.mcclient.hud.TntTimerModule;
import dev.mcclient.hud.ZoomModule;
import dev.mcclient.keystrokes.KeystrokesModule;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

import java.util.List;

/**
 * Entry point for the client's own modules. One jar rather than several, so the settings registry
 * the menu reads is a single piece of state.
 */
public final class McClientMods implements ModInitializer {

    public static final ClientModule CLIENT = new ClientModule();
    public static final KeystrokesModule KEYSTROKES = new KeystrokesModule();
    public static final BedwarsModule BEDWARS = new BedwarsModule();
    public static final MatchStatsModule MATCH_STATS = new MatchStatsModule();
    public static final GeneratorTimersModule GEN_TIMERS = new GeneratorTimersModule();
    public static final PotionHudModule POTIONS = new PotionHudModule();
    public static final TntTimerModule TNT = new TntTimerModule();
    public static final ResourceMonitorModule RESOURCES = new ResourceMonitorModule();
    public static final TrackerModules.Speed SPEED = new TrackerModules.Speed();
    public static final TrackerModules.Armor ARMOR = new TrackerModules.Armor();
    public static final TrackerModules.Weapon WEAPON = new TrackerModules.Weapon();
    public static final TrackerModules.Arrows ARROWS = new TrackerModules.Arrows();
    public static final TrackerModules.Blocks BLOCKS = new TrackerModules.Blocks();
    public static final TrackerModules.Coordinates COORDS = new TrackerModules.Coordinates();
    public static final ZoomModule ZOOM = new ZoomModule();
    public static final FullbrightModule FULLBRIGHT = new FullbrightModule();

    private static boolean ready;

    @Override
    public void onInitialize() {
        ModuleRegistry.register(CLIENT);
        ModuleRegistry.register(KEYSTROKES);
        ModuleRegistry.register(BEDWARS);
        ModuleRegistry.register(MATCH_STATS);
        ModuleRegistry.register(GEN_TIMERS);
        ModuleRegistry.register(POTIONS);
        ModuleRegistry.register(TNT);
        ModuleRegistry.register(RESOURCES);
        ModuleRegistry.register(SPEED);
        ModuleRegistry.register(ARMOR);
        ModuleRegistry.register(WEAPON);
        ModuleRegistry.register(ARROWS);
        ModuleRegistry.register(BLOCKS);
        ModuleRegistry.register(COORDS);
        ModuleRegistry.register(ZOOM);
        ModuleRegistry.register(FULLBRIGHT);
        System.out.println("[mcclient] " + ModuleRegistry.modules().size() + " modules registered");
    }

    /**
     * Loads config and lets modules touch game options. Deferred until the client exists, because
     * mod init runs before the game directory and options are available.
     */
    public static void ensureReady() {
        if (ready) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        ready = true;
        ModuleRegistry.load(client.runDirectory);
        List<Module> modules = ModuleRegistry.modules();
        for (int i = 0; i < modules.size(); i++) {
            modules.get(i).onClientReady();
        }
    }

    /** Draws every enabled HUD module. */
    public static void drawHuds(MinecraftClient client, boolean preview) {
        List<Module> modules = ModuleRegistry.modules();
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            if (module instanceof HudModule && module.isEnabled()) {
                ((HudModule) module).draw(client, preview);
            }
        }
    }
}
