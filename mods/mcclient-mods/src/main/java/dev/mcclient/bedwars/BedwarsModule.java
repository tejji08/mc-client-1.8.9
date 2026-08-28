package dev.mcclient.bedwars;

import dev.mcclient.core.HudModule;
import dev.mcclient.hud.Panel;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact bed/team readout for Bed Wars.
 *
 * <p>The vanilla sidebar already carries this, but spread over a tall column that is slow to read
 * mid-fight. This condenses it to one row per team. Everything shown is a reformatting of the
 * scoreboard the server already sent and the client already draws.
 */
public final class BedwarsModule extends HudModule {

    public BedwarsModule() {
        super("bedwars-hud", "Bed Wars HUD", "Condensed per-team bed status on Hypixel.", true,
                0.80f, 0.10f);
    }

    @Override
    public void draw(MinecraftClient client, boolean preview) {
        List<TeamState> teams = client.world == null ? new ArrayList<TeamState>() : SidebarSource.teams(client);
        if (teams.isEmpty()) {
            if (!preview) {
                return;
            }
            // Nothing to show outside a Bed Wars game, so the editor needs something to drag.
            teams = sample();
        }

        List<String> rows = new ArrayList<String>(teams.size());
        List<Integer> colours = new ArrayList<Integer>(teams.size());
        for (int i = 0; i < teams.size(); i++) {
            rows.add(format(teams.get(i)));
            colours.add(Integer.valueOf(colourFor(teams.get(i))));
        }
        Panel.draw(client, this, rows, colours);
    }

    private static List<TeamState> sample() {
        List<TeamState> teams = new ArrayList<TeamState>();
        teams.add(new TeamState("R", "Red", BedStatus.ALIVE, -1, false));
        teams.add(new TeamState("B", "Blue", BedStatus.ALIVE, -1, true));
        teams.add(new TeamState("G", "Green", BedStatus.BROKEN, 2, false));
        teams.add(new TeamState("Y", "Yellow", BedStatus.ELIMINATED, -1, false));
        return teams;
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

}
