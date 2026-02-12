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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.DefaultListModel;

public class TaskUpdater {
    public void updateTasks(List<Task> allTasks, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel) {
        updateTasks(allTasks, morningListModel, eveningListModel, true, null);
    }

    public void updateTasks(List<Task> allTasks, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel, boolean showAllWeekdaySpecificTasks, TaskManager taskManager) {
        String currentWeekday = LocalDateTime.now().getDayOfWeek().toString().toLowerCase();
        Collected c = collectVisible(allTasks, showAllWeekdaySpecificTasks, currentWeekday);
        List<Task> desiredMorning = buildDesiredList(c.morningParents, c.headingByParent, taskManager, showAllWeekdaySpecificTasks, currentWeekday);
        List<Task> desiredEvening = buildDesiredList(c.eveningParents, c.headingByParent, taskManager, showAllWeekdaySpecificTasks, currentWeekday);

        // Precompute display data for renderer (no checklist info for daily lists)
        DisplayPrecomputer.precomputeForList(desiredMorning, null, false);
        DisplayPrecomputer.precomputeForList(desiredEvening, null, false);
        syncModel(morningListModel, desiredMorning);
        syncModel(eveningListModel, desiredEvening);
    }

    private static class Collected {
        List<Task> morningParents = new ArrayList<>();
        List<Task> eveningParents = new ArrayList<>();
        java.util.Map<String, List<Task>> subtasksByParent = new java.util.HashMap<>();
        java.util.Map<String, Task> headingByParent = new java.util.HashMap<>();
    }

    private Collected collectVisible(List<Task> allTasks, boolean showAllWeekdaySpecificTasks, String currentWeekday) {
        Collected c = new Collected();
        for (Task task : allTasks) {
            boolean show;
            if (task.getWeekday() == null) show = true;
            else show = showAllWeekdaySpecificTasks || task.getWeekday().toLowerCase().equals(currentWeekday);
            if (!show) continue;
            if (task.getType() == TaskType.HEADING) {
                if (task.getParentId() != null) c.headingByParent.put(task.getParentId(), task);
                continue;
            }
            if (task.getType() != TaskType.CUSTOM) {
                if (task.getParentId() == null) {
                    if (task.getType() == TaskType.MORNING) c.morningParents.add(task);
                    else if (task.getType() == TaskType.EVENING) c.eveningParents.add(task);
                } else {
                    c.subtasksByParent.computeIfAbsent(task.getParentId(), k -> new ArrayList<>()).add(task);
                }
            }
        }
        return c;
    }

    private List<Task> buildDesiredList(List<Task> parents, java.util.Map<String, Task> headingByParent, TaskManager taskManager, boolean showAllWeekdaySpecificTasks, String currentWeekday) {
        List<Task> desired = new ArrayList<>();
        for (Task parent : parents) {
            Task heading = headingByParent.get(parent.getId());
            if (heading != null) desired.add(heading);
            desired.add(parent);
            if (taskManager != null) {
                java.util.List<Task> subs = taskManager.getSubtasksSorted(parent.getId());
                if (subs != null && !subs.isEmpty()) {
                    for (Task s : subs) {
                        boolean showSub = s.getWeekday() == null || showAllWeekdaySpecificTasks || s.getWeekday().toLowerCase().equals(currentWeekday);
                        if (showSub) desired.add(s);
                    }
                }
            }
        }
        return desired;
    }

    /**
     * Synchronize a DefaultListModel's contents with the desired list, performing minimal
     * add/remove/move operations. Uses equality via Objects.equals.
     */
    public static <T> void syncModel(DefaultListModel<T> model, List<T> desired) {
        // First, remove any accidental duplicate entries already present in the model
        // (duplicates are determined by equality, e.g. same Task id). This ensures
        // the subsequent sync logic operates on at-most-one instance per logical
        // element and prevents duplicate items lingering after merges.
        java.util.Set<T> seen = new java.util.HashSet<>();
        for (int i = 0; i < model.getSize(); ) {
            T current = model.getElementAt(i);
            if (seen.contains(current)) {
                model.removeElementAt(i);
            } else {
                seen.add(current);
                i++;
            }
        }

        // Create fast lookup set for O(1) containment checks - eliminates O(n²) nested loops
        Set<T> desiredSet = new HashSet<>(desired);
        
        // Remove items that are not desired - O(n) instead of O(n²)
        for (int i = model.getSize() - 1; i >= 0; i--) {
            T current = model.getElementAt(i);
            if (!desiredSet.contains(current)) {
                model.removeElementAt(i);
            }
        }

        // Create position map for O(1) lookups of existing elements - eliminates O(n²) position tracking
        Map<T, Integer> currentPositions = new HashMap<>();
        for (int i = 0; i < model.getSize(); i++) {
            currentPositions.put(model.getElementAt(i), i);
        }
        
        // Ensure order and add missing items
        for (int i = 0; i < desired.size(); i++) {
            T want = desired.get(i);
            if (i < model.getSize()) {
                T have = model.getElementAt(i);
                if (Objects.equals(have, want)) {
                    // Replace the instance with the freshly parsed one to avoid stale state
                    if (have != want) {
                        model.setElementAt(want, i);
                    }
                    continue;
                }
                // If the desired element exists later in the model, remove it from there and insert the fresh instance here
                Integer foundIndex = currentPositions.get(want);
                if (foundIndex != null && foundIndex > i) {
                    model.removeElementAt(foundIndex);
                    model.add(i, want);
                    // Update positions for elements that shifted due to the removal
                    currentPositions.remove(want);
                    for (Map.Entry<T, Integer> entry : currentPositions.entrySet()) {
                        if (entry.getValue() > foundIndex) {
                            entry.setValue(entry.getValue() - 1);
                        }
                    }
                    currentPositions.put(want, i);
                } else {
                    model.add(i, want);
                    // Update positions for elements that shifted due to the insertion
                    for (Map.Entry<T, Integer> entry : currentPositions.entrySet()) {
                        if (entry.getValue() >= i) {
                            entry.setValue(entry.getValue() + 1);
                        }
                    }
                    currentPositions.put(want, i);
                }
            } else {
                model.add(i, want);
                currentPositions.put(want, i);
            }
        }
    }
}