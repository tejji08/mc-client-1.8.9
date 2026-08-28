package dev.mcclient.bedwars;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercised against realistic Hypixel sidebar text, colour codes and all. This is the only part of
 * the mod that can be verified without a live Bed Wars game, so it carries the weight.
 */
class ScoreboardReaderTest {

    @Test
    void stripsSectionCodes() {
        assertEquals("R Red: OK", ScoreboardReader.strip("\u00a7cR \u00a7fRed: \u00a7aOK"));
        assertEquals("", ScoreboardReader.strip(null));
    }

    @Test
    void recognisesBedwarsTitle() {
        assertTrue(ScoreboardReader.isBedwars("\u00a7e\u00a7lBED WARS"));
        assertTrue(ScoreboardReader.isBedwars("BEDWARS"));
        assertFalse(ScoreboardReader.isBedwars("\u00a7e\u00a7lSKYWARS"));
        assertFalse(ScoreboardReader.isBedwars("\u00a7e\u00a7lHYPIXEL"));
    }

    @Test
    void parsesBedAlive() {
        TeamState team = ScoreboardReader.parseTeamLine("\u00a7cR \u00a7fRed: \u00a7a\u2714");
        assertEquals("R", team.letter());
        assertEquals("Red", team.name());
        assertEquals(BedStatus.ALIVE, team.status());
        assertEquals(-1, team.playersLeft());
        assertFalse(team.isSelf());
    }

    @Test
    void parsesBedBrokenWithSurvivorCount() {
        TeamState team = ScoreboardReader.parseTeamLine("\u00a7eY \u00a7fYellow: \u00a7a3");
        assertEquals(BedStatus.BROKEN, team.status());
        assertEquals(3, team.playersLeft());
    }

    @Test
    void parsesEliminatedTeam() {
        TeamState team = ScoreboardReader.parseTeamLine("\u00a7aG \u00a7fGreen: \u00a7c\u2718");
        assertEquals(BedStatus.ELIMINATED, team.status());
        assertEquals(-1, team.playersLeft());
    }

    @Test
    void detectsYourOwnTeam() {
        TeamState team = ScoreboardReader.parseTeamLine("\u00a79B \u00a7fBlue: \u00a7a\u2714 \u00a77YOU");
        assertTrue(team.isSelf());
        assertEquals(BedStatus.ALIVE, team.status(), "the YOU marker must not swallow the status");
    }

    @Test
    void ignoresStatLinesAndChrome() {
        // These share the "word: value" shape and must not be mistaken for teams.
        assertNull(ScoreboardReader.parseTeamLine("Kills: 5"));
        assertNull(ScoreboardReader.parseTeamLine("Final Kills: 2"));
        assertNull(ScoreboardReader.parseTeamLine("Beds Broken: 1"));
        assertNull(ScoreboardReader.parseTeamLine("\u00a7fwww.hypixel.net"));
        assertNull(ScoreboardReader.parseTeamLine("  "));
        assertNull(ScoreboardReader.parseTeamLine("02/14/24 \u00a78m4Cs2"));
    }

    @Test
    void readsAFullSidebar() {
        List<String> sidebar = Arrays.asList(
                "\u00a7f\u00a7l02/14/24 \u00a78m4Cs2",
                "",
                "\u00a7fKills: \u00a7a5",
                "\u00a7fFinal Kills: \u00a7a2",
                "\u00a7fBeds Broken: \u00a7a1",
                "",
                "\u00a7cR \u00a7fRed: \u00a7a\u2714",
                "\u00a79B \u00a7fBlue: \u00a7a\u2714 \u00a77YOU",
                "\u00a7aG \u00a7fGreen: \u00a7a2",
                "\u00a7eY \u00a7fYellow: \u00a7c\u2718",
                "",
                "\u00a7fwww.hypixel.net");

        List<TeamState> teams = ScoreboardReader.readTeams(sidebar);

        assertEquals(4, teams.size(), "exactly the four team rows, none of the chrome");
        assertEquals("Red", teams.get(0).name());
        assertEquals(BedStatus.ALIVE, teams.get(0).status());
        assertTrue(teams.get(1).isSelf());
        assertEquals(BedStatus.BROKEN, teams.get(2).status());
        assertEquals(2, teams.get(2).playersLeft());
        assertEquals(BedStatus.ELIMINATED, teams.get(3).status());
    }
}
