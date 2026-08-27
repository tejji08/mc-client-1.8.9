package dev.mcclient.launcher.gui;

import dev.mcclient.launcher.LauncherPaths;
import dev.mcclient.launcher.LauncherSettings;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;

/** RAM, window size, and where everything lives on disk. */
final class SettingsPanel extends JPanel {

    private final LauncherSettings settings;
    private final JLabel saved = Theme.label(" ", 12, Font.PLAIN, Theme.TEXT_DIM);

    SettingsPanel(LauncherSettings settings) {
        this.settings = settings;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.BG);
        setBorder(Theme.pad(26, 28, 24, 28));

        add(left(Theme.label("Settings", 28, Font.BOLD, Theme.TEXT)));
        add(Box.createVerticalStrut(20));
        add(memoryCard());
        add(Box.createVerticalStrut(12));
        add(windowCard());
        add(Box.createVerticalStrut(12));
        add(storageCard());
        add(Box.createVerticalStrut(14));
        add(left(saved));
        add(Box.createVerticalGlue());
    }

    private JComponent memoryCard() {
        JPanel card = card("Memory", "How much heap the game gets. 2 GB is plenty for 1.8.9 with this bundle.");

        JLabel value = Theme.label(settings.memoryMb() + " MB", 14, Font.BOLD, Theme.ACCENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider slider = new JSlider(1024, 12288, settings.memoryMb());
        slider.setSnapToTicks(true);
        slider.setMajorTickSpacing(1024);
        slider.setOpaque(false);
        slider.setForeground(Theme.ACCENT);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        slider.addChangeListener(e -> {
            value.setText(slider.getValue() + " MB");
            if (!slider.getValueIsAdjusting()) {
                settings.setMemoryMb(slider.getValue());
                persist();
            }
        });

        card.add(Box.createVerticalStrut(10));
        card.add(value);
        card.add(slider);
        return card;
    }

    private JComponent windowCard() {
        JPanel card = card("Window size", "Initial resolution the game opens at.");

        JSpinner width = spinner(settings.width(), 320, 7680);
        JSpinner height = spinner(settings.height(), 240, 4320);
        width.addChangeListener(e -> {
            settings.setWidth((Integer) width.getValue());
            persist();
        });
        height.addChangeListener(e -> {
            settings.setHeight((Integer) height.getValue());
            persist();
        });

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(width);
        row.add(Box.createHorizontalStrut(8));
        row.add(Theme.label("x", 13, Font.PLAIN, Theme.TEXT_DIM));
        row.add(Box.createHorizontalStrut(8));
        row.add(height);
        row.add(Box.createHorizontalGlue());

        card.add(Box.createVerticalStrut(10));
        card.add(row);
        return card;
    }

    private JComponent storageCard() {
        JPanel card = card("Storage", LauncherPaths.root().toString());

        JButton open = Theme.button("Open folder", Theme.SURFACE_ALT, Theme.BORDER, Theme.TEXT, 12);
        open.setAlignmentX(Component.LEFT_ALIGNMENT);
        open.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(LauncherPaths.root().toFile());
            } catch (IOException | UnsupportedOperationException ex) {
                saved.setText("Could not open the folder: " + ex.getMessage());
            }
        });

        card.add(Box.createVerticalStrut(10));
        card.add(open);
        return card;
    }

    private JPanel card(String title, String subtitle) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.SURFACE);
        card.setBorder(Theme.card());
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JLabel heading = Theme.label(title, 15, Font.BOLD, Theme.TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel note = Theme.label(subtitle, 12, Font.PLAIN, Theme.TEXT_DIM);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(3));
        card.add(note);
        return card;
    }

    private static JSpinner spinner(int value, int min, int max) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 10));
        spinner.setMaximumSize(new Dimension(110, 30));
        spinner.setPreferredSize(new Dimension(110, 30));
        spinner.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        // Spinners keep the platform look otherwise, which lands as a white box in a dark window.
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            JTextField field = editor.getTextField();
            field.setBackground(Theme.SURFACE_ALT);
            field.setForeground(Theme.TEXT);
            field.setCaretColor(Theme.TEXT);
            field.setBorder(Theme.pad(4, 6, 4, 6));
        }
        return spinner;
    }

    private JComponent left(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    private void persist() {
        try {
            settings.save();
            saved.setText("Saved.");
        } catch (IOException e) {
            saved.setText("Could not save settings: " + e.getMessage());
        }
    }
}
