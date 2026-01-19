/*
 * Daily Checklist
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class MenuBarBuilder {
    public static JMenuBar build(java.awt.Component parent, TaskManager taskManager, Runnable updateTasks) {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            if (taskManager.hasUndoneTasks()) {
                int response = JOptionPane.showConfirmDialog(parent,
                        "There are undone tasks. Are you sure you want to exit?",
                        "Confirm Exit",
                        JOptionPane.YES_NO_OPTION);
                if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            System.exit(0);
        });
        fileMenu.add(exitItem);

        //add about menu item
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> {
            JDialog aboutDialog = new JDialog((java.awt.Frame) parent, "About Daily Checklist", true);
            aboutDialog.setLayout(new BorderLayout());
            aboutDialog.setSize(600, 450);
            aboutDialog.setLocationRelativeTo(parent);
            aboutDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            // Title panel
            JPanel titlePanel = new JPanel();
            titlePanel.setBackground(new Color(0, 123, 255)); // Bootstrap blue
            JLabel titleLabel = new JLabel("Daily Checklist");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
            titleLabel.setForeground(Color.WHITE);
            titlePanel.add(titleLabel);
            aboutDialog.add(titlePanel, BorderLayout.NORTH);

            // Content panel
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            contentPanel.setBackground(Color.WHITE);
            String htmlContent = "<html><body style='font-family: Arial, sans-serif; font-size: 12pt; color: #333;'>" +
                "<h2>About Daily Checklist</h2>" +
                "<p>Daily Checklist is a simple application designed to help you manage your daily tasks. " +
                "It organizes tasks into morning and evening routines, with support for weekday-specific tasks to keep you on track.</p>" +
                "<h3>Version & Copyright</h3>" +
                "<p><b>Version:</b> 0.1</p>" +
                "<p><b>Copyright:</b> (C) 2025 Johan Andersson</p>" +
                "<p>This program is free software: you can redistribute it and/or modify<br>" +
                "it under the terms of the GNU General Public License as published by<br>" +
                "the Free Software Foundation, either version 3 of the License, or<br>" +
                "(at your option) any later version.</p>" +
                "<p>This program is distributed in the hope that it will be useful,<br>" +
                "but WITHOUT ANY WARRANTY; without even the implied warranty of<br>" +
                "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the<br>" +
                "GNU General Public License for more details.</p>" +
                "<p>You should have received a copy of the GNU General Public License<br>" +
                "along with this program. If not, see <a href='https://www.gnu.org/licenses/'>https://www.gnu.org/licenses/</a>.</p>" +
                "<h3>Credits</h3>" +
                "<p>This application is developed using Java, which is open source software licensed under GPL v2 with Classpath Exception. " +
                "For more information about Java, visit <a href='https://openjdk.java.net/'>https://openjdk.java.net/</a>.</p>" +
                "<p>Testing is performed using JUnit, an open source testing framework licensed under EPL 2.0. " +
                "For more information, visit <a href='https://junit.org/'>https://junit.org/</a>.</p>" +
                "</body></html>";
            JEditorPane contentPane = new JEditorPane("text/html", htmlContent);
            contentPane.setEditable(false);
            contentPane.setBackground(Color.WHITE);
            contentPane.setBorder(BorderFactory.createEmptyBorder());
            contentPane.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (Exception ex) {
                            // Handle exception, perhaps show a message
                            JOptionPane.showMessageDialog(aboutDialog, "Unable to open link: " + e.getURL(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
            JScrollPane scrollPane = new JScrollPane(contentPane);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            aboutDialog.add(contentPanel, BorderLayout.CENTER);

            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            buttonPanel.setBackground(Color.WHITE);
            JButton closeButton = new JButton("Close");
            closeButton.setFont(new Font("Arial", Font.PLAIN, 14));
            closeButton.setBackground(Color.LIGHT_GRAY);
            closeButton.setForeground(Color.BLACK);
            closeButton.setFocusPainted(false);
            closeButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            closeButton.addActionListener(ae -> aboutDialog.dispose());
            buttonPanel.add(closeButton);
            aboutDialog.add(buttonPanel, BorderLayout.SOUTH);

            aboutDialog.setVisible(true);
        });
        fileMenu.add(aboutItem);

        //Add a refresh option that reloads tasks from repository
        JMenuItem refreshItem = new JMenuItem("Refresh Tasks");
        refreshItem.addActionListener(e -> updateTasks.run());
        fileMenu.add(refreshItem);

        menuBar.add(fileMenu);
        return menuBar;
    }
}