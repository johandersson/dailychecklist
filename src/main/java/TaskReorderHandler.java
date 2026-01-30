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

import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;

public class TaskReorderHandler {
    public static boolean performReorder(DefaultListModel<Task> listModel, TaskManager taskManager, String checklistName, List<Task> tasks, int dropIndex) {
        dropIndex = clampDropIndex(listModel, dropIndex);
        sortTasksByModelPosition(listModel, tasks);
        int removedBefore = countRemovedBefore(listModel, tasks, dropIndex);
        int adjustedIndex = dropIndex - removedBefore;

        String targetParentId = determineTargetParentId(listModel, dropIndex);
        if (wouldCreateGrandchildren(taskManager, targetParentId, tasks)) return false;

        final int finalDrop = Math.max(0, adjustedIndex);
        executeReorderOnEdt(listModel, taskManager, checklistName, tasks, finalDrop, targetParentId);
        return true;
    }

    private static int clampDropIndex(DefaultListModel<Task> model, int dropIndex) {
        int size = model.getSize();
        if (dropIndex > size) dropIndex = size;
        if (dropIndex < 0) dropIndex = 0;
        return dropIndex;
    }

    private static void sortTasksByModelPosition(DefaultListModel<Task> model, List<Task> tasks) {
        tasks.sort((t1, t2) -> {
            int idx1 = -1, idx2 = -1;
            for (int i = 0; i < model.getSize(); i++) {
                if (model.get(i).getId().equals(t1.getId())) idx1 = i;
                if (model.get(i).getId().equals(t2.getId())) idx2 = i;
            }
            return Integer.compare(idx1, idx2);
        });
    }

    private static int countRemovedBefore(DefaultListModel<Task> model, List<Task> tasks, int dropIndex) {
        int count = 0;
        for (Task task : tasks) {
            for (int i = 0; i < model.getSize(); i++) {
                if (model.get(i).getId().equals(task.getId())) {
                    if (i < dropIndex) count++;
                    break;
                }
            }
        }
        return count;
    }

    private static boolean wouldCreateGrandchildren(TaskManager taskManager, String targetParentId, List<Task> tasks) {
        if (targetParentId == null) return false;
        boolean targetHasSubs = !taskManager.getSubtasks(targetParentId).isEmpty();
        if (!targetHasSubs) return false;
        for (Task t : tasks) {
            java.util.List<Task> movedSubs = taskManager.getSubtasks(t.getId());
            if (movedSubs != null && !movedSubs.isEmpty()) return true;
        }
        return false;
    }

    private static void executeReorderOnEdt(DefaultListModel<Task> listModel, TaskManager taskManager, String checklistName, List<Task> tasks, int finalDropIndex, String targetParentId) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            DebugLog.d("performReorder (invokeLater): finalDropIndex=%d", finalDropIndex);

            List<Task> toPersistParentChange = new ArrayList<>();
            for (Task t : tasks) {
                Task authoritative = taskManager.getTaskById(t.getId());
                if (authoritative == null) authoritative = t;
                authoritative.setParentId(targetParentId);
                toPersistParentChange.add(authoritative);
            }

            // Update UI model immediately
            // Remove moved tasks from the model
            for (Task task : tasks) {
                for (int i = 0; i < listModel.getSize(); i++) {
                    if (listModel.get(i).getId().equals(task.getId())) { listModel.remove(i); break; }
                }
            }

            int adjusted = finalDropIndex;
            if (adjusted < 0) adjusted = 0;
            if (adjusted > listModel.getSize()) adjusted = listModel.getSize();

            for (int i = 0; i < tasks.size(); i++) {
                listModel.add(adjusted + i, tasks.get(i));
            }

            DebugLog.d("performReorder: adjustedDropIndex=%d resultingSize=%d", adjusted, listModel.getSize());

            // Do persistence in background to avoid blocking EDT
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    taskManager.updateTasks(toPersistParentChange);
                    TaskOrderPersister.persist(listModel, checklistName, taskManager);
                } catch (Exception e) {
                    DebugLog.d("Error during reorder persistence: %s", e.getMessage());
                }
            });
        });
    }

    // Persisting logic moved to TaskOrderPersister for clarity and testability.
    // Determine the nearest top-level parent for the given dropIndex inside the provided model.
    private static String determineTargetParentId(DefaultListModel<Task> listModel, int dropIndex) {
        int size = listModel.getSize();
        if (dropIndex < 0) dropIndex = 0;
        if (dropIndex > size) dropIndex = size;
        // If the insertion point is clearly between two top-level parents (or at ends),
        // treat this as a top-level insertion (no parent).
        if (size == 0) return null;
        if (dropIndex == 0) {
            Task next = listModel.getElementAt(0);
            if (next == null || next.getParentId() == null) return null;
        }
        if (dropIndex == size) {
            Task prev = listModel.getElementAt(size - 1);
            if (prev == null || prev.getParentId() == null) return null;
        }

        if (dropIndex > 0 && dropIndex < size) {
            Task prev = listModel.getElementAt(dropIndex - 1);
            Task next = listModel.getElementAt(dropIndex);
            if (prev != null && next != null && prev.getParentId() == null && next.getParentId() == null) {
                return null; // between two top-level parents
            }
        }

        // Search backward for a top-level parent
        for (int i = dropIndex - 1; i >= 0; i--) {
            Task t = listModel.get(i);
            if (t != null && t.getParentId() == null) return t.getId();
        }
        // If none, search forward
        for (int i = dropIndex; i < size; i++) {
            Task t = listModel.get(i);
            if (t != null && t.getParentId() == null) return t.getId();
        }
        // No enclosing parent found => top-level
        return null;
    }
}
