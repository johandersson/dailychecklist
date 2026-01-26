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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.DefaultListModel;

public class TaskUpdater {
    public void updateTasks(List<Task> allTasks, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel) {
        updateTasks(allTasks, morningListModel, eveningListModel, true);
    }

    public void updateTasks(List<Task> allTasks, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel, boolean showAllWeekdaySpecificTasks) {
        String currentWeekday = LocalDateTime.now().getDayOfWeek().toString().toLowerCase();

        List<Task> desiredMorning = new ArrayList<>();
        List<Task> desiredEvening = new ArrayList<>();
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
                    desiredMorning.add(task);
                } else if (task.getType() == TaskType.EVENING) {
                    desiredEvening.add(task);
                }
            }
        }

        // Sync models incrementally to avoid clearing and repopulating (reduces O(N) churn)
        syncModel(morningListModel, desiredMorning);
        syncModel(eveningListModel, desiredEvening);
    }

    /**
     * Synchronize a DefaultListModel's contents with the desired list, performing minimal
     * add/remove/move operations. Uses equality via Objects.equals.
     */
    public static <T> void syncModel(DefaultListModel<T> model, List<T> desired) {
        // Remove items that are not desired
        for (int i = model.getSize() - 1; i >= 0; i--) {
            T current = model.getElementAt(i);
            boolean found = false;
            for (T d : desired) {
                if (Objects.equals(current, d)) { found = true; break; }
            }
            if (!found) {
                model.removeElementAt(i);
            }
        }

        // Ensure order and add missing items
        for (int i = 0; i < desired.size(); i++) {
            T want = desired.get(i);
            if (i < model.getSize()) {
                T have = model.getElementAt(i);
                if (Objects.equals(have, want)) continue; // already correct
                // If the desired element exists later in the model, remove it from there and insert here
                int foundIndex = -1;
                for (int j = i + 1; j < model.getSize(); j++) {
                    if (Objects.equals(model.getElementAt(j), want)) { foundIndex = j; break; }
                }
                if (foundIndex != -1) {
                    T moved = model.getElementAt(foundIndex);
                    model.removeElementAt(foundIndex);
                    model.add(i, moved);
                } else {
                    model.add(i, want);
                }
            } else {
                model.add(i, want);
            }
        }
    }
}