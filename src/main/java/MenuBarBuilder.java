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
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class MenuBarBuilder {
    public static JMenuBar build(java.awt.Component parent, TaskManager taskManager, Runnable updateTasks, DailyChecklist dailyChecklist) {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        // Search Tasks
        JMenuItem searchItem = new JMenuItem("Search Tasks");
        searchItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        searchItem.addActionListener(e -> SearchDialog.showSearchDialog(parent, taskManager, dailyChecklist));
        fileMenu.add(searchItem);

        // Refresh Tasks
        JMenuItem refreshItem = new JMenuItem("Refresh Tasks");
        refreshItem.addActionListener(e -> updateTasks.run());
        fileMenu.add(refreshItem);

        // Restore from Backup
        JMenuItem restoreItem = new JMenuItem("Restore from Backup");
        restoreItem.addActionListener(e -> {
            BackupRestoreDialog.showRestoreDialog(parent, taskManager, updateTasks);
        });
        fileMenu.add(restoreItem);

        fileMenu.addSeparator();

        // About
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> {
            JDialog aboutDialog = new JDialog((java.awt.Frame) parent, "About Daily Checklist", true);
            aboutDialog.setLayout(new BorderLayout());
            aboutDialog.setSize(600, 800);
            aboutDialog.setLocationRelativeTo(parent);
            aboutDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            // Title panel
            JPanel titlePanel = new JPanel();
            titlePanel.setBackground(new Color(0, 123, 255)); // Bootstrap blue
            JLabel titleLabel = new JLabel("Daily Checklist");
            titleLabel.setFont(FontManager.getTitleFont());
            titleLabel.setForeground(Color.WHITE);
            titlePanel.add(titleLabel);
            aboutDialog.add(titlePanel, BorderLayout.NORTH);

            // Content panel
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            contentPanel.setBackground(Color.WHITE);
            String gpl3Text = Gpl3TextProvider.getGpl3Text();

            String htmlContent = "<html><body style='font-family: " + FontManager.FONT_NAME + "; font-size: 12pt; color: #333;'>" +
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
                "<h3>Full GNU General Public License v3</h3>" +
                "<pre style='font-size: 12pt; white-space: pre-wrap;'>" + gpl3Text + "</pre>" +
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
            closeButton.setFont(FontManager.getButtonFont());
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

        // Add Help menu item
        JMenuItem helpItem = new JMenuItem("Help");
        helpItem.addActionListener(e -> {
            JDialog helpDialog = new JDialog((java.awt.Frame) parent, "Help - Daily Checklist", true);
            helpDialog.setLayout(new BorderLayout());
            helpDialog.setSize(600, 500);
            helpDialog.setLocationRelativeTo(parent);
            helpDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            // Title panel
            JPanel titlePanel = new JPanel();
            titlePanel.setBackground(new Color(0, 123, 255)); // Bootstrap blue
            JLabel titleLabel = new JLabel("Daily Checklist Help");
            titleLabel.setFont(FontManager.getHeader1Font());
            titleLabel.setForeground(Color.WHITE);
            titlePanel.add(titleLabel);
            helpDialog.add(titlePanel, BorderLayout.NORTH);

            // Content panel
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            contentPanel.setBackground(Color.WHITE);

            // Read help text from file
            String helpText = "";
            try (java.io.InputStream is = MenuBarBuilder.class.getResourceAsStream("/help.txt");
                 java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8")) {
                helpText = scanner.useDelimiter("\\A").next();
            } catch (Exception ex) {
                helpText = "Help file not found. Please refer to the README.md file for detailed instructions.";
            }

            JTextPane contentPane = new JTextPane();
            contentPane.setEditable(false);
            contentPane.setBackground(Color.WHITE);
            contentPane.setBorder(BorderFactory.createEmptyBorder());

            // Set up the styled document
            StyledDocument doc = contentPane.getStyledDocument();

            // Create styles
            Style defaultStyle = doc.addStyle("default", null);
            StyleConstants.setFontFamily(defaultStyle, FontManager.FONT_NAME);
            StyleConstants.setFontSize(defaultStyle, 12);

            Style header1Style = doc.addStyle("h1", defaultStyle);
            StyleConstants.setFontSize(header1Style, 24);
            StyleConstants.setBold(header1Style, true);

            Style header2Style = doc.addStyle("h2", defaultStyle);
            StyleConstants.setFontSize(header2Style, 18);
            StyleConstants.setBold(header2Style, true);

            Style header3Style = doc.addStyle("h3", defaultStyle);
            StyleConstants.setFontSize(header3Style, 14);
            StyleConstants.setBold(header3Style, true);

            Style boldStyle = doc.addStyle("bold", defaultStyle);
            StyleConstants.setBold(boldStyle, true);

            // Parse and insert HTML-like content with icons
            HelpTextRenderer.insertStyledTextWithIcons(doc, helpText, defaultStyle, boldStyle);

            JScrollPane scrollPane = new JScrollPane(contentPane);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            helpDialog.add(contentPanel, BorderLayout.CENTER);

            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            buttonPanel.setBackground(Color.WHITE);
            JButton closeButton = new JButton("Close");
            closeButton.setFont(FontManager.getButtonFont());
            closeButton.setBackground(Color.LIGHT_GRAY);
            closeButton.setForeground(Color.BLACK);
            closeButton.setFocusPainted(false);
            closeButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            closeButton.addActionListener(ae -> helpDialog.dispose());
            buttonPanel.add(closeButton);
            helpDialog.add(buttonPanel, BorderLayout.SOUTH);

            helpDialog.setVisible(true);
        });
        fileMenu.add(helpItem);

        fileMenu.addSeparator();

        // Ensure the File menu is added to the menu bar
        menuBar.add(fileMenu);

        return menuBar;
    }

}