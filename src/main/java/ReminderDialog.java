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
import java.awt.Font;
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
@SuppressWarnings("serial")
public class ReminderDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private transient final Runnable onOpen;
    private transient final Runnable onDone;
    private transient final Runnable onRemindLater;
    private transient final Runnable onRemindTomorrow;
    private transient final Runnable onMarkAsDone;

    @SuppressWarnings("this-escape")
    public ReminderDialog(JFrame parent, Reminder reminder, Runnable onOpen, Runnable onDone, Runnable onRemindLater, Runnable onRemindTomorrow, Runnable onMarkAsDone) {
        this(parent, reminder, null, null, onOpen, onDone, onRemindLater, onRemindTomorrow, onMarkAsDone);
    }

    @SuppressWarnings("this-escape")
    public ReminderDialog(JFrame parent, Reminder reminder, String displayTitle, String breadcrumbText, Runnable onOpen, Runnable onDone, Runnable onRemindLater, Runnable onRemindTomorrow, Runnable onMarkAsDone) {
        super(parent, "Reminder", true);
        this.onOpen = onOpen;
        this.onDone = onDone;
        this.onRemindLater = onRemindLater;
        this.onRemindTomorrow = onRemindTomorrow;
        this.onMarkAsDone = onMarkAsDone;

        setAlwaysOnTop(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        String checklistName = displayTitle != null ? displayTitle : reminder.getChecklistName();
        if (checklistName == null || checklistName.trim().isEmpty()) {
            checklistName = "Unknown Checklist";
        }

        // Format the reminder time nicely
        String timeString = String.format("%02d:%02d", reminder.getHour(), reminder.getMinute());
        String dateString = String.format("%04d-%02d-%02d", reminder.getYear(), reminder.getMonth(), reminder.getDay());

        // Top panel: title + checklist/task name + optional breadcrumb for subtasks
        JPanel topPanel = new JPanel(new BorderLayout());
        String titleHtml = "<html><div style='text-align:center;padding:6px;'><h2 style='color: #2E86AB;margin:0 0 4px 0;font-size:16px;'>⏰ Reminder</h2>" +
                "<div style='font-size:14px;font-weight:bold;color:#333;margin-bottom:4px;'>" + checklistName + "</div>" +
                "<div style='color:#666;font-size:11px;'>Scheduled for: " + timeString + " on " + dateString + "</div></div></html>";
        JLabel titleLabel = new JLabel(titleHtml);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        if (breadcrumbText != null && !breadcrumbText.trim().isEmpty()) {
            SubtaskBreadcrumb crumb = new SubtaskBreadcrumb();
            crumb.setFontToUse(FontManager.getTaskListFont().deriveFont(Font.PLAIN, FontManager.SIZE_SMALL));
            crumb.setText(breadcrumbText);
            crumb.setPreferredSize(new java.awt.Dimension(400, 20));
            JPanel crumbWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            crumbWrap.add(crumb);
            topPanel.add(crumbWrap, BorderLayout.SOUTH);
        }

        JLabel messageLabel = new JLabel("<html><div style='text-align:center;color:#555;font-style:italic;padding:8px;'>It's time to check your tasks!</div></html>");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        JButton openButton = new JButton("Open Checklist");
        openButton.setToolTipText("Open the checklist in the application");
        JButton dismissButton = new JButton("Remove Reminders");
        dismissButton.setToolTipText("Remove all reminders for this checklist");
        boolean isTaskLevel = reminder.getTaskId() != null;
        JButton markAsDoneButton = new JButton(isTaskLevel ? "Mark task as Done" : "Mark as Done");
        markAsDoneButton.setToolTipText(isTaskLevel ? "Mark the targeted task as done" : "Mark all tasks in checklist as done and open the checklist");
        JButton remindLaterButton = new JButton("Remind me in 15 minutes");
        remindLaterButton.setToolTipText("Remind me again in 15 minutes");
        JButton remindTomorrowButton = new JButton("Remind me tomorrow");
        remindTomorrowButton.setToolTipText("Remind me tomorrow at the same time");

        openButton.addActionListener(e -> {
            if (this.onOpen != null) this.onOpen.run();
            dispose();
        });

        dismissButton.addActionListener(e -> {
            if (this.onDone != null) this.onDone.run();
            dispose();
        });

        markAsDoneButton.addActionListener(e -> {
            if (this.onMarkAsDone != null) this.onMarkAsDone.run();
            dispose();
        });

        remindLaterButton.addActionListener(e -> {
            if (this.onRemindLater != null) this.onRemindLater.run();
            dispose();
        });

        remindTomorrowButton.addActionListener(e -> {
            if (this.onRemindTomorrow != null) this.onRemindTomorrow.run();
            dispose();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(openButton);
        // Only show mark-as-done button for task-level reminders; for checklist-level reminders
        // we intentionally omit this button to avoid ambiguity as requested.
        if (isTaskLevel) {
            buttonPanel.add(markAsDoneButton);
        }
        buttonPanel.add(remindLaterButton);
        buttonPanel.add(remindTomorrowButton);
        buttonPanel.add(dismissButton);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(messageLabel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }
}