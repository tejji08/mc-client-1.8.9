package dev.mcclient.bedwars;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns Hypixel's Bed Wars sidebar into structured team state.
 *
 * <p>Deliberately free of any Minecraft type: this is pure string work, which means it can be
 * unit-tested against real scoreboard text without a game running. That matters here, because the
 * only place to exercise it for real is a live Hypixel lobby.
 *
 * <p>It reads text the client is already rendering on screen. It asks the server for nothing and
 * reveals nothing the player cannot already see.
 */
public final class ScoreboardReader {

    private ScoreboardReader() {}

    /** e.g. "R Red: 3" -- a single letter, a team name, then a status token. */
    private static final Pattern TEAM_LINE =
            Pattern.compile("^\\s*([A-Za-z])\\s+([A-Za-z ]{2,16}?)\\s*:\\s*(.*)$");

    private static final Pattern SECTION_CODE = Pattern.compile("§[0-9a-fk-orA-FK-OR]");
    private static final Pattern COUNT = Pattern.compile("(\\d+)");
    private static final Pattern YOU_MARKER = Pattern.compile("(?i)\\(?YOU\\)?");

    /** Removes legacy section-sign colour/format codes. */
    public static String strip(String text) {
        if (text == null) {
            return "";
        }
        return SECTION_CODE.matcher(text).replaceAll("");
    }

    /** True when the sidebar title looks like a Bed Wars game rather than a lobby or another mode. */
    public static boolean isBedwars(String title) {
        String clean = strip(title).toUpperCase().replace(" ", "");
        return clean.contains("BEDWARS");
    }

    /**
     * Parses a single sidebar row.
     *
     * @return the team it describes, or null when the line is something else (a stat, a date, blank)
     */
    public static TeamState parseTeamLine(String rawLine) {
        String line = strip(rawLine).trim();
        if (line.isEmpty()) {
            return null;
        }
        Matcher matcher = TEAM_LINE.matcher(line);
        if (!matcher.matches()) {
            return null;
        }

        String letter = matcher.group(1).toUpperCase();
        String name = matcher.group(2).trim();
        String rest = matcher.group(3).trim();

        // "YOU" marks the player's own team; strip it before reading the status token.
        boolean self = rest.toUpperCase().contains("YOU");
        String statusPart = YOU_MARKER.matcher(rest).replaceAll("").trim();

        BedStatus status;
        int playersLeft = -1;
        if (containsAny(statusPart, "✔", "✓")) {
            status = BedStatus.ALIVE;
        } else if (containsAny(statusPart, "✘", "✗", "✕", "×")) {
            status = BedStatus.ELIMINATED;
        } else {
            Matcher count = COUNT.matcher(statusPart);
            if (count.find()) {
                // Bed gone, but this many players are still standing.
                status = BedStatus.BROKEN;
                playersLeft = Integer.parseInt(count.group(1));
            } else {
                status = BedStatus.UNKNOWN;
            }
        }
        return new TeamState(letter, name, status, playersLeft, self);
    }

    /** e.g. "Final Kills: 2" -- a label and a plain number. */
    private static final Pattern STAT_LINE = Pattern.compile("^\\s*([A-Za-z][A-Za-z ]*?)\\s*:\\s*(\\d+)\\s*$");

    /**
     * Your own stats for the current match, straight off the sidebar: kills, final kills, beds
     * broken -- whatever Hypixel chose to put there, keyed by its own label.
     *
     * <p>No API key and no network call: these numbers are already on screen, sent by the server,
     * and authoritative. Team rows are skipped, since "G Green: 2" is a survivor count, not a stat.
     */
    public static Map<String, Integer> readStats(List<String> lines) {
        Map<String, Integer> stats = new LinkedHashMap<String, Integer>();
        if (lines == null) {
            return stats;
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = strip(lines.get(i)).trim();
            if (line.isEmpty() || parseTeamLine(line) != null) {
                continue;
            }
            Matcher matcher = STAT_LINE.matcher(line);
            if (matcher.matches()) {
                try {
                    stats.put(matcher.group(1).trim(), Integer.valueOf(Integer.parseInt(matcher.group(2))));
                } catch (NumberFormatException e) {
                    // A number too large to be a Bed Wars stat is not one we care about.
                }
            }
        }
        return stats;
    }

    /** Parses every team row out of a full sidebar, preserving the order they appear in. */
    public static List<TeamState> readTeams(List<String> lines) {
        List<TeamState> teams = new ArrayList<TeamState>();
        if (lines == null) {
            return teams;
        }
        for (int i = 0; i < lines.size(); i++) {
            TeamState team = parseTeamLine(lines.get(i));
            if (team != null) {
                teams.add(team);
            }
        }
        return teams;
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (int i = 0; i < needles.length; i++) {
            if (haystack.contains(needles[i])) {
                return true;
            }
        }
        return false;
    }
}
