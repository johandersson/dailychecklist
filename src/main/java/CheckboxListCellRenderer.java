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
 */import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

@SuppressWarnings("serial")
public class CheckboxListCellRenderer extends JPanel implements ListCellRenderer<Task> {
    private static final long serialVersionUID = 1L;
    private boolean isChecked;
    private String taskName;
    private String weekdayAbbreviation;
    private Color weekdayColor;
    private boolean isWeekdayTask;
    private Font circleFont; // Font for the text inside the circle
    private String doneDate; // Timestamp when task was completed
    // Reserved areas on the right for icons: reminder area + weekday area.
    private static final int REMINDER_ICON_AREA = 80; // space reserved for reminder clock + optional time text
    private static final int WEEKDAY_ICON_AREA = 40; // space reserved for weekday circle
    private static final int RIGHT_ICON_SPACE = REMINDER_ICON_AREA + WEEKDAY_ICON_AREA + 12; // total reserved space
    
    private boolean showChecklistInfo; // Whether to show checklist name in display
    private boolean isSubtask; // True if this task is a subtask (for indentation)
    private ChecklistNameManager checklistNameManager; // Manager to resolve checklist IDs to names
    private transient TaskManager taskManager;
    private String taskId;
    private Reminder taskReminder;
    
    // Cached checkmark image for performance (smaller to avoid overflow)
    private static final BufferedImage checkmarkImage;

    // Mapping of weekdays to abbreviations and WCAG-compliant colors
    private static final Map<String, String> WEEKDAY_ABBREVIATIONS = new HashMap<>();
    private static final Map<String, Color> WEEKDAY_COLORS = new HashMap<>();

