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
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import javax.swing.DefaultListModel;

public class TaskUpdater {
    public void updateTasks(List<Task> allTasks, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel, boolean showWeekday) {
        updateTasks(allTasks, morningListModel, eveningListModel, showWeekday, getCurrentWeekday());
    }

    public void updateTasks(List<Task> allTasks, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel, boolean showWeekday, String currentWeekday) {
        morningListModel.clear();
        eveningListModel.clear();
        for (Task task : allTasks) {
            boolean show = task.getWeekday() == null || (showWeekday && task.getWeekday().equals(currentWeekday));
            if (show && task.getType() != TaskType.CUSTOM) {
                if (task.getType() == TaskType.MORNING) {
                    morningListModel.addElement(task);
                } else if (task.getType() == TaskType.EVENING) {
                    eveningListModel.addElement(task);
                }
            }
        }
    }

    private String getCurrentWeekday() {
        return LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
    }
}