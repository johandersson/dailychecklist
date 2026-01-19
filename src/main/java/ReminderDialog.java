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
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * A dialog for displaying reminders with HTML formatting and user-friendly design.
 */
public class ReminderDialog extends JDialog {
    private final Runnable onOpen;
    private final Runnable onDone;

    public ReminderDialog(JFrame parent, Reminder reminder, Runnable onOpen, Runnable onDone) {
        super(parent, "⏰ Reminder", true);
        this.onOpen = onOpen;
        this.onDone = onDone;

        setAlwaysOnTop(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        String checklistName = reminder.getChecklistName();
        if (checklistName == null || checklistName.trim().isEmpty()) {
            checklistName = "Unknown Checklist";
        }

        // Format the reminder time nicely
        String timeString = String.format("%02d:%02d", reminder.getHour(), reminder.getMinute());
        String dateString = String.format("%d/%d/%d", reminder.getMonth(), reminder.getDay(), reminder.getYear());

        // Create HTML-formatted message with better styling
        String htmlMessage = "<html><body style='font-family: Arial, sans-serif; font-size: 12px; padding: 10px;'>" +
            "<div style='text-align: center; margin-bottom: 15px;'>" +
            "<h2 style='color: #2E86AB; margin: 0 0 5px 0; font-size: 16px;'>⏰ Reminder</h2>" +
            "<div style='font-size: 14px; font-weight: bold; color: #333; margin-bottom: 8px;'>" + checklistName + "</div>" +
            "<div style='color: #666; font-size: 11px;'>Scheduled for: " + timeString + " on " + dateString + "</div>" +
            "</div>" +
            "<div style='text-align: center; color: #555; font-style: italic;'>" +
            "It's time to check your tasks!" +
            "</div>" +
            "</body></html>";

        JLabel messageLabel = new JLabel(htmlMessage);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JButton openButton = new JButton("📂 Open Checklist");
        openButton.setToolTipText("Open the checklist in the application");
        JButton doneButton = new JButton("✅ Done");
        doneButton.setToolTipText("Mark this reminder as completed");

        openButton.addActionListener(e -> {
            onOpen.run();
            dispose();
        });

        doneButton.addActionListener(e -> {
            onDone.run();
            dispose();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.add(openButton);
        buttonPanel.add(doneButton);

        setLayout(new BorderLayout());
        add(messageLabel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }
}