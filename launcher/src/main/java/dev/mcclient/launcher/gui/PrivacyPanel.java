package dev.mcclient.launcher.gui;

import dev.mcclient.launcher.NetworkDisclosure;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * Every host this client can contact, and what it will never do.
 *
 * <p>Any client can claim it respects your privacy in a README. This is the same claim in a form
 * you can check in ten seconds -- and one the commercial clients structurally cannot ship, because
 * their business is the data this page would have to list.
 */
final class PrivacyPanel extends JPanel {

    PrivacyPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(Theme.pad(26, 28, 24, 28));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        content.add(left(Theme.label("Where your data goes", 28, Font.BOLD, Theme.TEXT)));
        content.add(Box.createVerticalStrut(4));
        content.add(left(Theme.label(
                "The complete list of hosts this client is capable of contacting.",
                12, Font.PLAIN, Theme.TEXT_DIM)));
        content.add(Box.createVerticalStrut(18));

        List<NetworkDisclosure.Endpoint> endpoints = NetworkDisclosure.endpoints();
        for (int i = 0; i < endpoints.size(); i++) {
            content.add(endpointCard(endpoints.get(i)));
            content.add(Box.createVerticalStrut(8));
        }

        content.add(Box.createVerticalStrut(10));
        content.add(left(Theme.label("What it never does", 20, Font.BOLD, Theme.TEXT)));
        content.add(Box.createVerticalStrut(8));
        List<String> never = NetworkDisclosure.neverDoes();
        for (int i = 0; i < never.size(); i++) {
            JLabel line = Theme.label("<html>&bull; " + never.get(i) + "</html>", 12, Font.PLAIN, Theme.SUCCESS);
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            content.add(line);
            content.add(Box.createVerticalStrut(5));
        }

        content.add(Box.createVerticalStrut(14));
        JLabel audit = Theme.label(
                "<html>Verify rather than trust: <code>grep -rhoE '\"https?://[^\"]+\"' "
                        + "launcher/src/main mods/*/src/main</code></html>",
                11, Font.PLAIN, Theme.TEXT_DIM);
        audit.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(audit);
        content.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        Theme.styleScroll(scroll);
        add(scroll, BorderLayout.CENTER);
    }

    private JComponent endpointCard(NetworkDisclosure.Endpoint endpoint) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.SURFACE);
        card.setBorder(Theme.card());
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));

        JPanel titleRow = new JPanel();
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.add(Theme.label(endpoint.host(), 14, Font.BOLD, Theme.TEXT));
        titleRow.add(Box.createHorizontalStrut(10));
        titleRow.add(endpoint.sendsIdentity()
                ? Theme.badge("identifies you", Theme.WARN)
                : Theme.badge("anonymous", Theme.SUCCESS));
        titleRow.add(Box.createHorizontalGlue());

        JLabel purpose = Theme.label(endpoint.purpose(), 12, Font.PLAIN, Theme.TEXT_DIM);
        purpose.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel when = Theme.label(endpoint.when(), 11, Font.PLAIN, Theme.BORDER.brighter());
        when.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleRow);
        card.add(Box.createVerticalStrut(4));
        card.add(purpose);
        card.add(Box.createVerticalStrut(3));
        card.add(when);
        return card;
    }

    private JComponent left(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }
}
