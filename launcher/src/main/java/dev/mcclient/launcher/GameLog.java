package dev.mcclient.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Captures the game's output so a crash is something you can read rather than an exit code.
 *
 * <p>The launcher previously inherited the game's stdio, which meant the log went wherever the
 * launcher happened to be started from -- and nowhere at all when it was started from a shortcut.
 * Now every line is kept in memory for the GUI, appended to a file, and still echoed to the console
 * for the headless path.
 */
public final class GameLog {

    /** Enough to hold a crash and the lead-up, without letting a chatty server eat all the heap. */
    private static final int MAX_LINES = 4000;

    private final Deque<String> lines = new ArrayDeque<String>();
    private final List<Consumer<String>> listeners = new ArrayList<Consumer<String>>();
    private final Path file;
    private final boolean echoToConsole;
    private Writer writer;
    private boolean writerFailed;

    public GameLog(Path file, boolean echoToConsole) {
        this.file = file;
        this.echoToConsole = echoToConsole;
    }

    /** Where the log is being written, so the GUI can offer to open it. */
    public Path file() {
        return file;
    }

    public synchronized void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    /** Everything captured so far, oldest first. */
    public synchronized List<String> snapshot() {
        return new ArrayList<String>(lines);
    }

    /** The last {@code count} lines -- what you want to show when something has just gone wrong. */
    public synchronized List<String> tail(int count) {
        List<String> all = new ArrayList<String>(lines);
        int from = Math.max(0, all.size() - count);
        return all.subList(from, all.size());
    }

    /**
     * Pumps the process output on a daemon thread. Reading it is not optional: a process whose
     * output nobody drains will eventually block on a full pipe buffer and appear to hang.
     */
    public void pump(InputStream stream) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        append(line);
                    }
                } catch (IOException e) {
                    append("[launcher] log capture ended: " + e.getMessage());
                }
            }
        }, "game-log");
        thread.setDaemon(true);
        thread.start();
    }

    public void append(String line) {
        List<Consumer<String>> snapshot;
        synchronized (this) {
            lines.addLast(line);
            while (lines.size() > MAX_LINES) {
                lines.removeFirst();
            }
            snapshot = new ArrayList<Consumer<String>>(listeners);
        }
        if (echoToConsole) {
            System.out.println(line);
        }
        writeLine(line);
        for (int i = 0; i < snapshot.size(); i++) {
            // A listener that throws must not take the log pump down with it.
            try {
                snapshot.get(i).accept(line);
            } catch (RuntimeException ignored) {
                // Nothing useful to do; the line is already captured.
            }
        }
    }

    /** Held open rather than reopened per line: a busy server logs thousands of them. */
    private synchronized void writeLine(String line) {
        if (file == null || writerFailed) {
            return;
        }
        try {
            if (writer == null) {
                LauncherPaths.ensureDirectory(file.getParent());
                writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            writer.write(line);
            writer.write(System.lineSeparator());
            // Flushed each line so a hard crash still leaves a complete log behind.
            writer.flush();
        } catch (IOException e) {
            // Losing the file copy is not worth interrupting a running game over, but retrying
            // on every subsequent line would be: give up once and keep the in-memory copy.
            writerFailed = true;
            writer = null;
        }
    }

    /** Closes the log file. The in-memory copy stays readable afterwards. */
    public synchronized void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
                // Nothing useful to do on close failure here.
            }
            writer = null;
        }
    }

    /** Starts a fresh file for a new launch, so the log is about this run and not the last ten. */
    public synchronized void startNewFile() {
        if (file == null) {
            return;
        }
        close();
        writerFailed = false;
        try {
            LauncherPaths.ensureDirectory(file.getParent());
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // An un-deletable old log just means this run appends to it.
        }
    }
}
