package dev.mcclient.hud;

import dev.mcclient.core.BooleanSetting;
import dev.mcclient.core.HudModule;
import dev.mcclient.mixin.FpsAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * FPS, memory and ping in one panel.
 *
 * <p>All three are the client's own numbers: the frame counter it already keeps for F3, its own JVM
 * heap, and the latency the server reports for you in the player list.
 */
public final class ResourceMonitorModule extends HudModule {

    private static final long MB = 1024L * 1024L;

    private final BooleanSetting showFps;
    private final BooleanSetting showMemory;
    private final BooleanSetting showPing;

    public ResourceMonitorModule() {
        super("resources", "Resource Monitor", "FPS, memory use and ping.", true, 0.02f, 0.02f);
        showFps = add(new BooleanSetting("showFps", "Show FPS", true));
        showMemory = add(new BooleanSetting("showMemory", "Show memory", true));
        showPing = add(new BooleanSetting("showPing", "Show ping", true));
    }

    @Override
    public void draw(MinecraftClient client, boolean preview) {
        List<String> rows = new ArrayList<String>();
        if (showFps.get()) {
            rows.add(fps() + " FPS");
        }
        if (showMemory.get()) {
            Runtime runtime = Runtime.getRuntime();
            long max = runtime.maxMemory() / MB;
            long used = (runtime.totalMemory() - runtime.freeMemory()) / MB;
            long percent = max <= 0 ? 0 : used * 100 / max;
            rows.add(used + "/" + max + " MB  " + percent + "%");
        }
        if (showPing.get()) {
            int ping = ping(client);
            rows.add(ping < 0 ? "-- ms" : ping + " ms");
        }
        if (rows.isEmpty()) {
            return;
        }
        Panel.draw(client, this, rows);
    }

    /** A failed accessor mixin should show a zero, not take the HUD down with it. */
    private static int fps() {
        try {
            return FpsAccessor.getCurrentFps();
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Your own latency as the server reports it, or -1 in singleplayer where there is none. */
    private static int ping(MinecraftClient client) {
        if (client.player == null) {
            return -1;
        }
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            return -1;
        }
        PlayerListEntry entry = handler.getPlayerListEntry(client.player.getGameProfile().getId());
        return entry == null ? -1 : entry.getLatency();
    }
}
