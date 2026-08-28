package dev.mcclient.bedwars;

import dev.mcclient.core.ChoiceSetting;
import dev.mcclient.core.Corner;
import dev.mcclient.core.Module;
import dev.mcclient.core.OptionSetting;
import dev.mcclient.hud.Panel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Countdown to the next diamond and emerald spawn, pulled from the generators' own holograms.
 *
 * <p>Hypixel already draws "Spawns in 12 seconds" above each generator; this collects the nearest
 * ones into a corner so you don't have to be looking at the generator to know. Nothing is
 * predicted or modelled -- a modelled interval would drift the moment a generator is upgraded, so
 * the server's own hologram stays the source of truth.
 */
public final class GeneratorTimersModule extends Module {

    /** Hypixel stacks a generator's hologram over a few armour stands within a block of each other. */
    private static final double CLUSTER_RADIUS_SQ = 4.0;
    private static final double MAX_RANGE_SQ = 80.0 * 80.0;
    private static final int MAX_ROWS = 5;

    private static final int DIAMOND = 0xFF55FFFF;
    private static final int EMERALD = 0xFF55FF55;
    private static final int OTHER = 0xFFFFFFFF;
    private static final int URGENT = 0xFFFF5555;

    private final OptionSetting corner;
    private final ChoiceSetting scale;

    public GeneratorTimersModule() {
        super("gen-timers", "Generator Timers", "Diamond and emerald spawn countdowns.", true);
        corner = add(new OptionSetting("corner", "Position", Corner.NAMES, 3));
        scale = add(new ChoiceSetting("scale", "Scale", new float[] {0.75f, 1.0f, 1.25f, 1.5f}, 1.0f, "x"));
    }

    public void render(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }
        List<Cluster> clusters = collectClusters(client);
        if (clusters.isEmpty()) {
            return;
        }

        Collections.sort(clusters, new Comparator<Cluster>() {
            @Override
            public int compare(Cluster a, Cluster b) {
                // Soonest first; that is the number you actually want to read.
                return a.spawn.seconds() - b.spawn.seconds();
            }
        });

        List<String> rows = new ArrayList<String>();
        List<Integer> colours = new ArrayList<Integer>();
        int limit = Math.min(clusters.size(), MAX_ROWS);
        for (int i = 0; i < limit; i++) {
            Cluster cluster = clusters.get(i);
            rows.add(cluster.spawn.label() + "  " + cluster.spawn.seconds() + "s");
            colours.add(Integer.valueOf(colourFor(cluster.spawn)));
        }
        Panel.draw(client, corner.index(), scale.get(), rows, colours);
    }

    private int colourFor(GeneratorReader.Spawn spawn) {
        if (spawn.seconds() <= 3) {
            return URGENT;
        }
        if (spawn.type() == GeneratorReader.Type.DIAMOND) {
            return DIAMOND;
        }
        return spawn.type() == GeneratorReader.Type.EMERALD ? EMERALD : OTHER;
    }

    /**
     * Groups nearby named armour stands into one hologram each, then parses the joined text.
     * The tier, the resource name and the countdown are separate stands, so they only make sense
     * read together.
     */
    private List<Cluster> collectClusters(MinecraftClient client) {
        List<Cluster> clusters = new ArrayList<Cluster>();
        List<Entity> entities = client.world.loadedEntities;
        if (entities == null) {
            return clusters;
        }
        // Copy first: the entity list is mutated on the client thread while we read it.
        List<Entity> snapshot = new ArrayList<Entity>(entities);

        for (int i = 0; i < snapshot.size(); i++) {
            Entity entity = snapshot.get(i);
            if (!(entity instanceof ArmorStandEntity) || !entity.hasCustomName()) {
                continue;
            }
            if (client.player.squaredDistanceTo(entity) > MAX_RANGE_SQ) {
                continue;
            }
            String name = entity.getName() == null ? "" : entity.getName().asUnformattedString();
            if (name.isEmpty()) {
                continue;
            }
            Cluster target = null;
            for (int j = 0; j < clusters.size(); j++) {
                if (clusters.get(j).near(entity)) {
                    target = clusters.get(j);
                    break;
                }
            }
            if (target == null) {
                target = new Cluster(entity.x, entity.z);
                clusters.add(target);
            }
            target.text.append(' ').append(name);
        }

        List<Cluster> generators = new ArrayList<Cluster>();
        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);
            GeneratorReader.Spawn spawn = GeneratorReader.parse(cluster.text.toString());
            if (spawn != null) {
                cluster.spawn = spawn;
                generators.add(cluster);
            }
        }
        return generators;
    }

    /** One generator's hologram stack. */
    private static final class Cluster {

        private final double x;
        private final double z;
        private final StringBuilder text = new StringBuilder();
        private GeneratorReader.Spawn spawn;

        Cluster(double x, double z) {
            this.x = x;
            this.z = z;
        }

        boolean near(Entity entity) {
            double dx = entity.x - x;
            double dz = entity.z - z;
            return dx * dx + dz * dz <= CLUSTER_RADIUS_SQ;
        }
    }
}
