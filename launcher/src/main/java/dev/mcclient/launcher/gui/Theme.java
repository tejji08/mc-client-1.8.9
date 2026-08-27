package dev.mcclient.launcher.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * One place for the launcher's look. Swing rather than JavaFX deliberately -- Swing ships with the
 * JDK, so the launcher keeps its zero-runtime-dependency property.
 */
public final class Theme {

    private Theme() {}

    public static final Color BG = new Color(0x14161B);
    public static final Color SURFACE = new Color(0x1C1F26);
    public static final Color SURFACE_ALT = new Color(0x23272F);
    public static final Color BORDER = new Color(0x2E333D);
    public static final Color TEXT = new Color(0xE6E9EF);
    public static final Color TEXT_DIM = new Color(0x8B93A3);
    public static final Color ACCENT = new Color(0x3B82F6);
    public static final Color ACCENT_HOVER = new Color(0x5C9BFF);
    public static final Color SUCCESS = new Color(0x3FB950);
    public static final Color WARN = new Color(0xD29922);
    public static final Color DANGER = new Color(0xF85149);

    private static final String FAMILY = pickFamily();

    private static String pickFamily() {
        // Segoe UI on Windows, otherwise let the platform pick something sane.
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return "Segoe UI";
        }
        if (os.contains("mac")) {
            return "SF Pro Text";
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int size, int style) {
        return new Font(FAMILY, style, size);
    }

    public static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, style));
        label.setForeground(color);
        return label;
    }

    public static Border pad(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    /** Flat filled button with rounded corners and a hover state. */
    public static JButton button(String text, Color fill, Color hover, Color textColor, int fontSize) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color background = getModel().isRollover() ? hover : fill;
                if (!isEnabled()) {
                    background = SURFACE_ALT;
                }
                g2.setColor(background);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(font(fontSize, Font.BOLD));
        button.setForeground(textColor);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(pad(10, 18, 10, 18));
        return button;
    }

    /** Small coloured status pill, used for mod verification state. */
    public static JComponent badge(String text, Color color) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 38));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 110));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setFont(font(11, Font.BOLD));
        label.setForeground(color);
        label.setBorder(pad(3, 9, 3, 9));
        label.setOpaque(false);
        Dimension size = label.getPreferredSize();
        label.setMaximumSize(new Dimension(size.width, size.height));
        return label;
    }

    /** Card surface with a hairline border. */
    public static Border card() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                pad(14, 16, 14, 16));
    }

    /** Makes a component respond to clicks without looking like a button. */
    public static void onClick(JComponent component, Runnable action) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
    }
}
