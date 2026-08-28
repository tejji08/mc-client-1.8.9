package dev.mcclient.hud;

import dev.mcclient.core.BooleanSetting;
import dev.mcclient.core.HudModule;
import dev.mcclient.core.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The small read-only trackers, grouped in one file because each is a handful of lines over the
 * same shape: look at the local player, format a row, hand it to {@link Panel}.
 *
 * <p>Every one reads the client's own state -- your inventory, your position, your armour. None of
 * them asks the server for anything or reveals anything about another player.
 */
public final class TrackerModules {

    private TrackerModules() {}

    /** Horizontal movement speed in blocks per second. */
    public static final class Speed extends HudModule {

        private static final int SAMPLES = 5;
        private final float[] recent = new float[SAMPLES];
        private int index;

        private final BooleanSetting smooth;

        public Speed() {
            super("speed", "Speed", "Horizontal movement speed in blocks per second.", true,
                    0.02f, 0.20f);
            smooth = add(new BooleanSetting("smooth", "Smoothed", true));
        }

        @Override
        public void draw(MinecraftClient client, boolean preview) {
            if (client.player == null) {
                if (preview) {
                    Panel.draw(client, this, one("4.32 b/s"));
                }
                return;
            }
            double dx = client.player.x - client.player.prevX;
            double dz = client.player.z - client.player.prevZ;
            // Per-tick distance, and 20 ticks make a second.
            float instant = (float) (Math.sqrt(dx * dx + dz * dz) * 20.0);

            recent[index] = instant;
            index = (index + 1) % SAMPLES;

            float shown = instant;
            if (smooth.get()) {
                float total = 0f;
                for (int i = 0; i < SAMPLES; i++) {
                    total += recent[i];
                }
                shown = total / SAMPLES;
            }
            Panel.draw(client, this, one(String.format("%.2f b/s", Float.valueOf(shown))));
        }
    }

    /** Your four armour pieces and how much life they have left. */
    public static final class Armor extends HudModule {

        private final BooleanSetting hideEmpty;

        public Armor() {
            super("armor", "Armor Tracker", "Equipped armour with remaining durability.", true,
                    0.02f, 0.40f);
            hideEmpty = add(new BooleanSetting("hideEmpty", "Hide empty slots", true));
        }

        @Override
        public void draw(MinecraftClient client, boolean preview) {
            if (client.player == null || client.player.inventory == null) {
                if (preview) {
                    Panel.draw(client, this, one("Diamond Helmet  87%"));
                }
                return;
            }
            ItemStack[] armor = client.player.inventory.armor;
            List<String> rows = new ArrayList<String>();
            List<Integer> colours = new ArrayList<Integer>();
            // Vanilla stores boots first; helmet-down reads the way players think about it.
            for (int slot = armor.length - 1; slot >= 0; slot--) {
                ItemStack stack = armor[slot];
                if (stack == null) {
                    if (!hideEmpty.get()) {
                        rows.add("--");
                        colours.add(Integer.valueOf(0xFF777777));
                    }
                    continue;
                }
                rows.add(EquipmentModules.describe(stack, true));
                colours.add(Integer.valueOf(EquipmentModules.durabilityColour(stack, textColour())));
            }
            if (rows.isEmpty()) {
                if (preview) {
                    Panel.draw(client, this, one("No armour"));
                }
                return;
            }
            Panel.draw(client, this, rows, colours);
        }
    }

    /** Whatever is in your hand, with its durability. */
    public static final class Weapon extends HudModule {

        public Weapon() {
            super("weapon", "Weapon Tracker", "Held item with remaining durability.", true,
                    0.02f, 0.50f);
        }

        @Override
        public void draw(MinecraftClient client, boolean preview) {
            ItemStack held = EquipmentModules.held(client);
            if (held == null) {
                if (preview) {
                    Panel.draw(client, this, one("Diamond Sword  92%"));
                }
                return;
            }
            List<String> rows = one(EquipmentModules.describe(held, true));
            List<Integer> colours = new ArrayList<Integer>();
            colours.add(Integer.valueOf(EquipmentModules.durabilityColour(held, textColour())));
            Panel.draw(client, this, rows, colours);
        }
    }

    /** How many arrows you are carrying. */
    public static final class Arrows extends HudModule {

