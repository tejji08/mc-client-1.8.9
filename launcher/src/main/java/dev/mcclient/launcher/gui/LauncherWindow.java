package dev.mcclient.launcher.gui;

import dev.mcclient.launcher.GameLog;
import dev.mcclient.launcher.LauncherPaths;
import dev.mcclient.launcher.LauncherSettings;
import dev.mcclient.launcher.mods.ModManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The launcher window: sidebar navigation on the left, the selected panel on the right -- the
 * shape every game client has settled on, because it works.
 */
public final class LauncherWindow extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JLabel> navItems = new LinkedHashMap<>();
    private ModsPanel modsPanel;
    private LogsPanel logsPanel;

    /** Builds and shows the window on the event dispatch thread. */
    public static void open() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.put("ToolTip.background", Theme.SURFACE_ALT);
                UIManager.put("ToolTip.foreground", Theme.TEXT);
                new LauncherWindow().setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Launcher failed to start:\n" + e,
                        "mc-client-1.8.9", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private LauncherWindow() throws Exception {
        super("mc-client 1.8.9");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(880, 560));
        setSize(980, 620);
        setLocationRelativeTo(null);

        HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        ModManager mods = new ModManager(http);
        LauncherSettings settings = new LauncherSettings();

        content.setBackground(Theme.BG);
        GameLog log = new GameLog(LauncherPaths.root().resolve("logs").resolve("game-latest.log"), false);
        modsPanel = new ModsPanel(mods);
        logsPanel = new LogsPanel(log);
        content.add(new PlayPanel(this, http, mods, settings, log), "Play");
        content.add(modsPanel, "Mods");
        content.add(new SettingsPanel(settings), "Settings");
        content.add(logsPanel, "Logs");
        content.add(new PrivacyPanel(), "Privacy");

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        select("Play");
    }

    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.SURFACE);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER),
                Theme.pad(22, 18, 18, 18)));
        sidebar.setPreferredSize(new Dimension(196, 0));

        JLabel brand = Theme.label("mc-client", 20, Font.BOLD, Theme.TEXT);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel version = Theme.label("1.8.9  ·  Legacy Fabric", 12, Font.PLAIN, Theme.TEXT_DIM);
        version.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(brand);
        sidebar.add(version);
        sidebar.add(Box.createVerticalStrut(26));

        for (String name : new String[] {"Play", "Mods", "Settings", "Logs", "Privacy"}) {
            JLabel item = navItem(name);
            navItems.put(name, item);
            sidebar.add(item);
            sidebar.add(Box.createVerticalStrut(4));
        }

        sidebar.add(Box.createVerticalGlue());
        JLabel privacy = Theme.label("<html>No telemetry.<br>No phone-home.</html>", 11, Font.PLAIN, Theme.TEXT_DIM);
        privacy.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(privacy);
        return sidebar;
    }

    private JLabel navItem(String name) {
        JLabel item = Theme.label(name, 14, Font.BOLD, Theme.TEXT_DIM);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setBorder(Theme.pad(9, 12, 9, 12));
        item.setOpaque(true);
        item.setBackground(Theme.SURFACE);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        Theme.onClick(item, () -> select(name));
        return item;
    }

    private void select(String name) {
        navItems.forEach((key, label) -> {
            boolean active = key.equals(name);
            label.setForeground(active ? Theme.TEXT : Theme.TEXT_DIM);
            label.setBackground(active ? Theme.SURFACE_ALT : Theme.SURFACE);
        });
        cards.show(content, name);
        if (name.equals("Mods")) {
            // Re-verify on entry: "was fine last week" is not the claim the badge is making.
            modsPanel.refresh();
        } else if (name.equals("Logs")) {
            logsPanel.refresh();
        }
    }
}
