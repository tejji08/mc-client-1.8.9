package dev.mcclient.bedwars;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a Bed Wars generator's floating hologram text.
 *
 * <p>Hypixel already renders the countdown above every diamond and emerald generator -- this reads
 * the same armour-stand nametags the client is drawing, rather than modelling spawn intervals
 * ourselves. Modelling would drift the moment a generator is upgraded or the server lags; the
 * hologram is the server's own answer.
 *
 * <p>Pure string work and free of Minecraft types, so it can be tested without a live game.
 */
public final class GeneratorReader {

    public enum Type {
        DIAMOND,
        EMERALD,
        UNKNOWN
    }

    /** A generator's current state, as its hologram states it. */
    public static final class Spawn {

        private final Type type;
        private final int seconds;
        private final String tier;

        Spawn(Type type, int seconds, String tier) {
            this.type = type;
            this.seconds = seconds;
            this.tier = tier;
        }

        public Type type() {
            return type;
        }

        public int seconds() {
            return seconds;
        }

        /** Roman tier ("II"), or empty when the hologram doesn't say. */
        public String tier() {
            return tier;
        }

        public String label() {
            String name = type == Type.DIAMOND ? "Diamond" : type == Type.EMERALD ? "Emerald" : "Gen";
            return tier.isEmpty() ? name : name + " " + tier;
        }
    }

    private static final Pattern SPAWNS_IN = Pattern.compile("(?i)spawns?\\s+in\\s+(\\d+)");
    private static final Pattern BARE_SECONDS = Pattern.compile("(?i)(\\d+)\\s*(?:s\\b|seconds?)");
    private static final Pattern TIER = Pattern.compile("(?i)tier\\s+([IVX]+)");

    private GeneratorReader() {}

    /**
     * Parses the combined nametags of one generator's hologram stack.
     *
     * <p>Hypixel splits it over several armour stands ("Tier II", "Diamond", "Spawns in 12
     * seconds"), so the caller clusters them by position and passes the joined text.
     *
     * @return the generator's state, or null when this isn't a generator hologram
     */
    public static Spawn parse(String hologramText) {
        String text = ScoreboardReader.strip(hologramText);
        if (text.isEmpty()) {
            return null;
        }

        int seconds = -1;
        Matcher spawns = SPAWNS_IN.matcher(text);
        if (spawns.find()) {
            seconds = parseOrMinusOne(spawns.group(1));
        } else {
            Matcher bare = BARE_SECONDS.matcher(text);
            if (bare.find()) {
                seconds = parseOrMinusOne(bare.group(1));
            }
        }
        if (seconds < 0) {
            return null;
        }

        String lower = text.toLowerCase();
        Type type = lower.contains("diamond") ? Type.DIAMOND
                : lower.contains("emerald") ? Type.EMERALD
                : Type.UNKNOWN;

        String tier = "";
        Matcher tierMatch = TIER.matcher(text);
        if (tierMatch.find()) {
            tier = tierMatch.group(1).toUpperCase();
        }
        return new Spawn(type, seconds, tier);
    }

    private static int parseOrMinusOne(String raw) {
        try {
            int value = Integer.parseInt(raw);
            // A "countdown" of minutes is not a generator; guard against matching unrelated text.
            return value > 600 ? -1 : value;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
