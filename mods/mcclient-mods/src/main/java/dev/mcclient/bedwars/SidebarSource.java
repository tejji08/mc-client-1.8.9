package dev.mcclient.bedwars;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Lifts the Bed Wars sidebar out of the scoreboard as plain strings, so both HUD modules read it
 * the same way and {@link ScoreboardReader} can stay free of Minecraft types.
 */
public final class SidebarSource {

    private static final int SIDEBAR_SLOT = 1;
    private static final int MAX_SIDEBAR_ROWS = 15;

    private SidebarSource() {}

    /**
     * The sidebar exactly as vanilla renders it: highest score at the top, team prefix and suffix
     * applied, capped at the 15 rows the sidebar can actually show. Empty unless this is Bed Wars.
     */
    public static List<String> lines(MinecraftClient client) {
        if (client == null || client.world == null) {
            return Collections.emptyList();
        }
        Scoreboard scoreboard = client.world.getScoreboard();
        if (scoreboard == null) {
            return Collections.emptyList();
        }
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(SIDEBAR_SLOT);
        if (objective == null || !ScoreboardReader.isBedwars(objective.getDisplayName())) {
            return Collections.emptyList();
        }

        List<ScoreboardPlayerScore> scores =
                new ArrayList<ScoreboardPlayerScore>(scoreboard.getAllPlayerScores(objective));
        // Vanilla hides entries whose "player name" starts with '#'.
        for (int i = scores.size() - 1; i >= 0; i--) {
            String name = scores.get(i).getPlayerName();
            if (name == null || name.startsWith("#")) {
                scores.remove(i);
            }
        }
        Collections.sort(scores, new Comparator<ScoreboardPlayerScore>() {
            @Override
            public int compare(ScoreboardPlayerScore a, ScoreboardPlayerScore b) {
                return b.getScore() - a.getScore();
            }
        });

        List<String> lines = new ArrayList<String>();
        int limit = Math.min(scores.size(), MAX_SIDEBAR_ROWS);
        for (int i = 0; i < limit; i++) {
            String name = scores.get(i).getPlayerName();
            Team team = scoreboard.getPlayerTeam(name);
            lines.add(Team.decorateName(team, name));
        }
        return lines;
    }

    public static List<TeamState> teams(MinecraftClient client) {
        return ScoreboardReader.readTeams(lines(client));
    }

    public static Map<String, Integer> stats(MinecraftClient client) {
        return ScoreboardReader.readStats(lines(client));
    }
}
