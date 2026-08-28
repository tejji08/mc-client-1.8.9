package dev.mcclient.bedwars;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.mcclient.core.ChoiceSetting;
import dev.mcclient.core.Corner;
import dev.mcclient.core.Module;
import dev.mcclient.core.OptionSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.Window;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Compact bed/team readout for Bed Wars.
 *
 * <p>The vanilla sidebar already carries this, but spread over a tall column that is slow to read
 * mid-fight. This condenses it to one row per team. Everything shown is a reformatting of the
 * scoreboard the server already sent and the client already draws.
 */
public final class BedwarsModule extends Module {

    private static final int SIDEBAR_SLOT = 1;
    private static final int MAX_SIDEBAR_ROWS = 15;
    private static final int PANEL_BG = 0x90000000;
    private static final int ROW_HEIGHT = 11;

    private final OptionSetting corner;
    private final ChoiceSetting scale;

    public BedwarsModule() {
        super("bedwars-hud", "Bed Wars HUD", "Condensed per-team bed status on Hypixel.", true);
        corner = add(new OptionSetting("corner", "Position", Corner.NAMES, 1));
        scale = add(new ChoiceSetting("scale", "Scale", new float[] {0.75f, 1.0f, 1.25f, 1.5f}, 1.0f, "x"));
    }

    public void render(MinecraftClient client) {
        if (client.world == null) {
            return;
        }
        List<TeamState> teams = readTeams(client);
        if (teams.isEmpty()) {
            return;
        }

        TextRenderer font = client.textRenderer;
        float s = scale.get();
        Window window = new Window(client);

        int width = 0;
        List<String> rows = new ArrayList<String>(teams.size());
        for (int i = 0; i < teams.size(); i++) {
            String row = format(teams.get(i));
            rows.add(row);
            width = Math.max(width, font.getStringWidth(row));
        }
        int height = rows.size() * ROW_HEIGHT;

        int screenWidth = (int) (window.getScaledWidth() / s);
        int screenHeight = (int) (window.getScaledHeight() / s);
        int x = Corner.x(corner.index(), screenWidth, width, 10);
        int y = Corner.y(corner.index(), screenHeight, height, 10);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.scale(s, s, 1.0f);

        DrawableHelper.fill(x - 4, y - 4, x + width + 4, y + height + 2, PANEL_BG);
        for (int i = 0; i < rows.size(); i++) {
            font.draw(rows.get(i), x, y + i * ROW_HEIGHT, colourFor(teams.get(i)));
        }

        GlStateManager.popMatrix();
    }

    /** "R Red  BED" / "G Green  x2" / "Y Yellow  OUT", with a marker on your own team. */
    private String format(TeamState team) {
        String status;
        switch (team.status()) {
            case ALIVE:
                status = "BED";
                break;
            case BROKEN:
                status = team.playersLeft() >= 0 ? "x" + team.playersLeft() : "--";
                break;
            case ELIMINATED:
                status = "OUT";
                break;
            default:
                status = "?";
                break;
        }
        return (team.isSelf() ? "> " : "  ") + team.letter() + " " + team.name() + "  " + status;
    }

    private int colourFor(TeamState team) {
        if (team.status() == BedStatus.ELIMINATED) {
            return 0xFF555555;
        }
        String letter = team.letter();
        if ("R".equals(letter)) {
            return 0xFFFF5555;
        }
        if ("B".equals(letter)) {
            return 0xFF5555FF;
        }
        if ("G".equals(letter)) {
            return 0xFF55FF55;
        }
        if ("Y".equals(letter)) {
            return 0xFFFFFF55;
        }
        if ("A".equals(letter)) {
            return 0xFF55FFFF;
        }
        if ("P".equals(letter)) {
            return 0xFFFF55FF;
        }
        if ("S".equals(letter)) {
            return 0xFFAAAAAA;
        }
        return 0xFFFFFFFF;
    }

    /**
     * Pulls the sidebar the same way vanilla renders it: highest score at the top, team
     * prefix/suffix applied, capped at the 15 rows the sidebar can actually show.
     */
    private List<TeamState> readTeams(MinecraftClient client) {
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
        return ScoreboardReader.readTeams(lines);
    }
}
