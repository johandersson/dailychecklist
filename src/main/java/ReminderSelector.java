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

public final class ReminderSelector {
    private ReminderSelector() {}

    public static Reminder selectReminderForType(TaskManager taskManager, String title) {
        List<Reminder> reminders = taskManager.getReminders();
        // Do not allow checklist-level reminders for the built-in daily lists
        if (!isDailyChecklistTitle(title)) {
            Reminder checklistLevel = findChecklistLevelReminder(reminders, title);
            if (checklistLevel != null) return checklistLevel;
        }
        return findEarliestTaskReminderForType(taskManager, reminders, title);
    }

    private static boolean isDailyChecklistTitle(String title) {
        if (title == null) return false;
        return title.equalsIgnoreCase("Morning") || title.equalsIgnoreCase("Evening");
    }

    private static Reminder findChecklistLevelReminder(List<Reminder> reminders, String title) {
        for (Reminder r : reminders) {
            if (r.getTaskId() == null && title.equalsIgnoreCase(r.getChecklistName())) {
                return r;
            }
        }
        return null;
    }

    private static Reminder findEarliestTaskReminderForType(TaskManager taskManager, List<Reminder> reminders, String title) {
        java.time.LocalDateTime best = null;
        Reminder display = null;
        List<Task> allTasks = taskManager.getAllTasks();
        for (Reminder r : reminders) {
            Task t = taskForReminder(r, allTasks);
            if (t == null) continue;
            if (!reminderMatchesType(t, title)) continue;
            java.time.LocalDateTime rt = reminderDateTime(r);
            if (best == null || rt.isBefore(best)) {
                best = rt;
                display = r;
            }
        }
        return display;
    }

    private static Task taskForReminder(Reminder r, List<Task> allTasks) {
        if (r.getTaskId() == null) return null;
        return allTasks.stream().filter(x -> r.getTaskId().equals(x.getId())).findFirst().orElse(null);
    }

    private static boolean reminderMatchesType(Task t, String title) {
        return (title.equalsIgnoreCase("Morning") && t.getType() == TaskType.MORNING)
                || (title.equalsIgnoreCase("Evening") && t.getType() == TaskType.EVENING);
    }

    public static java.time.LocalDateTime reminderDateTime(Reminder r) {
        return java.time.LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
    }
}
