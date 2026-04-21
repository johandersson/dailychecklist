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
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import javax.swing.BoxLayout;
import java.awt.Window;
import java.awt.KeyboardFocusManager;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JScrollPane;
import javax.swing.JComponent;
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
    // Recurrence UI helpers
    private JPanel recurrencePanel;
    private final String[] weekdayAbbr = {"Mo","Tu","We","Th","Fr","Sa","Su"};
    private final Color[] weekdayCols = { new Color(165,42,42), new Color(0,90,156), new Color(139,128,0), new Color(34,139,34), new Color(139,69,19), new Color(255,69,0), new Color(199,21,133) };
    private int weekdayIconSize = 36;
    private int recurrenceDaysMask = 0;

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
        // For recurring reminders year/month/day are 0; don't show an invalid date in the dialog
        String dateString = reminder.isRecurring() ? null : String.format("%04d-%02d-%02d", reminder.getYear(), reminder.getMonth(), reminder.getDay());

        JPanel topPanel = buildTopPanel(checklistName, breadcrumbText, timeString, dateString, reminder);
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
        // Ensure dialog minimum size to avoid clipping day icons
        int minW = Math.max(520, getWidth());
        int minH = Math.max(260, getHeight());
        setMinimumSize(new Dimension(minW, minH));
        centerOverParent(parent);
    }

    private void initDialogSettings(JFrame parent) {
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(true);
        // Ensure the dialog repaints and regains proper stacking when moved across screens
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentMoved(java.awt.event.ComponentEvent e) {
                // schedule repaint on EDT to avoid flicker across monitors / DPI changes
                javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            // Invalidate cached weekday icons when moving across displays
                            try { IconCache.invalidateWeekdayIcons(); } catch (Throwable ignore) {}
                            // Refresh icons inside the recurrence panel so labels get new icons for the current GC/DPI
                            refreshRecurrenceIcons();
                            revalidate();
                            repaint();
                            // Try to nudge window manager to refresh stacking
                            toFront();
                        } catch (Exception ignore) {}
                });
            }
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                javax.swing.SwingUtilities.invokeLater(() -> { revalidate(); repaint(); });
            }
        });
    }

    private JPanel buildTopPanel(String checklistName, String breadcrumbText, String timeString, String dateString, Reminder reminder) {
        JPanel topPanel = new JPanel(new BorderLayout());
        String titleHtml = formatTitleHtml(checklistName, timeString, dateString);
        JLabel titleLabel = new JLabel(titleHtml);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        JPanel southWrap = new JPanel();
        southWrap.setOpaque(false);
        southWrap.setLayout(new BoxLayout(southWrap, BoxLayout.Y_AXIS));
        if (breadcrumbText != null && !breadcrumbText.trim().isEmpty()) {
            SubtaskBreadcrumb crumb = new SubtaskBreadcrumb();
            crumb.setFontToUse(FontManager.getTaskListFont().deriveFont(Font.PLAIN, FontManager.SIZE_SMALL));
            crumb.setText(breadcrumbText);
            crumb.setPreferredSize(new java.awt.Dimension(400, 20));
            JPanel crumbWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            crumbWrap.setOpaque(false);
            crumbWrap.add(crumb);
            southWrap.add(crumbWrap);
        }
        // Add recurrence badges if this is a recurring reminder
        if (reminder != null && reminder.isRecurring()) {
            recurrenceDaysMask = reminder.getDaysBitmask();
            JComponent rec = buildRecurrencePanel(reminder);
            southWrap.add(rec);
        }
        if (southWrap.getComponentCount() > 0) {
            topPanel.add(southWrap, BorderLayout.SOUTH);
        }
        return topPanel;
    }

    private void centerOverParent(JFrame parent) {
        int w = Math.max(getWidth(), 640);
        int h = Math.max(getHeight(), 320);
        if (parent == null || !parent.isShowing()) {
            setSize(w, h);
            setLocationRelativeTo(null);
            return;
        }
        try {
            Point ownerOnScreen = parent.getLocationOnScreen();
            int ownerCenterX = ownerOnScreen.x + parent.getWidth() / 2;
            int ownerCenterY = ownerOnScreen.y + parent.getHeight() / 2;

            java.awt.Rectangle deviceBounds = null;
            java.awt.GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            for (java.awt.GraphicsDevice d : devices) {
                java.awt.Rectangle b = d.getDefaultConfiguration().getBounds();
                if (b.contains(ownerCenterX, ownerCenterY)) {
                    deviceBounds = b;
                    break;
                }
            }
            if (deviceBounds == null) {
                deviceBounds = parent.getGraphicsConfiguration().getBounds();
            }

            int x = deviceBounds.x + (deviceBounds.width - w) / 2;
            int y = deviceBounds.y + (deviceBounds.height - h) / 2;
            // nudge inside bounds
            if (x < deviceBounds.x) x = deviceBounds.x + 8;
            if (y < deviceBounds.y) y = deviceBounds.y + 8;
            if (x + w > deviceBounds.x + deviceBounds.width) x = deviceBounds.x + deviceBounds.width - w - 8;
            if (y + h > deviceBounds.y + deviceBounds.height) y = deviceBounds.y + deviceBounds.height - h - 8;

            setSize(w, h);
            setLocation(x, y);
        } catch (Exception ex) {
            // Fallback
            setSize(w, h);
            setLocationRelativeTo(parent);
        }
    }

    private JComponent buildRecurrencePanel(Reminder reminder) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        p.setOpaque(false);
        for (int i = 0; i < 7; i++) {
            boolean selected = (reminder.getDaysBitmask() & (1 << i)) != 0;
            javax.swing.Icon icon = IconCache.getWeekdayIcon(weekdayAbbr[i], weekdayCols[i], selected, weekdayIconSize);
            JLabel lbl = new JLabel(icon);
            lbl.setToolTipText(weekdayAbbr[i]);
            lbl.putClientProperty("weekdayIndex", Integer.valueOf(i));
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            p.add(lbl);
        }
        // Wrap in a scroll pane so small windows won't chop icons
        JScrollPane scroller = new JScrollPane(p, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        // Save reference for refresh
        this.recurrencePanel = p;
        return scroller;
    }

    private void refreshRecurrenceIcons() {
        if (recurrencePanel == null) return;
        Component[] comps = recurrencePanel.getComponents();
        for (Component c : comps) {
            if (c instanceof JLabel) {
                JLabel lbl = (JLabel) c;
                Object o = lbl.getClientProperty("weekdayIndex");
                if (o instanceof Integer) {
                    int i = ((Integer) o).intValue();
                    boolean selected = (recurrenceDaysMask & (1 << i)) != 0;
                    try {
                        // attempt to read current reminder selection via tooltip/compare; fallback to existing icon state
                        String tip = lbl.getToolTipText();
                        // If tooltip matches, we can decide selection by comparing icon description is not available; skip
                    } catch (Exception ignore) {}
                    javax.swing.Icon icon = IconCache.getWeekdayIcon(weekdayAbbr[i], weekdayCols[i], selected, weekdayIconSize);
                    lbl.setIcon(icon);
                }
            }
        }
    }

    private String formatTitleHtml(String checklistName, String timeString, String dateString) {
        String datePart = (dateString == null || dateString.trim().isEmpty()) ? "" : " on " + dateString;
        return "<html><div style='text-align:center;padding:6px;'><h2 style='color: #2E86AB;margin:0 0 4px 0;font-size:16px;'>⏰ Reminder</h2>" +
                "<div style='font-size:14px;font-weight:bold;color:#333;margin-bottom:4px;'>" + checklistName + "</div>" +
                "<div style='color:#666;font-size:11px;'>Scheduled for: " + timeString + datePart + "</div></div></html>";
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
        
        // Create note display area with nice formatting - visible by default
        javax.swing.JTextArea noteArea = new javax.swing.JTextArea(task.getNote());
        noteArea.setEditable(false);
        noteArea.setFont(FontManager.getTaskListFont());
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setBackground(new java.awt.Color(245, 245, 245)); // Light gray background
        noteArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(noteArea);
        scroll.setPreferredSize(new java.awt.Dimension(400, 120));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);
        
        // Button to toggle note visibility (starts as visible)
        JButton toggleButton = new JButton("Hide note");
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
        panel.add(buttonWrap, BorderLayout.SOUTH);
        
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