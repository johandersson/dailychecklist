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
import java.util.List;
import javax.swing.Icon;

/**
 * Cell renderer for checklist list items that displays a clock icon for checklists with reminders.
 */
@SuppressWarnings("serial")
public class ChecklistCellRenderer extends IconListCellRenderer<String> {
    private static final long serialVersionUID = 1L;

    private final transient TaskManager taskManager;

    public ChecklistCellRenderer(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    protected Icon getIconForValue(String checklistName) {
        if (checklistName != null) {
            Reminder nearest = nearestReminderForChecklist(checklistName);
            if (nearest != null) {
                ReminderClockIcon.State state = computeState(nearest);
                return new ReminderClockIcon(nearest.getHour(), nearest.getMinute(), state);
            }
        }
        return null;
    }

    @Override
    protected String getTextForValue(String checklistName) {
        return checklistName;
    }

    

    private Reminder nearestReminderForChecklist(String checklistName) {
        if (taskManager == null) return null;
        List<Reminder> reminders = taskManager.getReminders();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Reminder best = null;
        long bestDiff = Long.MAX_VALUE;
        for (Reminder r : reminders) {
            if (!java.util.Objects.equals(r.getChecklistName(), checklistName)) continue;
            java.time.LocalDateTime dt = java.time.LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
            long diff = Math.abs(java.time.Duration.between(now, dt).toMinutes());
            if (diff < bestDiff) {
                bestDiff = diff;
                best = r;
            }
        }
        return best;
    }

    private ReminderClockIcon.State computeState(Reminder r) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime dt = java.time.LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
        if (dt.isBefore(now)) return ReminderClockIcon.State.OVERDUE;
        long minutes = java.time.Duration.between(now, dt).toMinutes();
        if (minutes <= 60) return ReminderClockIcon.State.DUE_SOON;
        return ReminderClockIcon.State.FUTURE;
    }
}