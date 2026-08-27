package dev.mcclient.launcher.gui;

import dev.mcclient.launcher.Progress;
import dev.mcclient.launcher.mods.InstalledMod;
import dev.mcclient.launcher.mods.ModEntry;
import dev.mcclient.launcher.mods.ModManager;
import dev.mcclient.launcher.mods.ModStatus;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.io.IOException;

/**
 * The mod list, and the honest version of it: every row states what the launcher can actually
 * prove about that jar right now, not what it hoped when it downloaded it.
 */
final class ModsPanel extends JPanel {

    private final ModManager mods;
    private final JPanel list = new JPanel();
    private final JLabel status = Theme.label(" ", 12, Font.PLAIN, Theme.TEXT_DIM);

    ModsPanel(ModManager mods) {
        this.mods = mods;

        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(Theme.pad(26, 28, 24, 28));

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(header(), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);

        refresh();
    }

    private Component header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(Theme.pad(0, 0, 16, 0));

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        JLabel title = Theme.label("Mods", 28, Font.BOLD, Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel note = Theme.label("Every jar is pinned to a sha256 in the bundled manifest. "
                + "A mismatch is blocked from the game folder.", 12, Font.PLAIN, Theme.TEXT_DIM);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(title);
        titles.add(Box.createVerticalStrut(4));
        titles.add(note);

        JButton verify = Theme.button("Re-verify all", Theme.SURFACE_ALT, Theme.BORDER, Theme.TEXT, 13);
        verify.addActionListener(e -> refresh());

        header.add(titles, BorderLayout.CENTER);
        header.add(verify, BorderLayout.EAST);
        return header;
    }

    private Component footer() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(Theme.pad(12, 0, 0, 0));
        footer.add(status, BorderLayout.CENTER);
        return footer;
    }

    /** Re-hashes everything on disk and rebuilds the rows. Cheap enough for a handful of jars. */
    void refresh() {
        list.removeAll();
        for (InstalledMod mod : mods.resolveAll()) {
            list.add(row(mod));
            list.add(Box.createVerticalStrut(8));
        }
        list.revalidate();
        list.repaint();
    }

    private JComponent row(InstalledMod mod) {
        ModEntry entry = mod.entry();

        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setBackground(Theme.SURFACE);
        row.setBorder(Theme.card());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

        JCheckBox toggle = new JCheckBox();
        toggle.setSelected(mod.enabled());
        toggle.setOpaque(false);
        toggle.setEnabled(!entry.required());
        toggle.setToolTipText(entry.required() ? "Required by the bundle" : "Load this mod at launch");
        toggle.addActionListener(e -> setEnabled(entry, toggle.isSelected()));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JPanel titleRow = new JPanel();
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.add(Theme.label(entry.name(), 15, Font.BOLD, Theme.TEXT));
        titleRow.add(Box.createHorizontalStrut(8));
        titleRow.add(Theme.label(entry.displayVersion(), 12, Font.PLAIN, Theme.TEXT_DIM));
        titleRow.add(Box.createHorizontalStrut(10));
        titleRow.add(Theme.badge(mod.status().label(), colorFor(mod.status())));
        titleRow.add(Box.createHorizontalGlue());

        JLabel description = Theme.label(entry.description(), 12, Font.PLAIN, Theme.TEXT_DIM);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel meta = Theme.label(entry.category() + "  ·  " + entry.license() + "  ·  "
                + (entry.sizeBytes() / 1024) + " KB", 11, Font.PLAIN, Theme.BORDER.brighter());
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(titleRow);
        text.add(Box.createVerticalStrut(3));
        text.add(description);
        text.add(Box.createVerticalStrut(3));
        text.add(meta);

        row.add(toggle, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);

        JComponent action = actionFor(mod);
        if (action != null) {
            // BorderLayout.EAST would stretch the button to the full row height -- centre it instead.
            JPanel holder = new JPanel(new GridBagLayout());
            holder.setOpaque(false);
            holder.add(action);
            row.add(holder, BorderLayout.EAST);
        }
        return row;
    }

    private JComponent actionFor(InstalledMod mod) {
        switch (mod.status()) {
            case NOT_INSTALLED -> {
                JButton install = Theme.button("Install", Theme.ACCENT, Theme.ACCENT_HOVER, Theme.TEXT, 12);
                install.addActionListener(e -> install(mod, install));
                return install;
            }
            case CORRUPT -> {
                JButton repair = Theme.button("Repair", Theme.DANGER, Theme.DANGER.brighter(), Theme.TEXT, 12);
                repair.setToolTipText("This jar does not match its pinned hash and will not be loaded.");
                repair.addActionListener(e -> install(mod, repair));
                return repair;
            }
            default -> {
                return null;
            }
        }
    }

    private void install(InstalledMod mod, JButton button) {
        button.setEnabled(false);
        button.setText("...");
        Progress progress = message -> SwingUtilities.invokeLater(() -> status.setText(message));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                mods.install(mod.entry(), progress);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    status.setText(mod.name() + " installed and verified.");
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    status.setText("Failed: " + cause.getMessage());
                }
                refresh();
            }
        }.execute();
    }

    private void setEnabled(ModEntry entry, boolean enabled) {
        try {
            mods.setEnabled(entry, enabled);
            status.setText(entry.name() + (enabled ? " enabled." : " disabled."));
        } catch (IOException e) {
            status.setText("Could not save mod state: " + e.getMessage());
        }
    }

    private static Color colorFor(ModStatus status) {
        return switch (status) {
            case VERIFIED -> Theme.SUCCESS;
            case CORRUPT -> Theme.DANGER;
            case LOCAL_DEV -> Theme.WARN;
            case NOT_INSTALLED -> Theme.TEXT_DIM;
        };
    }
}
