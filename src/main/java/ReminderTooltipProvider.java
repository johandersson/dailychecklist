/*
 * Daily Checklist
 * Copyright (C) 2025 Johan Andersson
 */
import java.util.Objects;

public final class ReminderTooltipProvider {
    private ReminderTooltipProvider() {}

    public static String getTooltip(Task t, int relX, int cellW, TaskManager taskManager) {
        if (t == null) return null;

        int reminderStart = cellW - UiLayout.WEEKDAY_ICON_AREA - UiLayout.REMINDER_ICON_AREA;
        int weekdayStart = cellW - UiLayout.WEEKDAY_ICON_AREA;

        // Weekday tooltip (far right circle)
        if (relX >= weekdayStart) {
            String wd = t.getWeekday();
            if (wd != null && !wd.isEmpty()) {
                String nice = wd.substring(0,1).toUpperCase() + wd.substring(1).toLowerCase();
                return "<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>" + nice + " daily task</p></html>";
            }
            return null;
        }

        // Reminder tooltip (left of weekday area)
        if (relX >= reminderStart && relX < weekdayStart) {
            Reminder found = null;
            if (taskManager != null && t != null && t.getId() != null) {
                for (Reminder r : taskManager.getReminders()) {
                    if (Objects.equals(t.getId(), r.getTaskId())) { found = r; break; }
                }
            }
            if (found != null) {
                String txt = String.format("Reminder: %04d-%02d-%02d %02d:%02d", found.getYear(), found.getMonth(), found.getDay(), found.getHour(), found.getMinute());
                return "<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>" + txt + "</p></html>";
            }
            return null;
        }

        return null;
    }
}
