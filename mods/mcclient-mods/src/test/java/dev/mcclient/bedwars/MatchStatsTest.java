package dev.mcclient.bedwars;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The local match-stats path: sidebar numbers plus deaths inferred from chat. Both are pure string
 * work, so both can be pinned down here without a live Hypixel game.
 */
class MatchStatsTest {

    private static final List<String> SIDEBAR = Arrays.asList(
            "§f§l02/14/24 §8m4Cs2",
            "",
            "§fKills: §a5",
            "§fFinal Kills: §a2",
            "§fBeds Broken: §a1",
            "",
            "§cR §fRed: §a✔",
            "§9B §fBlue: §a✔ §7YOU",
            "§aG §fGreen: §a2",
            "",
            "§fwww.hypixel.net");

    @Test
    void readsOwnStatsOffTheSidebar() {
        Map<String, Integer> stats = ScoreboardReader.readStats(SIDEBAR);

        assertEquals(Integer.valueOf(5), stats.get("Kills"));
        assertEquals(Integer.valueOf(2), stats.get("Final Kills"));
        assertEquals(Integer.valueOf(1), stats.get("Beds Broken"));
    }

    @Test
    void doesNotMistakeTeamRowsForStats() {
        Map<String, Integer> stats = ScoreboardReader.readStats(SIDEBAR);

        // "G Green: 2" is a survivor count, not a stat line.
        assertFalse(stats.containsKey("G Green"), "team survivor counts must not leak into stats");
        assertFalse(stats.containsKey("Green"));
        assertEquals(3, stats.size(), "only the three real stat rows");
    }

    @Test
    void countsAnOrdinaryDeath() {
        assertEquals(DeathWatcher.Event.DEATH,
                DeathWatcher.classify("§7Steve was killed by §cAlex§7.", "Steve"));
        assertEquals(DeathWatcher.Event.DEATH,
                DeathWatcher.classify("Steve fell into the void.", "Steve"));
    }

    @Test
    void countsAFinalDeath() {
        assertEquals(DeathWatcher.Event.FINAL_DEATH,
                DeathWatcher.classify("Steve was killed by Alex. FINAL KILL!", "Steve"));
    }

    @Test
    void ignoresSomeoneElseDying() {
        assertEquals(DeathWatcher.Event.NONE,
                DeathWatcher.classify("Alex was killed by Steve. FINAL KILL!", "Steve"));
    }

    @Test
    void ignoresYourOwnChatMessages() {
        // "Steve: gg" is you talking. Counting it as a death would be a silent, constant over-count.
        assertEquals(DeathWatcher.Event.NONE, DeathWatcher.classify("Steve: gg", "Steve"));
        assertEquals(DeathWatcher.Event.NONE,
                DeathWatcher.classify("§b[MVP+] §fSteve§f: rushing red", "Steve"));
    }

    @Test
    void ignoresLongerNamesThatStartWithYours() {
        // Without the word-boundary check, SteveCat dying would count as Steve dying.
        assertEquals(DeathWatcher.Event.NONE,
                DeathWatcher.classify("SteveCat was killed by Alex.", "Steve"));
    }

    @Test
    void ignoresJoinAndLeaveNoise() {
        assertEquals(DeathWatcher.Event.NONE, DeathWatcher.classify("Steve joined the lobby!", "Steve"));
        assertEquals(DeathWatcher.Event.NONE, DeathWatcher.classify("Steve disconnected.", "Steve"));
    }

    @Test
    void handlesAMissingUsername() {
        assertEquals(DeathWatcher.Event.NONE, DeathWatcher.classify("Steve died.", null));
        assertEquals(DeathWatcher.Event.NONE, DeathWatcher.classify("Steve died.", ""));
    }

    @Test
    void statsAreEmptyOutsideBedwars() {
        assertTrue(ScoreboardReader.readStats(null).isEmpty());
        assertTrue(ScoreboardReader.readStats(Arrays.asList("", "  ", "§fwww.hypixel.net")).isEmpty());
    }
}