    static {
        WEEKDAY_ABBREVIATIONS.put("monday", "Mo");
        WEEKDAY_ABBREVIATIONS.put("tuesday", "Tu");
        WEEKDAY_ABBREVIATIONS.put("wednesday", "We");
        WEEKDAY_ABBREVIATIONS.put("thursday", "Th");
        WEEKDAY_ABBREVIATIONS.put("friday", "Fr");
        WEEKDAY_ABBREVIATIONS.put("saturday", "Sa");
        WEEKDAY_ABBREVIATIONS.put("sunday", "Su");

        // WCAG 2.1 compliant colors
        WEEKDAY_COLORS.put("monday", new Color(165, 42, 42));   // Brown
        WEEKDAY_COLORS.put("tuesday", new Color(0, 90, 156));   // Dark Blue
        WEEKDAY_COLORS.put("wednesday", new Color(139, 128, 0)); // Dark Yellow
        WEEKDAY_COLORS.put("thursday", new Color(34, 139, 34));  // Forest Green
        WEEKDAY_COLORS.put("friday", new Color(139, 69, 19));    // Saddle Brown
        WEEKDAY_COLORS.put("saturday", new Color(255, 69, 0));   // Dark Orange
        WEEKDAY_COLORS.put("sunday", new Color(199, 21, 133));   // Dark Pink
        
        // Pre-render checkmark image for performance and ensure it fits the checkbox
        checkmarkImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = checkmarkImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(76, 175, 80)); // Material green checkmark
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Draw a clearer checkmark centered in the 16x16 image
        g2.drawLine(3, 9, 7, 13);
        g2.drawLine(7, 13, 13, 5);
        g2.dispose();
    }

    private final Dimension preferredSize = new Dimension(200, 50);

    @SuppressWarnings("this-escape")
    public CheckboxListCellRenderer() {
        this(false); // Default to not showing checklist info
    }

    @SuppressWarnings("this-escape")
    public CheckboxListCellRenderer(boolean showChecklistInfo) {
        this(showChecklistInfo, null);
    }

    public CheckboxListCellRenderer(boolean showChecklistInfo, ChecklistNameManager checklistNameManager) {
        this.circleFont = getAvailableFont("Yu Gothic UI", Font.BOLD, 12); // Font for circle text
        this.showChecklistInfo = showChecklistInfo;
        this.checklistNameManager = checklistNameManager;
        this.taskManager = null;
    }

    public CheckboxListCellRenderer(TaskManager taskManager) {
        this(false, null);
        this.taskManager = taskManager;
    }

    @Override
    public Dimension getPreferredSize() {
        return preferredSize;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Task> list, Task task, int index, boolean isSelected, boolean cellHasFocus) {
        this.isChecked = task.isDone();
        this.taskName = task.getName();
        this.taskId = task.getId();
        this.taskReminder = null;

        // Indentation for subtasks (one level)
        this.isSubtask = (task.getParentId() != null);

        // Find a reminder that targets this specific task (task-level reminder)
        if (taskManager != null && this.taskId != null) {
            List<Reminder> reminders = taskManager.getReminders();
            for (Reminder r : reminders) {
                if (this.taskId.equals(r.getTaskId())) {
                    this.taskReminder = r;
                    break;
                }
            }
        }

        // Resolve checklist name from ID if showing checklist info
        String checklistName = null;
        if (showChecklistInfo && task.getChecklistId() != null && checklistNameManager != null) {
            Checklist checklist = checklistNameManager.getChecklistById(task.getChecklistId());
            if (checklist != null) {
                checklistName = checklist.getName();
            }
        }

        // If showing checklist info, append checklist name to task name
        if (showChecklistInfo && checklistName != null && !checklistName.trim().isEmpty()) {
            this.taskName = task.getName() + " (" + checklistName + ")";
        }
        
        String weekdayKey = task.getWeekday() != null ? task.getWeekday().toLowerCase() : null;
        this.weekdayAbbreviation = WEEKDAY_ABBREVIATIONS.get(weekdayKey);
        this.weekdayColor = WEEKDAY_COLORS.get(weekdayKey);
        this.isWeekdayTask = task.getWeekday() != null && WEEKDAY_ABBREVIATIONS.containsKey(weekdayKey);
        this.doneDate = task.getDoneDate();

        // Build informative tooltip for this task list cell.
        StringBuilder tip = new StringBuilder();
        if (this.taskReminder != null) {
            tip.append(String.format("Reminder: %04d-%02d-%02d %02d:%02d", this.taskReminder.getYear(), this.taskReminder.getMonth(), this.taskReminder.getDay(), this.taskReminder.getHour(), this.taskReminder.getMinute()));
        }
        if (this.isWeekdayTask) {
            String wd = task.getWeekday();
            if (wd != null && !wd.isEmpty()) {
                // Capitalize first letter
                String nice = wd.substring(0, 1).toUpperCase() + wd.substring(1).toLowerCase();
                if (tip.length() > 0) tip.append(" — ");
                tip.append("Weekday: ").append(nice);
            }
        }
        setToolTipText(tip.length() > 0 ? tip.toString() : null);

        setFont(FontManager.getTaskListFont()); // Use consistent font for all task lists
        setOpaque(true); // Ensure background is painted
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            putClientProperty("selected", Boolean.TRUE);
        } else {
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
            putClientProperty("selected", Boolean.FALSE);
        }

        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Enable anti-aliasing for smoother rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int baseIndent = 40;
        int subtaskIndent = 24;
        int textStartX = baseIndent + (isSubtask ? subtaskIndent : 0); // Indent subtasks

        int checkboxX = 10 + (isSubtask ? subtaskIndent : 0);
        int checkboxY = getHeight() / 2 - 11, checkboxSize = 22;

        // Define checkbox dimensions (slightly increased to better fit checkmark)
        // checkboxX now set above

        // Draw subtle shadow behind checkbox
        g2.setColor(new Color(200, 200, 200, 100)); // Light gray shadow with transparency
        g2.fillRoundRect(checkboxX + 2, checkboxY + 2, checkboxSize, checkboxSize, 8, 8);

        // Draw checkbox outline
        g2.setColor(new Color(120, 120, 120)); // Softer gray outline
        g2.drawRoundRect(checkboxX, checkboxY, checkboxSize, checkboxSize, 8, 8);

        // Fill checkbox with white
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(checkboxX + 1, checkboxY + 1, checkboxSize - 2, checkboxSize - 2, 8, 8);

        // Draw checkmark if selected (center it within the checkbox)
        if (isChecked) {
            int imgW = checkmarkImage.getWidth();
            int imgH = checkmarkImage.getHeight();
            int imgX = checkboxX + (checkboxSize - imgW) / 2;
            int imgY = checkboxY + (checkboxSize - imgH) / 2;
            g2.drawImage(checkmarkImage, imgX, imgY, null);
        }

        // Draw the task text next to the checkbox. Reserve space on the right for weekday/reminder icons.
        g2.setColor(getForeground());
        g2.setFont(getFont());
        FontMetrics fmMain = g2.getFontMetrics();
        int textY = getHeight() / 2 + 5;
        int availableWidth = getWidth() - textStartX - RIGHT_ICON_SPACE - 6;
        String drawTaskName = taskName != null ? taskName : "";
        if (availableWidth > 12 && fmMain.stringWidth(drawTaskName) > availableWidth) {
            while (fmMain.stringWidth(drawTaskName + "…") > availableWidth && drawTaskName.length() > 0) {
                drawTaskName = drawTaskName.substring(0, drawTaskName.length() - 1);
            }
            drawTaskName = drawTaskName + "…";
        }
        g2.drawString(drawTaskName, textStartX, textY);

        // Draw timestamp if task is checked - always in black, smaller font beneath the title
        if (isChecked && doneDate != null && !doneDate.isEmpty()) {
            g2.setColor(Color.BLACK);
            g2.setFont(getFont().deriveFont(Font.PLAIN, FontManager.SIZE_SMALL));
            FontMetrics fmSmall = g2.getFontMetrics();
            int availableWidthSmall = getWidth() - textStartX - RIGHT_ICON_SPACE - 6;
            String timeText = "✓ " + doneDate;
            if (availableWidthSmall > 12 && fmSmall.stringWidth(timeText) > availableWidthSmall) {
                while (fmSmall.stringWidth(timeText + "…") > availableWidthSmall && timeText.length() > 0) {
                    timeText = timeText.substring(0, timeText.length() - 1);
                }
                timeText = timeText + "…";
            }
            g2.drawString(timeText, textStartX, getHeight() / 2 + 20);
        }

        // Draw weekday circle at the far right so it doesn't overlap with reminder icon
        if (isWeekdayTask) {
            int circleSize = 30;
            int areaX = getWidth() - WEEKDAY_ICON_AREA;
            int circleX = areaX + (WEEKDAY_ICON_AREA - circleSize) / 2;
            int circleY = getHeight() / 2 - circleSize / 2;
            g2.setColor(weekdayColor != null ? weekdayColor : new Color(120, 120, 120));
            g2.fillOval(circleX, circleY, circleSize, circleSize);

            // Draw the weekday abbreviation inside the circle (centered)
            g2.setColor(Color.WHITE);
            g2.setFont(circleFont);
            FontMetrics fm = g2.getFontMetrics();
            int textX = circleX + (circleSize - fm.stringWidth(weekdayAbbreviation != null ? weekdayAbbreviation : "")) / 2;
            int textCenterY = circleY + circleSize / 2 + (fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(weekdayAbbreviation != null ? weekdayAbbreviation : "", textX, textCenterY);
        }

        // Draw a reminder clock icon (and optional time text) inside the reserved right area for task-level reminders
        if (taskReminder != null) {
            ReminderClockIcon.State state = computeState(taskReminder);
            javax.swing.Icon icon = IconCache.getReminderClockIcon(taskReminder.getHour(), taskReminder.getMinute(), state, true);
            int iconW = icon.getIconWidth();
            int iconH = icon.getIconHeight();
            // reminder area is left of the weekday area
            int areaX = getWidth() - WEEKDAY_ICON_AREA - REMINDER_ICON_AREA;
            // center the icon (and its time text) within the reminder reserved area
            int iconX = areaX + Math.max(2, (REMINDER_ICON_AREA - iconW) / 2);
            int iconY = getHeight() / 2 - iconH / 2;
            icon.paintIcon(this, g2, iconX, iconY);
        }
    }

    private ReminderClockIcon.State computeState(Reminder r) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime dt = java.time.LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
        if (dt.isBefore(now)) {
            long minutesOverdue = java.time.Duration.between(dt, now).toMinutes();
            if (minutesOverdue > 60) return ReminderClockIcon.State.VERY_OVERDUE;
            return ReminderClockIcon.State.OVERDUE;
        }
        long minutes = java.time.Duration.between(now, dt).toMinutes();
        if (minutes <= 60) return ReminderClockIcon.State.DUE_SOON;
        return ReminderClockIcon.State.FUTURE;
    }


    /**
     * Checks if a font is available on the system. If not, falls back to Arial.
     */
    private Font getAvailableFont(String preferredFontName, int style, int size) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();

        for (String font : availableFonts) {
            if (font.equalsIgnoreCase(preferredFontName)) {
                return new Font(preferredFontName, style, size);
            }
        }
        return new Font(FontManager.FONT_NAME, style, size); // Fallback to Yu Gothic UI
    }
}


