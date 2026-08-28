package dev.mcclient.keystrokes;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rolling clicks-per-second over a one-second window.
 *
 * <p>Deliberately a plain counter with no input hooks of its own: it is fed edge transitions by
 * whatever is already polling the key, which keeps it unit-testable and keeps this mod firmly on
 * the "reads state the client already has" side of the line. It cannot generate a click.
 */
public final class ClickCounter {

    private static final long WINDOW_MS = 1000L;

    private final Deque<Long> clicks = new ArrayDeque<Long>();
    private boolean wasDown;

    /** Feed the key's current state each frame; a false->true transition counts as one click. */
    public void update(boolean down, long nowMs) {
        if (down && !wasDown) {
            clicks.addLast(nowMs);
        }
        wasDown = down;
        prune(nowMs);
    }

    public int cps(long nowMs) {
        prune(nowMs);
        return clicks.size();
    }

    public boolean isDown() {
        return wasDown;
    }

    private void prune(long nowMs) {
        while (!clicks.isEmpty() && nowMs - clicks.peekFirst() >= WINDOW_MS) {
            clicks.removeFirst();
        }
    }
}
