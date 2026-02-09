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
import java.util.Objects;

public final class ReminderTooltipProvider {
    private ReminderTooltipProvider() {}

    public static String getTooltip(Task t, int relX, int cellW, TaskManager taskManager) {
        if (t == null) return null;

        int infoStart = cellW - UiLayout.WEEKDAY_ICON_AREA - UiLayout.NOTE_ICON_AREA - UiLayout.REMINDER_ICON_AREA;
        int reminderStart = cellW - UiLayout.WEEKDAY_ICON_AREA - UiLayout.NOTE_ICON_AREA;
        int noteAreaStart = cellW - UiLayout.WEEKDAY_ICON_AREA - UiLayout.NOTE_ICON_AREA;
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

        // Note icon tooltip (left of weekday, right of reminder)
        if (relX >= noteAreaStart && relX < weekdayStart) {
            if (t.hasNote()) {
                return "<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>Task has a note (click to view/edit)</p></html>";
            }
            return null;
        }

        // Reminder tooltip (left of note icon area)
        if (relX >= infoStart && relX < noteAreaStart) {
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
