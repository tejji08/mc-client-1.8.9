package dev.mcclient.hud;

import dev.mcclient.core.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Fuse countdown for nearby primed TNT.
 *
 * <p>The fuse lives on the TNT entity the client was already sent and is already rendering, so
 * nothing hidden is exposed -- but it does put a number on something players normally judge by
 * eye, and unlike this client's other modules it is <strong>not clearly on Hypixel's allowed
 * list</strong>. Off by default for that reason: enabling it on Hypixel is a decision for whoever
 * is playing, and worth checking against their current rules first.
 */
public final class TntTimerModule extends HudModule {

    private static final double MAX_RANGE_SQ = 40.0 * 40.0;
    private static final int MAX_ROWS = 5;
    private static final int NORMAL = 0xFFFFAA00;
    private static final int URGENT = 0xFFFF5555;
    private static final int URGENT_TICKS = 20;

    public TntTimerModule() {
        super("tnt-timers", "TNT Timers", "Fuse countdown for nearby TNT. Off by default -- see README.",
                false, 0.55f, 0.62f);
    }

    @Override
    public void draw(MinecraftClient client, boolean preview) {
        List<TntEntity> nearby = collect(client);
        if (nearby.isEmpty()) {
            if (preview) {
                drawSample(client);
            }
            return;
        }
        Collections.sort(nearby, new Comparator<TntEntity>() {
            @Override
            public int compare(TntEntity a, TntEntity b) {
                return a.fuseTimer - b.fuseTimer;
            }
        });

        List<String> rows = new ArrayList<String>();
        List<Integer> colours = new ArrayList<Integer>();
        int limit = Math.min(nearby.size(), MAX_ROWS);
        for (int i = 0; i < limit; i++) {
            TntEntity tnt = nearby.get(i);
            int blocks = (int) Math.sqrt(client.player.squaredDistanceTo(tnt));
            rows.add("TNT  " + Format.fuse(tnt.fuseTimer) + "  " + blocks + "m");
            colours.add(Integer.valueOf(tnt.fuseTimer <= URGENT_TICKS ? URGENT : NORMAL));
        }
        Panel.draw(client, this, rows, colours);
    }

    private List<TntEntity> collect(MinecraftClient client) {
        List<TntEntity> nearby = new ArrayList<TntEntity>();
        if (client.world == null || client.player == null || client.world.loadedEntities == null) {
            return nearby;
        }
        // Copy first: the entity list is mutated on the client thread while we read it.
        List<Entity> snapshot = new ArrayList<Entity>(client.world.loadedEntities);
        for (int i = 0; i < snapshot.size(); i++) {
            Entity entity = snapshot.get(i);
            if (entity instanceof TntEntity && client.player.squaredDistanceTo(entity) <= MAX_RANGE_SQ) {
                nearby.add((TntEntity) entity);
            }
        }
        return nearby;
    }

    /** No live TNT means nothing to drag, so the editor shows a sample row. */
    private void drawSample(MinecraftClient client) {
        List<String> rows = new ArrayList<String>();
        List<Integer> colours = new ArrayList<Integer>();
        rows.add("TNT  2.4s  6m");
        colours.add(Integer.valueOf(NORMAL));
        Panel.draw(client, this, rows, colours);
    }
}
