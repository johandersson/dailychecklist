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
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/**
 * Panel showing a timeline view of today's reminders, similar to a calendar day view.
 */
public class TodayPanel extends JPanel {
    private final TaskManager taskManager;
    private final int startHour = 6; // Start timeline at 6 AM
    private final int endHour = 22;  // End timeline at 10 PM
    private final int hoursToShow = endHour - startHour;
    private final int hourHeight = 60; // Height of each hour block
    private final int timelineWidth = 80; // Width of the time column
    private final int reminderBlockHeight = 40; // Height of reminder blocks
    private final int reminderBlockWidth = 200; // Width of reminder blocks

    private LocalDate today;
    private List<Reminder> todaysReminders;
    private Map<String, Task> taskCache;

    public TodayPanel(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.today = LocalDate.now();
        this.todaysReminders = new ArrayList<>();
        this.taskCache = new HashMap<>();

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add component listener to handle resizing
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refreshData();
                repaint();
            }
        });

        refreshData();
    }

    /**
     * Refresh the data for today's reminders.
     */
    public void refreshData() {
        this.today = LocalDate.now();
        this.todaysReminders = getTodaysReminders();
        this.taskCache.clear();

        // Cache tasks for reminders
        for (Reminder reminder : todaysReminders) {
            if (reminder.getTaskId() != null) {
                Task task = taskManager.getTaskById(reminder.getTaskId());
                if (task != null) {
                    taskCache.put(reminder.getTaskId(), task);
                }
            }
        }

        repaint();
    }

    /**
     * Get all reminders for today.
     */
    private List<Reminder> getTodaysReminders() {
        List<Reminder> allReminders = taskManager.getReminders();
        List<Reminder> todays = new ArrayList<>();

        for (Reminder reminder : allReminders) {
            if (reminder.getYear() == today.getYear() &&
                reminder.getMonth() == today.getMonthValue() &&
                reminder.getDay() == today.getDayOfMonth()) {
                todays.add(reminder);
            }
        }

        return todays;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Draw timeline background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Draw hour lines and time labels
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(g2d.getFont().deriveFont(12f));

        for (int hour = startHour; hour <= endHour; hour++) {
            int y = (hour - startHour) * hourHeight;

            // Draw horizontal line
            g2d.drawLine(timelineWidth, y, width, y);

            // Draw time label
            String timeLabel = String.format("%02d:00", hour);
            FontMetrics fm = g2d.getFontMetrics();
            int labelY = y + fm.getAscent() + 5;
            g2d.drawString(timeLabel, 10, labelY);
        }

        // Draw reminder blocks
        drawReminderBlocks(g2d, width);

        g2d.dispose();
    }

    /**
     * Draw reminder blocks on the timeline.
     */
    private void drawReminderBlocks(Graphics2D g2d, int width) {
        // Group reminders by time to handle overlaps
        Map<String, List<Reminder>> remindersByTime = new HashMap<>();

        for (Reminder reminder : todaysReminders) {
            String timeKey = reminder.getHour() + ":" + reminder.getMinute();
            remindersByTime.computeIfAbsent(timeKey, k -> new ArrayList<>()).add(reminder);
        }

        // Draw each group of reminders
        for (Map.Entry<String, List<Reminder>> entry : remindersByTime.entrySet()) {
            List<Reminder> timeReminders = entry.getValue();
            int hour = Integer.parseInt(entry.getKey().split(":")[0]);
            int minute = Integer.parseInt(entry.getKey().split(":")[1]);

            int y = (hour - startHour) * hourHeight + (minute * hourHeight / 60);

            for (int i = 0; i < timeReminders.size(); i++) {
                Reminder reminder = timeReminders.get(i);
                int x = timelineWidth + 10 + (i * (reminderBlockWidth + 10));

                drawReminderBlock(g2d, reminder, x, y);
            }
        }
    }

    /**
     * Draw a single reminder block.
     */
    private void drawReminderBlock(Graphics2D g2d, Reminder reminder, int x, int y) {
        // Draw reminder block background
        g2d.setColor(new Color(70, 130, 180)); // Steel blue
        g2d.fillRoundRect(x, y, reminderBlockWidth, reminderBlockHeight, 8, 8);

        // Draw border
        g2d.setColor(new Color(50, 100, 150));
        g2d.drawRoundRect(x, y, reminderBlockWidth, reminderBlockHeight, 8, 8);

        // Draw text
        g2d.setColor(Color.WHITE);
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 11f));

        String title = getReminderTitle(reminder);
        String subtitle = getReminderSubtitle(reminder);

        FontMetrics fm = g2d.getFontMetrics();
        int textY = y + fm.getAscent() + 5;

        // Draw title
        if (title.length() > 20) {
            title = title.substring(0, 17) + "...";
        }
        g2d.drawString(title, x + 8, textY);

        // Draw subtitle if present
        if (subtitle != null && !subtitle.isEmpty()) {
            g2d.setFont(g2d.getFont().deriveFont(Font.PLAIN, 9f));
            textY += fm.getHeight() + 2;
            if (subtitle.length() > 25) {
                subtitle = subtitle.substring(0, 22) + "...";
            }
            g2d.drawString(subtitle, x + 8, textY);
        }
    }

    /**
     * Get the title for a reminder block.
     */
    private String getReminderTitle(Reminder reminder) {
        if (reminder.getTaskId() != null) {
            Task task = taskCache.get(reminder.getTaskId());
            if (task != null) {
                return task.getName();
            }
        }
        return reminder.getChecklistName();
    }

    /**
     * Get the subtitle (breadcrumb) for a reminder block.
     */
    private String getReminderSubtitle(Reminder reminder) {
        if (reminder.getTaskId() != null) {
            Task task = taskCache.get(reminder.getTaskId());
            if (task != null && task.getParentId() != null) {
                // This is a subtask, show breadcrumb
                Task parent = taskManager.getTaskById(task.getParentId());
                if (parent != null) {
                    return "↳ " + parent.getName();
                }
            }
        }
        return null;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, hoursToShow * hourHeight + 50);
    }
}