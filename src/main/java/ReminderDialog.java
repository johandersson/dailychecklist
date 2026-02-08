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
    private TaskManager taskManager; // For retrieving task notes

    @SuppressWarnings("this-escape")
    public ReminderDialog(JFrame parent, Reminder reminder, Runnable onOpen, Runnable onDone, Runnable onRemindLater, Runnable onRemindTomorrow, Runnable onMarkAsDone) {
        this(parent, reminder, null, null, onOpen, onDone, onRemindLater, onRemindTomorrow, onMarkAsDone, null);
    }

    @SuppressWarnings("this-escape")
    public ReminderDialog(JFrame parent, Reminder reminder, String displayTitle, String breadcrumbText, Runnable onOpen, Runnable onDone, Runnable onRemindLater, Runnable onRemindTomorrow, Runnable onMarkAsDone) {
        this(parent, reminder, displayTitle, breadcrumbText, onOpen, onDone, onRemindLater, onRemindTomorrow, onMarkAsDone, null);
    }

    @SuppressWarnings("this-escape")
    public ReminderDialog(JFrame parent, Reminder reminder, String displayTitle, String breadcrumbText, Runnable onOpen, Runnable onDone, Runnable onRemindLater, Runnable onRemindTomorrow, Runnable onMarkAsDone, TaskManager taskManager) {
        super(parent, "Reminder", true);
        this.onOpen = onOpen;
        this.onDone = onDone;
        this.onRemindLater = onRemindLater;
        this.onRemindTomorrow = onRemindTomorrow;
        this.onMarkAsDone = onMarkAsDone;
        this.taskManager = taskManager;

        initDialogSettings(parent);

        String checklistName = displayTitle != null ? displayTitle : reminder.getChecklistName();
        if (checklistName == null || checklistName.trim().isEmpty()) {
            checklistName = "Unknown Checklist";
        }

        String timeString = String.format("%02d:%02d", reminder.getHour(), reminder.getMinute());
        String dateString = String.format("%04d-%02d-%02d", reminder.getYear(), reminder.getMonth(), reminder.getDay());

        JPanel topPanel = buildTopPanel(checklistName, breadcrumbText, timeString, dateString);
        JLabel messageLabel = buildMessageLabel();
        JPanel notePanel = buildNotePanel(reminder);
        JPanel buttonPanel = buildButtonPanel(reminder);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(messageLabel, BorderLayout.NORTH);
        if (notePanel != null) {
            centerPanel.add(notePanel, BorderLayout.CENTER);
        }
        add(centerPanel, BorderLayout.CENTER);
        
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private void initDialogSettings(JFrame parent) {
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);
    }

    private JPanel buildTopPanel(String checklistName, String breadcrumbText, String timeString, String dateString) {
        JPanel topPanel = new JPanel(new BorderLayout());
        String titleHtml = formatTitleHtml(checklistName, timeString, dateString);
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
        return topPanel;
    }

    private String formatTitleHtml(String checklistName, String timeString, String dateString) {
        return "<html><div style='text-align:center;padding:6px;'><h2 style='color: #2E86AB;margin:0 0 4px 0;font-size:16px;'>⏰ Reminder</h2>" +
                "<div style='font-size:14px;font-weight:bold;color:#333;margin-bottom:4px;'>" + checklistName + "</div>" +
                "<div style='color:#666;font-size:11px;'>Scheduled for: " + timeString + " on " + dateString + "</div></div></html>";
    }

    private JPanel buildNotePanel(Reminder reminder) {
        // If reminder is for a specific task, get the task's note
        if (taskManager == null || reminder.getTaskId() == null) {
            return null;
        }
        
        Task task = taskManager.getTaskById(reminder.getTaskId());
        if (task == null || !task.hasNote()) {
            return null;
        }
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 12, 6, 12));
        
        // Create collapsible note area (similar to ErrorDialog)
        javax.swing.JTextArea noteArea = new javax.swing.JTextArea(task.getNote());
        noteArea.setEditable(false);
        noteArea.setFont(FontManager.getTaskListFont());
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setVisible(false);
        
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(noteArea);
        scroll.setPreferredSize(new java.awt.Dimension(400, 120));
        scroll.setVisible(false);
        panel.add(scroll, BorderLayout.CENTER);
        
        // Button to toggle note visibility
        JButton toggleButton = new JButton("Show note");
        toggleButton.setFont(FontManager.getButtonFont());
        toggleButton.addActionListener(e -> {
            boolean showing = scroll.isVisible();
            scroll.setVisible(!showing);
            noteArea.setVisible(!showing);
            toggleButton.setText(showing ? "Show note" : "Hide note");
            pack();
        });
        
        JPanel buttonWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonWrap.add(toggleButton);
        panel.add(buttonWrap, BorderLayout.NORTH);
        
        return panel;
    }

    private JLabel buildMessageLabel() {
        JLabel messageLabel = new JLabel("<html><div style='text-align:center;color:#555;font-style:italic;padding:8px;'>It's time to check your tasks!</div></html>");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return messageLabel;
    }

    private JPanel buildButtonPanel(Reminder reminder) {
        JButton openButton = new JButton("Open Checklist");
        openButton.setToolTipText("<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>Open the checklist in the application</p></html>");
        JButton dismissButton = new JButton("Remove Reminders");
        dismissButton.setToolTipText("<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>Remove all reminders for this checklist</p></html>");
        boolean isTaskLevel = reminder.getTaskId() != null;
        JButton markAsDoneButton = new JButton(isTaskLevel ? "Mark task as Done" : "Mark as Done");
        markAsDoneButton.setToolTipText(isTaskLevel ? "<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>Mark the targeted task as done</p></html>" : "<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>Mark all tasks in checklist as done and open the checklist</p></html>");
        JButton remindLaterButton = new JButton("Remind me in 15 minutes");
        remindLaterButton.setToolTipText("<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>Remind me again in 15 minutes</p></html>");
        JButton remindTomorrowButton = new JButton("Remind me tomorrow");
        remindTomorrowButton.setToolTipText("<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>Remind me tomorrow at the same time</p></html>");

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
        if (isTaskLevel) {
            buttonPanel.add(markAsDoneButton);
        }
        buttonPanel.add(remindLaterButton);
        buttonPanel.add(remindTomorrowButton);
        buttonPanel.add(dismissButton);
        return buttonPanel;
    }
}