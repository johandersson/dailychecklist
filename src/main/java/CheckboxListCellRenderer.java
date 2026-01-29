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
 */import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.util.HashMap;
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
    // Layout constants are centralized in UiLayout
    
    private boolean showChecklistInfo; // Whether to show checklist name in display
    private boolean isSubtask; // True if this task is a subtask (for indentation)
    private ChecklistNameManager checklistNameManager; // Manager to resolve checklist IDs to names
    private transient TaskManager taskManager;
    private boolean showSubtaskBreadcrumb = false; // When true, subtasks are not indented and show breadcrumb text to the right
    private boolean showAddSubtaskIcon = true; // Controls whether to draw the add-subtask icon
    private String breadcrumbText = null;
    private final SubtaskBreadcrumb breadcrumbComponent = new SubtaskBreadcrumb();
    private String taskId;
    private Reminder taskReminder;
    private Font lastBaseFont = null;
    private Font smallFont = null;
    private FontMetrics fmMainCached = null;
    private FontMetrics fmSmallCached = null;
    
    // Checkmark is provided by IconCache

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
        
        // No local checkmark bitmap; use IconCache.getCheckmarkIcon()
    }

    private final Dimension preferredSize = new Dimension(200, 50);

    @SuppressWarnings("this-escape")
    public CheckboxListCellRenderer() {
        this(false); // Default to not showing checklist info
    }

    @SuppressWarnings("this-escape")
    public CheckboxListCellRenderer(boolean showChecklistInfo) {
        this(showChecklistInfo, (ChecklistNameManager) null);
    }

    public CheckboxListCellRenderer(boolean showChecklistInfo, ChecklistNameManager checklistNameManager) {
        this.circleFont = getAvailableFont("Yu Gothic UI", Font.BOLD, 12); // Font for circle text
        this.showChecklistInfo = showChecklistInfo;
        this.checklistNameManager = checklistNameManager;
        this.taskManager = null;
    }

    public CheckboxListCellRenderer(boolean showChecklistInfo, TaskManager taskManager) {
        this(showChecklistInfo, (ChecklistNameManager) null);
        this.taskManager = taskManager;
    }

    public void setShowSubtaskBreadcrumb(boolean show) {
        this.showSubtaskBreadcrumb = show;
    }

    public void setShowAddSubtaskIcon(boolean show) {
        this.showAddSubtaskIcon = show;
    }

    public CheckboxListCellRenderer(TaskManager taskManager) {
        this(false, (ChecklistNameManager) null);
        this.taskManager = taskManager;
    }

    @Override
    public Dimension getPreferredSize() {
        return preferredSize;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Task> list, Task task, int index, boolean isSelected, boolean cellHasFocus) {
        populateFromTask(task);
        buildToolTip(task);
        applySelectionStyles(list, isSelected);
        setFont(FontManager.getTaskListFont()); // Use consistent font for all task lists
        setOpaque(true); // Ensure background is painted
        return this;
    }

    // Populate renderer state fields from the given Task (keeps getListCellRendererComponent small)
    private void populateFromTask(Task task) {
        setBasicFields(task);
        resolveReminderForTask();
        resolveChecklistInfo(task);
        resolveBreadcrumb(task);
        resolveWeekday(task);
    }

    private void setBasicFields(Task task) {
        this.isChecked = task.isDone();
        this.taskName = task.getName();
        this.taskId = task.getId();
        this.taskReminder = null;
        this.isSubtask = (task.getParentId() != null) && !this.showSubtaskBreadcrumb;
        this.breadcrumbText = null;
        this.doneDate = task.getDoneDate();
    }

    private void resolveReminderForTask() {
        if (taskManager != null && this.taskId != null) {
            this.taskReminder = taskManager.getReminderForTask(this.taskId);
        }
    }

    private void resolveChecklistInfo(Task task) {
        if (showChecklistInfo && task.getChecklistId() != null && checklistNameManager != null) {
            String cname = checklistNameManager.getNameById(task.getChecklistId());
            if (cname != null && !cname.trim().isEmpty()) {
                this.taskName = task.getName() + " (" + cname + ")";
            }
        }
    }

    private void resolveBreadcrumb(Task task) {
        if (!this.showSubtaskBreadcrumb || task.getParentId() == null) return;
        String parentName = null;
        if (taskManager != null) {
            Task parent = taskManager.getTaskById(task.getParentId());
            if (parent != null) parentName = parent.getName();
        }
        if (task.getType() == TaskType.CUSTOM) {
            String checklistDisplay = null;
            if (taskManager != null) {
                java.util.Set<Checklist> lists = taskManager.getCustomChecklists();
                for (Checklist c : lists) {
                    if (c.getId() != null && c.getId().equals(task.getChecklistId())) {
                        checklistDisplay = c.getName();
                        break;
                    }
                }
            }
            if (checklistDisplay != null && parentName != null) {
                breadcrumbText = checklistDisplay + " > " + parentName;
            } else if (parentName != null) {
                breadcrumbText = parentName;
            }
        } else {
            breadcrumbText = parentName;
        }
    }

    private void resolveWeekday(Task task) {
        String weekdayKey = task.getWeekday() != null ? task.getWeekday().toLowerCase() : null;
        this.weekdayAbbreviation = WEEKDAY_ABBREVIATIONS.get(weekdayKey);
        this.weekdayColor = WEEKDAY_COLORS.get(weekdayKey);
        this.isWeekdayTask = task.getWeekday() != null && WEEKDAY_ABBREVIATIONS.containsKey(weekdayKey);
    }

    private void buildToolTip(Task task) {
        // For top-level tasks show only the add-subtask affordance in the tooltip.
        if (task.getParentId() == null) {
            setToolTipText("<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>Add subtask</p></html>");
            return;
        }

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
        if (tip.length() > 0) {
            String html = "<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>" + tip.toString() + "</p></html>";
            setToolTipText(html);
        } else {
            setToolTipText(null);
        }
    }

    private void applySelectionStyles(JList<? extends Task> list, boolean isSelected) {
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            putClientProperty("selected", Boolean.TRUE);
        } else {
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
            putClientProperty("selected", Boolean.FALSE);
        }
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

        int checkboxX = UiLayout.CHECKBOX_X + (isSubtask ? subtaskIndent : 0);
        int checkboxY = getHeight() / 2 - UiLayout.CHECKBOX_SIZE / 2, checkboxSize = UiLayout.CHECKBOX_SIZE;

        drawCheckbox(g2, checkboxX, checkboxY, checkboxSize);

        int textY = getHeight() / 2 + 5;
        int availableWidth = getWidth() - textStartX - UiLayout.RIGHT_ICON_SPACE - 6;
        drawTaskText(g2, textStartX, textY, availableWidth);
        drawBreadcrumbIfNeeded(g2, textStartX, textY);
        drawDoneTimestampIfNeeded(g2, textStartX);
        drawAddSubtaskIfNeeded(g2);
        drawWeekdayCircleIfNeeded(g2);
        drawReminderIfNeeded(g2);
    }

    private void drawCheckbox(Graphics2D g2, int checkboxX, int checkboxY, int checkboxSize) {
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
            javax.swing.Icon ck = IconCache.getCheckmarkIcon();
            int imgW = ck.getIconWidth();
            int imgH = ck.getIconHeight();
            int imgX = checkboxX + (checkboxSize - imgW) / 2;
            int imgY = checkboxY + (checkboxSize - imgH) / 2;
            ck.paintIcon(this, g2, imgX, imgY);
        }
    }

    private void drawTaskText(Graphics2D g2, int textStartX, int textY, int availableWidth) {
        g2.setColor(getForeground());
        Font currentFont = getFont();
        FontMetrics fmMain = ensureFontSetup(g2, currentFont);
        String drawTaskName = taskName != null ? taskName : "";
        if (availableWidth > 12 && fmMain.stringWidth(drawTaskName) > availableWidth) {
            drawTaskName = computeDisplayedTaskName(drawTaskName, availableWidth, fmMain);
        }
        g2.drawString(drawTaskName, textStartX, textY);
    }

    private FontMetrics ensureFontSetup(Graphics2D g2, Font currentFont) {
        if (smallFont == null || !currentFont.equals(lastBaseFont)) {
            lastBaseFont = currentFont;
            smallFont = currentFont.deriveFont(Font.PLAIN, FontManager.SIZE_SMALL);
        }
        g2.setFont(currentFont);
        return FontMetricsCache.get(currentFont);
    }

    private String computeDisplayedTaskName(String drawTaskName, int availableWidth, FontMetrics fmMain) {
        // Try to use precomputed per-task cumulative widths if available
        Task backing = (taskManager != null && taskId != null) ? taskManager.getTaskById(taskId) : null;
        if (backing != null && backing.cachedDisplayFullName != null && backing.cachedDisplayFullName.equals(drawTaskName) && backing.cachedCumulativeCharWidthsMain != null) {
            int[] cum = backing.cachedCumulativeCharWidthsMain;
            int ellWidth = fmMain.charWidth('…');
            int allowed = Math.max(0, availableWidth - ellWidth);
            // binary search last index with cum[idx] <= allowed
            int lo = 0, hi = cum.length - 1, found = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (cum[mid] <= allowed) { found = mid; lo = mid + 1; } else { hi = mid - 1; }
            }
            if (found >= 0) {
                return drawTaskName.substring(0, found + 1) + "…";
            }
            return "…";
        } else {
            // Fallback: binary-search on length using FontMetrics stringWidth
            int lo = 0, hi = drawTaskName.length() - 1, best = -1;
            int ellWidth = fmMain.charWidth('…');
            int allowed = Math.max(0, availableWidth - ellWidth);
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                String sub = drawTaskName.substring(0, mid + 1);
                if (fmMain.stringWidth(sub) <= allowed) { best = mid; lo = mid + 1; } else { hi = mid - 1; }
            }
            if (best >= 0) return drawTaskName.substring(0, best + 1) + "…"; else return "…";
        }
    }

    private void drawBreadcrumbIfNeeded(Graphics2D g2, int textStartX, int textY) {
        if (breadcrumbText == null || breadcrumbText.isEmpty()) return;
        // Compute available width and position for breadcrumb component
        if (smallFont == null) smallFont = getFont().deriveFont(Font.PLAIN, FontManager.SIZE_SMALL);
        if (fmSmallCached == null || !smallFont.equals(fmSmallCached.getFont())) {
            fmSmallCached = g2.getFontMetrics(smallFont);
        }
        FontMetrics fmCrumb = fmSmallCached;
        int crumbWidth = fmCrumb.stringWidth(breadcrumbText) + 6;
        int crumbX = getWidth() - UiLayout.RIGHT_ICON_SPACE - 6 - crumbWidth;
        if (crumbX > textStartX + 10) {
            breadcrumbComponent.setFontToUse(smallFont);
            breadcrumbComponent.setText(breadcrumbText);
            breadcrumbComponent.setSize(crumbWidth, getHeight());
            // Paint the component into our Graphics2D at the computed location without creating a new Graphics
            java.awt.geom.AffineTransform at = g2.getTransform();
            try {
                g2.translate(crumbX, 0);
                breadcrumbComponent.paint(g2);
            } finally {
                g2.setTransform(at);
            }
        }
    }

    private void drawDoneTimestampIfNeeded(Graphics2D g2, int textStartX) {
        if (!(isChecked && doneDate != null && !doneDate.isEmpty())) return;
        g2.setColor(Color.BLACK);
        if (smallFont == null) smallFont = getFont().deriveFont(Font.PLAIN, FontManager.SIZE_SMALL);
        g2.setFont(smallFont);
        if (fmSmallCached == null || !smallFont.equals(fmSmallCached.getFont())) fmSmallCached = g2.getFontMetrics(smallFont);
        FontMetrics fmSmall = fmSmallCached;
        int availableWidthSmall = getWidth() - textStartX - UiLayout.RIGHT_ICON_SPACE - 6;
        String timeText = "✓ " + doneDate;
        if (availableWidthSmall > 12 && fmSmall.stringWidth(timeText) > availableWidthSmall) {
            int lt = timeText.length();
            while (lt > 0 && fmSmall.stringWidth(timeText.substring(0, lt) + "…") > availableWidthSmall) lt--;
            timeText = timeText.substring(0, lt) + "…";
        }
        g2.drawString(timeText, textStartX, getHeight() / 2 + 20);
    }

    private void drawWeekdayCircleIfNeeded(Graphics2D g2) {
        if (!isWeekdayTask) return;
        int circleSize = 30;
        int areaX = getWidth() - UiLayout.WEEKDAY_ICON_AREA;
        int circleX = areaX + (UiLayout.WEEKDAY_ICON_AREA - circleSize) / 2;
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

    private void drawReminderIfNeeded(Graphics2D g2) {
        if (taskReminder == null) return;
        ReminderClockIcon.State state = computeState(taskReminder);
        javax.swing.Icon icon = IconCache.getReminderClockIcon(taskReminder.getHour(), taskReminder.getMinute(), state, true);
        int iconW = icon.getIconWidth();
        int iconH = icon.getIconHeight();
        // reminder area is left of the weekday area
        int areaX = getWidth() - UiLayout.WEEKDAY_ICON_AREA - UiLayout.REMINDER_ICON_AREA;
        // center the icon (and its time text) within the reminder reserved area
        int iconX = areaX + Math.max(2, (UiLayout.REMINDER_ICON_AREA - iconW) / 2);
        int iconY = getHeight() / 2 - iconH / 2;
        icon.paintIcon(this, g2, iconX, iconY);
    }

    private void drawAddSubtaskIfNeeded(Graphics2D g2) {
        if (!showAddSubtaskIcon) return;
        // Show add-subtask icon for top-level tasks (parent == null)
        if (this.isSubtask) return; // only top-level
        if (this.taskId == null || this.taskManager == null) return;
        Task backing = taskManager.getTaskById(this.taskId);
        if (backing == null) return;
        if (backing.getParentId() != null) return; // not top-level

        javax.swing.Icon add = IconCache.getAddSubtaskIcon();
        int aw = add.getIconWidth();
        int ah = add.getIconHeight();
        // Place it to the left of the reminder area
        int areaX = getWidth() - UiLayout.WEEKDAY_ICON_AREA - UiLayout.REMINDER_ICON_AREA;
        int iconX = areaX - UiLayout.ADD_SUBTASK_OFFSET; // spacing
        if (iconX < 0) iconX = Math.max(2, getWidth() - UiLayout.RIGHT_ICON_SPACE - aw - 6);
        int iconY = getHeight() / 2 - ah / 2;
        add.paintIcon(this, g2, iconX, iconY);
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


