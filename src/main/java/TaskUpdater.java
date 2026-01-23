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
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.DefaultListModel;

public class TaskUpdater {
    public void updateTasks(List<Task> allTasks, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel) {
        updateTasks(allTasks, morningListModel, eveningListModel, true);
    }

    public void updateTasks(List<Task> allTasks, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel, boolean showAllWeekdaySpecificTasks) {
        morningListModel.clear();
        eveningListModel.clear();
        String currentWeekday = LocalDateTime.now().getDayOfWeek().toString().toLowerCase();
        for (Task task : allTasks) {
            boolean show;
            if (task.getWeekday() == null) {
                show = true; // Non-weekday-specific tasks always show
            } else {
                // If "show all" is enabled, show all weekday-specific tasks; otherwise show only today's weekday
                show = showAllWeekdaySpecificTasks || task.getWeekday().toLowerCase().equals(currentWeekday);
            }
            if (show && task.getType() != TaskType.CUSTOM) {
                if (task.getType() == TaskType.MORNING) {
                    morningListModel.addElement(task);
                } else if (task.getType() == TaskType.EVENING) {
                    eveningListModel.addElement(task);
                }
            }
        }
    }
}