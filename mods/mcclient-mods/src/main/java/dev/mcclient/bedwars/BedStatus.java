package dev.mcclient.bedwars;

/** What the sidebar is telling us about one team's bed. */
public enum BedStatus {

    /** Bed still standing -- that team can respawn. */
    ALIVE,
    /** Bed gone, but players still alive; the scoreboard shows how many. */
    BROKEN,
    /** Bed gone and everyone dead. Out of the game. */
    ELIMINATED,
    /** Line recognised as a team, but the status token wasn't one we know. */
    UNKNOWN
}