        private final NumberSetting lowThreshold;

        public Arrows() {
            super("arrows", "Arrow Tracker", "Arrows left in your inventory.", true, 0.02f, 0.55f);
            lowThreshold = add(new NumberSetting("low", "Warn below", 0f, 64f, 1f, 4f, "", 0));
        }

        @Override
        public void draw(MinecraftClient client, boolean preview) {
            if (client.player == null || client.player.inventory == null) {
                if (preview) {
                    Panel.draw(client, this, one("Arrows  16"));
                }
                return;
            }
            int count = countMatching(client, ArrowMatch.INSTANCE);
            List<Integer> colours = new ArrayList<Integer>();
            colours.add(Integer.valueOf(count <= lowThreshold.getInt() ? 0xFFFF5555 : textColour()));
            Panel.draw(client, this, one("Arrows  " + count), colours);
        }
    }

    /** How many placeable blocks you are carrying -- the number that decides a Bed Wars bridge. */
    public static final class Blocks extends HudModule {

        private final NumberSetting lowThreshold;

        public Blocks() {
            super("blocks", "Blocks Tracker", "Placeable blocks left in your inventory.", true,
                    0.02f, 0.45f);
            lowThreshold = add(new NumberSetting("low", "Warn below", 0f, 128f, 1f, 8f, "", 0));
        }

        @Override
        public void draw(MinecraftClient client, boolean preview) {
            if (client.player == null || client.player.inventory == null) {
                if (preview) {
                    Panel.draw(client, this, one("Blocks  64"));
                }
                return;
            }
            int count = countMatching(client, BlockMatch.INSTANCE);
            List<Integer> colours = new ArrayList<Integer>();
            colours.add(Integer.valueOf(count <= lowThreshold.getInt() ? 0xFFFF5555 : textColour()));
            Panel.draw(client, this, one("Blocks  " + count), colours);
        }
    }

    /** Where you are, and which way you are facing. */
    public static final class Coordinates extends HudModule {

        private static final String[] FACING = {"South", "South West", "West", "North West",
                                                "North", "North East", "East", "South East"};

        private final BooleanSetting showFacing;

        public Coordinates() {
            super("coords", "Coordinates", "Your position and facing.", false, 0.02f, 0.10f);
            showFacing = add(new BooleanSetting("showFacing", "Show facing", true));
        }

        @Override
        public void draw(MinecraftClient client, boolean preview) {
            if (client.player == null) {
                if (preview) {
                    Panel.draw(client, this, one("0, 64, 0"));
                }
                return;
            }
            List<String> rows = new ArrayList<String>();
            rows.add(String.format("%.0f, %.0f, %.0f",
                    Double.valueOf(client.player.x),
                    Double.valueOf(client.player.y),
                    Double.valueOf(client.player.z)));
            if (showFacing.get()) {
                // Yaw runs from -180..180 with 0 facing south; eight sectors of 45 degrees.
                int sector = Math.round(client.player.yaw / 45.0f) & 7;
                rows.add(FACING[sector]);
            }
            Panel.draw(client, this, rows);
        }
    }

    // ---- shared helpers -------------------------------------------------------------------

    private interface StackMatch {
        boolean matches(ItemStack stack);
    }

    private static final class ArrowMatch implements StackMatch {
        static final ArrowMatch INSTANCE = new ArrowMatch();

        @Override
        public boolean matches(ItemStack stack) {
            return stack.getItem() == Items.ARROW;
        }
    }

    private static final class BlockMatch implements StackMatch {
        static final BlockMatch INSTANCE = new BlockMatch();

        @Override
        public boolean matches(ItemStack stack) {
            // Anything that places as a block: wool, wood, obsidian, whatever the map gives you.
            return stack.getItem() instanceof BlockItem;
        }
    }

    private static int countMatching(MinecraftClient client, StackMatch match) {
        int total = 0;
        ItemStack[] main = client.player.inventory.main;
        for (int i = 0; i < main.length; i++) {
            ItemStack stack = main[i];
            if (stack != null && match.matches(stack)) {
                total += stack.count;
            }
        }
        return total;
    }

    private static List<String> one(String row) {
        List<String> rows = new ArrayList<String>(1);
        rows.add(row);
        return rows;
    }
}
