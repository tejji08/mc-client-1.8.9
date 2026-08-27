package dev.mcclient.launcher.gui;

import dev.mcclient.launcher.auth.SignInPrompt;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;

/**
 * Shows the Microsoft device code. Sign-in happens in the user's own browser against
 * login.microsoftonline.com -- the launcher never sees the password, and there is no embedded
 * webview to quietly harvest one.
 */
final class SignInDialog implements SignInPrompt {

    private final Frame owner;
    private JDialog dialog;

    SignInDialog(Frame owner) {
        this.owner = owner;
    }

    @Override
    public void show(String verificationUri, String userCode) {
        SwingUtilities.invokeLater(() -> build(verificationUri, userCode));
    }

    @Override
    public void dismiss() {
        SwingUtilities.invokeLater(() -> {
            if (dialog != null) {
                dialog.dispose();
                dialog = null;
            }
        });
    }

    private void build(String verificationUri, String userCode) {
        dialog = new JDialog(owner, "Sign in with Microsoft", false);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.SURFACE);
        content.setBorder(Theme.pad(24, 28, 24, 28));

        add(content, Theme.label("Sign in to Microsoft", 18, Font.BOLD, Theme.TEXT));
        content.add(Box.createVerticalStrut(6));
        add(content, Theme.label("Open the page below and enter this code.", 13, Font.PLAIN, Theme.TEXT_DIM));
        content.add(Box.createVerticalStrut(18));

        JLabel code = Theme.label(userCode, 30, Font.BOLD, Theme.ACCENT);
        code.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));
        code.setForeground(Theme.ACCENT);
        add(content, code);
        content.add(Box.createVerticalStrut(4));
        add(content, Theme.label(verificationUri, 13, Font.PLAIN, Theme.TEXT));
        content.add(Box.createVerticalStrut(20));

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        var copy = Theme.button("Copy code", Theme.SURFACE_ALT, Theme.BORDER, Theme.TEXT, 13);
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(userCode), null));
        var open = Theme.button("Open page", Theme.ACCENT, Theme.ACCENT_HOVER, Theme.TEXT, 13);
        open.addActionListener(e -> openBrowser(verificationUri));

        buttons.add(copy);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(open);
        content.add(buttons);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.SURFACE);
        root.add(content, BorderLayout.CENTER);
        root.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static void add(JPanel panel, JLabel label) {
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }

    private void openBrowser(String uri) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(uri));
            }
        } catch (Exception ignored) {
            // The URL is on screen either way -- failing to auto-open is not worth an error dialog.
        }
    }
}
