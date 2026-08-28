package dev.mcclient.bedwars;

/**
 * Works out, from a chat line, whether <em>you</em> just died.
 *
 * <p>The sidebar reports kills, final kills and beds broken, but not deaths -- so deaths are the
 * one match stat that has to come from chat. Still entirely local: this reads messages the client
 * already received and displayed.
 *
 * <p>Matching is deliberately shape-based rather than an exact list of Hypixel's death strings.
 * There are dozens of them ("fell into the void", "was shot by", "was killed by"), they change, and
 * a missed variant would silently under-count. The shape that actually distinguishes a death
 * message is: it opens with your name, and your name is not followed by a colon -- because a chat
 * message from you reads "name: hello", and a ranked one does not start with your name at all.
 */
public final class DeathWatcher {

    /** What a single chat line meant for the local player. */
    public enum Event {
        NONE,
        DEATH,
        FINAL_DEATH
    }

    private DeathWatcher() {}

    public static Event classify(String rawLine, String username) {
        if (username == null || username.isEmpty()) {
            return Event.NONE;
        }
        String line = ScoreboardReader.strip(rawLine).trim();
        if (!line.startsWith(username)) {
            return Event.NONE;
        }

        String rest = line.substring(username.length());
        if (rest.isEmpty()) {
            return Event.NONE;
        }
        // "name: hello" is you talking, not you dying.
        char next = rest.charAt(0);
        if (next == ':') {
            return Event.NONE;
        }
        // Guard against matching a longer name that merely starts with ours (Bob vs BobCat).
        if (next != ' ') {
            return Event.NONE;
        }

        String tail = rest.trim();
        if (tail.isEmpty()) {
            return Event.NONE;
        }
        // Lobby noise that also opens with the bare name.
        String upper = tail.toUpperCase();
        if (upper.startsWith("JOINED") || upper.startsWith("HAS JOINED")
                || upper.startsWith("LEFT") || upper.startsWith("DISCONNECTED")
                || upper.startsWith("RECONNECTED")) {
            return Event.NONE;
        }

        return upper.contains("FINAL KILL") ? Event.FINAL_DEATH : Event.DEATH;
    }
}
