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

    private transient TaskManager taskManager;
    private final ReminderClockIcon clockIcon = new ReminderClockIcon();

    public ChecklistCellRenderer(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    protected Icon getIconForValue(String checklistName) {
        if (checklistName != null && hasReminders(checklistName)) {
            return clockIcon;
        }
        return null;
    }

    @Override
    protected String getTextForValue(String checklistName) {
        return checklistName;
    }

    private boolean hasReminders(String checklistName) {
        if (taskManager == null || checklistName == null) {
            return false;
        }
        List<Reminder> reminders = taskManager.getReminders();
        return reminders.stream()
            .anyMatch(reminder -> java.util.Objects.equals(reminder.getChecklistName(), checklistName));
    }
}