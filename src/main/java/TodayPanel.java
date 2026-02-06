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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private final int startHour = 0; // Start timeline at midnight
    private final int endHour = 23;  // End timeline at 11 PM (last hour line)
    private final int hoursToShow = endHour - startHour + 1; // +1 to include the last hour
    private final int hourHeight = 60; // Height of each hour block
    private final int timelineWidth = 80; // Width of the time column
    private final int reminderBlockHeight = 40; // Height of reminder blocks
    private final int reminderBlockWidth = 200; // Width of reminder blocks

    private LocalDate today;
    private List<Reminder> todaysReminders;
    private Map<String, Task> taskCache;
    private Map<Rectangle, Reminder> reminderBlockBounds; // Track click regions
    
    // Cached timeline background image for performance
    private BufferedImage timelineBackgroundCache;
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    
    // Reference to the scroll pane container for scrolling to current time
    private JScrollPane scrollPaneContainer;

    public TodayPanel(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.today = LocalDate.now();
        this.todaysReminders = new ArrayList<>();
        this.taskCache = new HashMap<>();
        this.reminderBlockBounds = new HashMap<>();

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Listen for task changes (which include reminder changes)
        taskManager.addTaskChangeListener(() -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                refreshData();
            });
        });

        // Add component listener to handle resizing
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Invalidate cache on resize
                invalidateTimelineCache();
                refreshData();
                repaint();
            }
        });
        
        // Add mouse listener for clicking reminder blocks
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleReminderClick(e.getPoint());
            }
        });
        
        // Add cursor change and tooltip on hover
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean overReminder = false;
                Reminder hoveredReminder = null;
                for (Map.Entry<Rectangle, Reminder> entry : reminderBlockBounds.entrySet()) {
                    if (entry.getKey().contains(e.getPoint())) {
                        overReminder = true;
                        hoveredReminder = entry.getValue();
                        break;
                    }
                }
                setCursor(overReminder ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                
                // Update tooltip
                if (hoveredReminder != null) {
                    setToolTipText(getReminderTooltip(hoveredReminder));
                } else {
                    setToolTipText(null);
                }
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
        this.reminderBlockBounds.clear();

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
     * Invalidate the cached timeline background.
     */
    private void invalidateTimelineCache() {
        timelineBackgroundCache = null;
        cachedWidth = -1;
        cachedHeight = -1;
    }
    
    /**
     * Handle clicks on reminder blocks to jump to the task.
     */
    private void handleReminderClick(Point clickPoint) {
        for (Map.Entry<Rectangle, Reminder> entry : reminderBlockBounds.entrySet()) {
            if (entry.getKey().contains(clickPoint)) {
                Reminder reminder = entry.getValue();
                jumpToReminder(reminder);
                break;
            }
        }
    }
    
    /**
     * Jump to the task or checklist associated with a reminder.
     */
    private void jumpToReminder(Reminder reminder) {
        DailyChecklist app = DailyChecklist.getInstance();
        if (app == null) return;
        
        if (reminder.getTaskId() != null) {
            // Jump to specific task
            Task task = taskManager.getTaskById(reminder.getTaskId());
            if (task != null) {
                app.jumpToTask(task);
            }
        } else if (reminder.getChecklistName() != null) {
            // Jump to checklist
            app.showCustomChecklist(reminder.getChecklistName());
        }
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
        
        // Clear previous bounds before redrawing
        reminderBlockBounds.clear();

        // Draw cached timeline background or create it if needed
        if (timelineBackgroundCache == null || cachedWidth != width || cachedHeight != height) {
            createTimelineBackgroundCache(width, height);
        }
        
        // Draw the cached background
        g2d.drawImage(timelineBackgroundCache, 0, 0, null);

        // Draw reminder blocks on top (these change frequently)
        drawReminderBlocks(g2d, width);
        
        // Draw current time marker (red line)
        drawCurrentTimeMarker(g2d, width);

        g2d.dispose();
    }
    
    /**
     * Create a cached image of the timeline background (hour lines and labels).
     * This is expensive to draw but doesn't change, so we cache it.
     */
    private void createTimelineBackgroundCache(int width, int height) {
        cachedWidth = width;
        cachedHeight = height;
        timelineBackgroundCache = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = timelineBackgroundCache.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw timeline background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Draw hour lines and time labels
        g2d.setColor(new Color(120, 120, 120)); // Medium gray for better visibility
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
        // Track bounds for click detection
        reminderBlockBounds.put(new Rectangle(x, y, reminderBlockWidth, reminderBlockHeight), reminder);
        
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
    
    /**
     * Get tooltip text for a reminder.
     */
    private String getReminderTooltip(Reminder reminder) {
        StringBuilder tooltip = new StringBuilder("<html>");
        
        // Add time
        tooltip.append(String.format("<b>%02d:%02d</b><br>", reminder.getHour(), reminder.getMinute()));
        
        // Add title
        String title = getReminderTitle(reminder);
        if (title != null) {
            tooltip.append(title);
        }
        
        // Add breadcrumb if present
        String subtitle = getReminderSubtitle(reminder);
        if (subtitle != null && !subtitle.isEmpty()) {
            tooltip.append("<br>").append(subtitle);
        }
        
        // Add checklist info if not a task reminder
        if (reminder.getTaskId() == null && reminder.getChecklistName() != null) {
            tooltip.append("<br><i>Checklist reminder</i>");
        }
        
        tooltip.append("</html>");
        return tooltip.toString();
    }

    /**
     * Draw a red line at the current time of day.
     */
    private void drawCurrentTimeMarker(Graphics2D g2d, int width) {
        LocalTime now = LocalTime.now();
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();
        
        // Calculate Y position for current time
        int y = (currentHour - startHour) * hourHeight + (currentMinute * hourHeight / 60);
        
        // Draw red line
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(timelineWidth, y, width, y);
        
        // Draw time label with background
        String timeLabel = String.format("%02d:%02d", currentHour, currentMinute);
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 11f));
        FontMetrics fm = g2d.getFontMetrics();
        int labelWidth = fm.stringWidth(timeLabel);
        int labelHeight = fm.getHeight();
        
        // Draw label background
        g2d.setColor(Color.RED);
        g2d.fillRect(timelineWidth + 5, y - labelHeight / 2, labelWidth + 6, labelHeight);
        
        // Draw label text
        g2d.setColor(Color.WHITE);
        g2d.drawString(timeLabel, timelineWidth + 8, y + fm.getAscent() / 2);
    }
    
    /**
     * Set the scroll pane container reference.
     * This should be called after adding the panel to a scroll pane.
     */
    public void setScrollPaneContainer(JScrollPane scrollPane) {
        this.scrollPaneContainer = scrollPane;
    }
    
    /**
     * Scroll to the current time of day.
     */
    public void scrollToCurrentTime() {
        LocalTime now = LocalTime.now();
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();
        
        // Calculate Y position for current time
        int y = (currentHour - startHour) * hourHeight + (currentMinute * hourHeight / 60);
        
        // Scroll to the position (center it in viewport if possible)
        if (scrollPaneContainer != null) {
            JViewport viewport = scrollPaneContainer.getViewport();
            int viewportHeight = viewport.getHeight();
            
            // Center the current time in the viewport
            int scrollY = Math.max(0, y - viewportHeight / 2);
            
            Rectangle rect = new Rectangle(0, scrollY, 1, viewportHeight);
            scrollRectToVisible(rect);
        } else {
            // Fallback if no scroll pane set
            Rectangle rect = new Rectangle(0, y - 100, 1, 200);
            scrollRectToVisible(rect);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, hoursToShow * hourHeight + 50);
    }
}