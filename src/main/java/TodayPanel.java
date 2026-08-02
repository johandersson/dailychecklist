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
    private final int calendarEventBlockWidth = 200; // Width of imported calendar-event blocks
    private final int calendarEventAreaGap = 20; // Gap separating calendar events from reminder blocks
    private final int allDayEventHeight = 24; // Height of an all-day event banner

    private LocalDate today;
    private List<Reminder> todaysReminders;
    private List<CalendarEvent> todaysCalendarEvents;
    private Map<String, Task> taskCache;
    private Map<Rectangle, Reminder> reminderBlockBounds; // Track click regions
    private Map<Rectangle, CalendarEvent> calendarEventBlockBounds; // Track click regions for imported events
    
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
        this.todaysCalendarEvents = new ArrayList<>();
        this.taskCache = new HashMap<>();
        this.reminderBlockBounds = new HashMap<>();
        this.calendarEventBlockBounds = new HashMap<>();

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
        
        // Add mouse listener for clicking reminder and calendar-event blocks
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    if (!handleReminderClick(e.getPoint())) {
                        handleCalendarEventClick(e.getPoint());
                    }
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handleRightClick(e.getPoint(), e.getX(), e.getY());
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handleRightClick(e.getPoint(), e.getX(), e.getY());
                }
            }
        });
        
        // Add cursor change and tooltip on hover
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean overBlock = false;
                Reminder hoveredReminder = null;
                CalendarEvent hoveredEvent = null;
                for (Map.Entry<Rectangle, Reminder> entry : reminderBlockBounds.entrySet()) {
                    if (entry.getKey().contains(e.getPoint())) {
                        overBlock = true;
                        hoveredReminder = entry.getValue();
                        break;
                    }
                }
                if (hoveredReminder == null) {
                    for (Map.Entry<Rectangle, CalendarEvent> entry : calendarEventBlockBounds.entrySet()) {
                        if (entry.getKey().contains(e.getPoint())) {
                            overBlock = true;
                            hoveredEvent = entry.getValue();
                            break;
                        }
                    }
                }
                setCursor(overBlock ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                
                // Update tooltip
                if (hoveredReminder != null) {
                    setToolTipText(getReminderTooltip(hoveredReminder));
                } else if (hoveredEvent != null) {
                    setToolTipText(getCalendarEventTooltip(hoveredEvent));
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
        this.todaysCalendarEvents = getTodaysCalendarEvents();
        this.taskCache.clear();
        this.reminderBlockBounds.clear();
        this.calendarEventBlockBounds.clear();

        // Cache tasks for reminders
        for (Reminder reminder : todaysReminders) {
            if (reminder.getTaskId() != null) {
                Task task = taskManager.getTaskById(reminder.getTaskId());
                if (task != null) {
                    taskCache.put(reminder.getTaskId(), task);
                }
            }
        }

        // Revalidate to update preferred size based on new reminder count
        revalidate();
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
     * @return true if a reminder block was clicked.
     */
    private boolean handleReminderClick(Point clickPoint) {
        for (Map.Entry<Rectangle, Reminder> entry : reminderBlockBounds.entrySet()) {
            if (entry.getKey().contains(clickPoint)) {
                Reminder reminder = entry.getValue();
                jumpToReminder(reminder);
                return true;
            }
        }
        return false;
    }

    /**
     * Handle clicks on imported calendar-event blocks to show their details.
     */
    private void handleCalendarEventClick(Point clickPoint) {
        for (Map.Entry<Rectangle, CalendarEvent> entry : calendarEventBlockBounds.entrySet()) {
            if (entry.getKey().contains(clickPoint)) {
                showCalendarEventDetails(entry.getValue());
                break;
            }
        }
    }

    /**
     * Handle right-clicks on reminder or calendar-event blocks to show a context menu.
     */
    private void handleRightClick(Point clickPoint, int x, int y) {
        for (Map.Entry<Rectangle, Reminder> entry : reminderBlockBounds.entrySet()) {
            if (entry.getKey().contains(clickPoint)) {
                showReminderContextMenu(entry.getValue(), x, y);
                return;
            }
        }
        for (Map.Entry<Rectangle, CalendarEvent> entry : calendarEventBlockBounds.entrySet()) {
            if (entry.getKey().contains(clickPoint)) {
                showCalendarEventContextMenu(entry.getValue(), x, y);
                return;
            }
        }
    }
    
    /**
     * Show context menu for a reminder block.
     */
    private void showReminderContextMenu(Reminder reminder, int x, int y) {
        JPopupMenu popup = new JPopupMenu();
        
        JMenuItem editItem = new JMenuItem("Edit Reminder");
        editItem.addActionListener(e -> editReminder(reminder));
        popup.add(editItem);
        
        JMenuItem deleteItem = new JMenuItem("Delete Reminder");
        deleteItem.addActionListener(e -> deleteReminder(reminder));
        popup.add(deleteItem);
        
        popup.show(this, x, y);
    }

    /**
     * Show context menu for an imported calendar-event block.
     */
    private void showCalendarEventContextMenu(CalendarEvent event, int x, int y) {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem detailsItem = new JMenuItem("View Details");
        detailsItem.addActionListener(e -> showCalendarEventDetails(event));
        popup.add(detailsItem);

        JMenuItem deleteItem = new JMenuItem("Delete Event");
        deleteItem.addActionListener(e -> deleteCalendarEvent(event));
        popup.add(deleteItem);

        popup.show(this, x, y);
    }

    /**
     * Show a simple details dialog for an imported calendar event.
     */
    private void showCalendarEventDetails(CalendarEvent event) {
        JOptionPane.showMessageDialog(this, getCalendarEventDetailsText(event), "Calendar Event", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Delete an imported calendar event after confirmation.
     */
    private void deleteCalendarEvent(CalendarEvent event) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Remove this imported calendar event?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            taskManager.removeCalendarEvent(event);
            refreshData();
            repaint();
        }
    }
    
    /**
     * Open the reminder edit dialog for the selected reminder.
     */
    private void editReminder(Reminder reminder) {
        String checklistName = reminder.getChecklistName();
        if (checklistName == null) {
            JOptionPane.showMessageDialog(this, "Cannot edit this reminder.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        ReminderEditDialog dialog = new ReminderEditDialog(
            taskManager, 
            checklistName, 
            reminder, 
            () -> {
                refreshData();
                repaint();
            },
            reminder.getTaskId()
        );
        dialog.setVisible(true);
    }
    
    /**
     * Delete the selected reminder.
     */
    private void deleteReminder(Reminder reminder) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete this reminder?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            taskManager.removeReminder(reminder);
            refreshData();
            repaint();
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

        int todayDowBit = 1 << (today.getDayOfWeek().getValue() - 1); // 1=Mon..7=Sun

        for (Reminder reminder : allReminders) {
            try {
                if (reminder.isRecurring()) {
                    if ((reminder.getDaysBitmask() & todayDowBit) != 0) {
                        todays.add(reminder);
                    }
                    continue;
                }

                // Non-recurring: match exact date
                if (reminder.getYear() == today.getYear() &&
                    reminder.getMonth() == today.getMonthValue() &&
                    reminder.getDay() == today.getDayOfMonth()) {
                    todays.add(reminder);
                }
            } catch (Exception ex) {
                // skip malformed reminders
            }
        }

        return todays;
    }

    /**
     * Get all imported calendar events that occur today.
     */
    private List<CalendarEvent> getTodaysCalendarEvents() {
        List<CalendarEvent> todays = new ArrayList<>();
        for (CalendarEvent event : taskManager.getCalendarEvents()) {
            try {
                if (event.occursOn(today)) {
                    todays.add(event);
                }
            } catch (Exception ex) {
                // skip malformed events
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
        calendarEventBlockBounds.clear();

        // Draw cached timeline background or create it if needed
        if (timelineBackgroundCache == null || cachedWidth != width || cachedHeight != height) {
            createTimelineBackgroundCache(width, height);
        }
        
        // Draw the cached background
        g2d.drawImage(timelineBackgroundCache, 0, 0, null);

        // Draw reminder blocks on top (these change frequently)
        drawReminderBlocks(g2d, width);

        // Draw imported calendar-event blocks in their own non-overlapping area
        drawCalendarEventBlocks(g2d, width);
        
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
     * X coordinate where the imported-calendar-events column area begins: always
     * to the right of every reminder block so the two block kinds never overlap.
     */
    private int calculateCalendarEventsAreaX() {
        int reminderColumns = getMaxOverlappingReminders();
        int reminderAreaWidth = reminderColumns > 0 ? reminderColumns * (reminderBlockWidth + 10) : 0;
        return timelineWidth + 10 + reminderAreaWidth + calendarEventAreaGap;
    }

    /**
     * Draw imported calendar-event blocks. All-day events are shown as stacked
     * banners at the top; timed events are laid out via {@link CalendarEventLayout}
     * so concurrent events get their own column instead of overlapping.
     */
    private void drawCalendarEventBlocks(Graphics2D g2d, int width) {
        if (todaysCalendarEvents.isEmpty()) return;

        int areaX = calculateCalendarEventsAreaX();

        List<CalendarEvent> allDayEvents = new ArrayList<>();
        List<CalendarEvent> timedEvents = new ArrayList<>();
        for (CalendarEvent event : todaysCalendarEvents) {
            (event.isAllDay() ? allDayEvents : timedEvents).add(event);
        }

        for (int i = 0; i < allDayEvents.size(); i++) {
            int y = 4 + i * (allDayEventHeight + 4);
            drawCalendarEventBlock(g2d, allDayEvents.get(i), areaX, y, calendarEventBlockWidth, allDayEventHeight, null, null);
        }

        for (CalendarEventLayout.PositionedEvent pe : CalendarEventLayout.layout(timedEvents, today)) {
            int y = (pe.start.getHour() - startHour) * hourHeight + (pe.start.getMinute() * hourHeight / 60);
            int durationMinutes = (pe.end.getHour() * 60 + pe.end.getMinute()) - (pe.start.getHour() * 60 + pe.start.getMinute());
            int blockHeight = Math.max(reminderBlockHeight, Math.round(durationMinutes * hourHeight / 60f));
            int x = areaX + pe.column * (calendarEventBlockWidth + 10);
            drawCalendarEventBlock(g2d, pe.event, x, y, calendarEventBlockWidth, blockHeight, pe.start, pe.end);
        }
    }

    /**
     * Draw a single calendar-event block using a distinct color from reminder blocks.
     */
    private void drawCalendarEventBlock(Graphics2D g2d, CalendarEvent event, int x, int y, int blockWidth, int blockHeight, LocalTime start, LocalTime end) {
        calendarEventBlockBounds.put(new Rectangle(x, y, blockWidth, blockHeight), event);

        g2d.setColor(new Color(0, 150, 136)); // Teal, distinct from reminder steel-blue
        g2d.fillRoundRect(x, y, blockWidth, blockHeight, 8, 8);

        g2d.setColor(new Color(0, 105, 92));
        g2d.drawRoundRect(x, y, blockWidth, blockHeight, 8, 8);

        g2d.setColor(Color.WHITE);
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 11f));

        String title = event.getTitle();
        if (title.length() > 20) {
            title = title.substring(0, 17) + "...";
        }
        FontMetrics fm = g2d.getFontMetrics();
        int textY = y + fm.getAscent() + 5;
        g2d.drawString(title, x + 8, textY);

        String subtitle = getCalendarEventSubtitle(event, start, end);
        if (subtitle != null && !subtitle.isEmpty() && blockHeight >= reminderBlockHeight) {
            g2d.setFont(g2d.getFont().deriveFont(Font.PLAIN, 9f));
            textY += fm.getHeight() + 2;
            if (subtitle.length() > 25) {
                subtitle = subtitle.substring(0, 22) + "...";
            }
            g2d.drawString(subtitle, x + 8, textY);
        }
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
     * Build the compact subtitle line (time range and optional location) shown
     * below a calendar-event block's title.
     */
    private String getCalendarEventSubtitle(CalendarEvent event, LocalTime start, LocalTime end) {
        String timePart = event.isAllDay() ? "All day" : String.format("%02d:%02d\u2013%02d:%02d", start.getHour(), start.getMinute(), end.getHour(), end.getMinute());
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            return timePart + " \u2022 " + event.getLocation();
        }
        return timePart;
    }

    /**
     * Get tooltip text for an imported calendar event.
     */
    private String getCalendarEventTooltip(CalendarEvent event) {
        LocalTime start = event.effectiveStartTime(today);
        LocalTime end = event.effectiveEndTime(today);
        StringBuilder tooltip = new StringBuilder("<html>");
        tooltip.append("<b>").append(event.getTitle()).append("</b><br>");
        tooltip.append(getCalendarEventSubtitle(event, start, end));
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            tooltip.append("<br><i>").append(event.getDescription()).append("</i>");
        }
        tooltip.append("</html>");
        return tooltip.toString();
    }

    /**
     * Build a plain-text summary for the calendar-event details dialog.
     */
    private String getCalendarEventDetailsText(CalendarEvent event) {
        LocalTime start = event.effectiveStartTime(today);
        LocalTime end = event.effectiveEndTime(today);
        StringBuilder sb = new StringBuilder();
        sb.append(event.getTitle()).append('\n');
        sb.append(getCalendarEventSubtitle(event, start, end)).append('\n');
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            sb.append("Location: ").append(event.getLocation()).append('\n');
        }
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            sb.append('\n').append(event.getDescription());
        }
        return sb.toString();
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
        // Calculate width based on maximum overlapping reminders at any time
        int maxOverlapping = getMaxOverlappingReminders();
        
        // Base width includes timeline + padding
        int baseWidth = timelineWidth + 20;
        
        // Add width for reminder blocks
        int reminderAreaWidth = 0;
        if (maxOverlapping > 0) {
            reminderAreaWidth = (maxOverlapping * reminderBlockWidth) + ((maxOverlapping - 1) * 10);
        }

        // Add width for imported calendar-event blocks, placed after the reminder area
        int calendarAreaWidth = 0;
        int maxCalendarColumns = getMaxOverlappingCalendarEvents();
        if (maxCalendarColumns > 0) {
            calendarAreaWidth = calendarEventAreaGap + (maxCalendarColumns * calendarEventBlockWidth) + ((maxCalendarColumns - 1) * 10);
        }
        
        int totalWidth = Math.max(600, baseWidth + reminderAreaWidth + calendarAreaWidth);
        
        return new Dimension(totalWidth, hoursToShow * hourHeight + 50);
    }
    
    /**
     * Calculate the maximum number of overlapping reminders at any single time.
     */
    private int getMaxOverlappingReminders() {
        if (todaysReminders == null || todaysReminders.isEmpty()) {
            return 0;
        }
        
        Map<String, Integer> reminderCountByTime = new HashMap<>();
        
        for (Reminder reminder : todaysReminders) {
            String timeKey = reminder.getHour() + ":" + reminder.getMinute();
            reminderCountByTime.put(timeKey, reminderCountByTime.getOrDefault(timeKey, 0) + 1);
        }
        
        int maxCount = 0;
        for (int count : reminderCountByTime.values()) {
            maxCount = Math.max(maxCount, count);
        }
        
        return maxCount;
    }

    /**
     * Calculate the maximum number of overlapping columns needed for today's
     * timed calendar events (all-day events always use a single column).
     */
    private int getMaxOverlappingCalendarEvents() {
        if (todaysCalendarEvents == null || todaysCalendarEvents.isEmpty()) {
            return 0;
        }
        int maxColumns = 0;
        List<CalendarEvent> timedEvents = new ArrayList<>();
        for (CalendarEvent event : todaysCalendarEvents) {
            if (event.isAllDay()) {
                maxColumns = Math.max(maxColumns, 1);
            } else {
                timedEvents.add(event);
            }
        }
        for (CalendarEventLayout.PositionedEvent pe : CalendarEventLayout.layout(timedEvents, today)) {
            maxColumns = Math.max(maxColumns, pe.columnCount);
        }
        return maxColumns;
    }
}