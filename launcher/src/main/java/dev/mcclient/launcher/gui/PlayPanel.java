package dev.mcclient.launcher.gui;

import dev.mcclient.launcher.GameLauncher;
import dev.mcclient.launcher.GameLog;
import dev.mcclient.launcher.LauncherSettings;
import dev.mcclient.launcher.Progress;
import dev.mcclient.launcher.auth.SessionResolver;
import dev.mcclient.launcher.auth.model.MinecraftSession;
import dev.mcclient.launcher.mods.ModManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.net.http.HttpClient;

/** The front page: sign in, verify, launch, and watch it happen. */
final class PlayPanel extends JPanel {

    private final JFrame owner;
    private final HttpClient http;
    private final ModManager mods;
    private final LauncherSettings settings;
    private final GameLog gameLog;

    private final JButton play = Theme.button("PLAY", Theme.ACCENT, Theme.ACCENT_HOVER, Theme.TEXT, 20);
    private final JLabel status = Theme.label("Ready", 13, Font.PLAIN, Theme.TEXT_DIM);
    private final JProgressBar bar = new JProgressBar();
    private final JTextArea log = new JTextArea();

    PlayPanel(JFrame owner, HttpClient http, ModManager mods, LauncherSettings settings, GameLog gameLog) {
        this.owner = owner;
        this.http = http;
        this.mods = mods;
        this.settings = settings;
        this.gameLog = gameLog;

        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(Theme.pad(26, 28, 24, 28));

        add(header(), BorderLayout.NORTH);
        add(logView(), BorderLayout.CENTER);
        add(controls(), BorderLayout.SOUTH);
    }

    private Component header() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = Theme.label("Minecraft 1.8.9", 28, Font.BOLD, Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        int modCount = mods.manifest().mods().size();
        JLabel subtitle = Theme.label(
                "Legacy Fabric  ·  " + modCount + " mods in the verified bundle  ·  " + settings.memoryMb() + " MB",
                13, Font.PLAIN, Theme.TEXT_DIM);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(18));
        return header;
    }

    private Component logView() {
        log.setEditable(false);
        log.setBackground(Theme.SURFACE);
        log.setForeground(Theme.TEXT_DIM);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setBorder(Theme.pad(12, 14, 12, 14));
        log.setText("Press PLAY. First launch downloads the 1.8.9 assets, which takes a few minutes.\n");

        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.SURFACE);
        return scroll;
    }

    private Component controls() {
        bar.setStringPainted(false);
        bar.setBackground(Theme.SURFACE_ALT);
        bar.setForeground(Theme.ACCENT);
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(0, 6));

        play.setPreferredSize(new Dimension(190, 52));
        play.addActionListener(e -> launch());

        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        row.setBorder(Theme.pad(16, 0, 0, 0));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.setBorder(Theme.pad(12, 0, 0, 0));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(status);
        left.add(Box.createVerticalStrut(8));
        left.add(bar);

        row.add(left, BorderLayout.CENTER);
        row.add(play, BorderLayout.EAST);
        return row;
    }

    private void launch() {
        play.setEnabled(false);
        play.setText("WORKING");
        bar.setValue(0);
        append("");

        Progress progress = new Progress() {
            @Override
            public void status(String message) {
                SwingUtilities.invokeLater(() -> {
                    status.setText(message);
                    append(message);
                });
            }

            @Override
            public void bytes(long done, long total) {
                if (total <= 0) {
                    return;
                }
                int percent = (int) (done * 100 / total);
                SwingUtilities.invokeLater(() -> bar.setValue(percent));
            }
        };

        new SwingWorker<Process, Void>() {
            @Override
            protected Process doInBackground() throws Exception {
                MinecraftSession session = new SessionResolver(http).resolve(progress, new SignInDialog(owner));
                return new GameLauncher(http, mods, gameLog).launch(session, settings, progress);
            }

            @Override
            protected void done() {
                try {
                    Process game = get();
                    status.setText("Game running");
                    append("Game started (pid " + game.pid() + ").");
                    play.setText("RUNNING");
                    // Re-arm the button when the game exits, so a crash doesn't strand the launcher.
                    game.onExit().thenRun(() -> SwingUtilities.invokeLater(() -> {
                        int code = game.exitValue();
                        append("Game exited with code " + code + ".");
                        if (code != 0) {
                            // A bare exit code tells you nothing; the last thing the game said does.
                            status.setText("Game crashed (exit " + code + ") -- see the Logs tab");
                            append("");
                            append("--- last lines before exit ---");
                            for (String line : gameLog.tail(15)) {
                                append(line);
                            }
                        } else {
                            status.setText("Ready");
                        }
                        play.setText("PLAY");
                        play.setEnabled(true);
                        bar.setValue(0);
                    }));
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    status.setText("Failed: " + cause.getMessage());
                    append("ERROR: " + cause);
                    play.setText("PLAY");
                    play.setEnabled(true);
                    bar.setValue(0);
                }
            }
        }.execute();
    }

    private void append(String line) {
        log.append(line + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }
}
