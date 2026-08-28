package dev.mcclient.bedwars;

/** One parsed team row from the Bed Wars sidebar. Java 8, so a plain class rather than a record. */
public final class TeamState {

    private final String letter;
    private final String name;
    private final BedStatus status;
    private final int playersLeft;
    private final boolean self;

    public TeamState(String letter, String name, BedStatus status, int playersLeft, boolean self) {
        this.letter = letter;
        this.name = name;
        this.status = status;
        this.playersLeft = playersLeft;
        this.self = self;
    }

    public String letter() {
        return letter;
    }

    public String name() {
        return name;
    }

    public BedStatus status() {
        return status;
    }

    /** Players remaining once the bed is gone, or -1 when the scoreboard doesn't say. */
    public int playersLeft() {
        return playersLeft;
    }

    public boolean isSelf() {
        return self;
    }

    @Override
    public String toString() {
        return letter + " " + name + " " + status + (playersLeft >= 0 ? " x" + playersLeft : "") + (self ? " (you)" : "");
    }
}
