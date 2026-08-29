package dev.mcclient.launcher.gui;

import dev.mcclient.launcher.GameLog;
import dev.mcclient.launcher.LauncherPaths;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** The game's own output, so a crash is something you can read instead of an exit code. */
final class LogsPanel extends JPanel {

    private final GameLog log;
    private final JTextArea view = new JTextArea();
    private final JCheckBox follow = new JCheckBox("Follow");
    private final JLabel status = Theme.label(" ", 12, Font.PLAIN, Theme.TEXT_DIM);

    LogsPanel(GameLog log) {
        this.log = log;

        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(Theme.pad(26, 28, 24, 28));

        add(header(), BorderLayout.NORTH);
        add(logView(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);

        // Lines arrive on the log pump thread; Swing must only be touched on the EDT.
        log.addListener(line -> SwingUtilities.invokeLater(() -> appendLine(line)));
    }

    private Component header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(Theme.pad(0, 0, 16, 0));

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        JLabel title = Theme.label("Logs", 28, Font.BOLD, Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        Path file = log.file();
        JLabel note = Theme.label(file == null ? "In memory only" : file.toString(),
                12, Font.PLAIN, Theme.TEXT_DIM);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(title);
        titles.add(Box.createVerticalStrut(4));
        titles.add(note);

        follow.setSelected(true);
        follow.setOpaque(false);
        follow.setForeground(Theme.TEXT_DIM);
        follow.setToolTipText("Scroll to the newest line as it arrives");

        header.add(titles, BorderLayout.CENTER);
        header.add(follow, BorderLayout.EAST);
        return header;
    }

    private Component logView() {
        view.setEditable(false);
        view.setBackground(Theme.SURFACE);
        view.setForeground(Theme.TEXT_DIM);
        view.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        view.setBorder(Theme.pad(12, 14, 12, 14));

        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.SURFACE);
        return scroll;
    }

    private Component footer() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.setBorder(Theme.pad(14, 0, 0, 0));

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setOpaque(false);

        JButton openFolder = Theme.button("Open logs folder", Theme.SURFACE_ALT, Theme.BORDER, Theme.TEXT, 12);
        openFolder.addActionListener(e -> open(LauncherPaths.root().resolve("logs")));

        JButton openCrashes = Theme.button("Crash reports", Theme.SURFACE_ALT, Theme.BORDER, Theme.TEXT, 12);
        openCrashes.setToolTipText("Minecraft writes its own crash reports here");
        openCrashes.addActionListener(e ->
                open(LauncherPaths.root().resolve("game").resolve("crash-reports")));

        JButton copy = Theme.button("Copy all", Theme.SURFACE_ALT, Theme.BORDER, Theme.TEXT, 12);
        copy.addActionListener(e -> {
            view.selectAll();
            view.copy();
            view.select(view.getDocument().getLength(), view.getDocument().getLength());
            status.setText("Log copied to the clipboard.");
        });

        buttons.add(openFolder);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(openCrashes);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(copy);

        footer.add(status, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    /** Replays whatever was captured before this panel existed. */
    void refresh() {
        view.setText("");
        List<String> lines = log.snapshot();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            text.append(lines.get(i)).append('\n');
        }
        view.setText(text.length() == 0 ? "Nothing logged yet. Press PLAY.\n" : text.toString());
        scrollToEnd();
    }

    private void appendLine(String line) {
        view.append(line + "\n");
        scrollToEnd();
    }

    private void scrollToEnd() {
        if (follow.isSelected()) {
            view.setCaretPosition(view.getDocument().getLength());
        }
    }

    private void open(Path path) {
        try {
            LauncherPaths.ensureDirectory(path);
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException | UnsupportedOperationException ex) {
            status.setText("Could not open " + path + ": " + ex.getMessage());
        }
    }
}
