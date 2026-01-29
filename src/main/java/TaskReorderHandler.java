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
        // Ensure drop index is within valid bounds
        int originalSize = listModel.getSize();
        if (dropIndex > originalSize) {
            DebugLog.d("performReorder: checklist=%s dropIndex=%d tasks=%s", checklistName, dropIndex, tasks.toString());
            dropIndex = originalSize;
        }
        if (dropIndex < 0) {
            dropIndex = 0;
        }

        // Sort tasks by their current position in the list for proper insertion order
        tasks.sort((t1, t2) -> {
            int idx1 = -1, idx2 = -1;
            for (int i = 0; i < listModel.getSize(); i++) {
                if (listModel.get(i).getId().equals(t1.getId())) idx1 = i;
                if (listModel.get(i).getId().equals(t2.getId())) idx2 = i;
            }
            return Integer.compare(idx1, idx2);
        });

        // Calculate how many items will be removed before the drop position
        int itemsRemovedBeforeDrop = 0;
        for (Task task : tasks) {
            for (int i = 0; i < listModel.getSize(); i++) {
                if (listModel.get(i).getId().equals(task.getId())) {
                    if (i < dropIndex) {
                        itemsRemovedBeforeDrop++;
                    }
                    break;
                }
            }
        }

        // Adjust drop index for removals that occur before it
        dropIndex -= itemsRemovedBeforeDrop;

        // Determine target parent based on drop location in the model so we can
        // update moved tasks' parentId before persisting the new order.
        final int finalDropIndex = dropIndex;
        final String targetParentId = determineTargetParentId(listModel, dropIndex);

        javax.swing.SwingUtilities.invokeLater(() -> {
            DebugLog.d("performReorder (invokeLater): finalDropIndex=%d", finalDropIndex);
            // Update parentId for moved tasks to reflect intended placement
            List<Task> toPersistParentChange = new ArrayList<>();
            for (Task t : tasks) {
                Task authoritative = taskManager.getTaskById(t.getId());
                if (authoritative == null) authoritative = t;
                authoritative.setParentId(targetParentId);
                toPersistParentChange.add(authoritative);
            }
            // Persist parent changes first so TaskOrderPersister sees authoritative state
            taskManager.updateTasks(toPersistParentChange);
            // Remove all tasks from their current positions
            for (Task task : tasks) {
                for (int i = 0; i < listModel.getSize(); i++) {
                    if (listModel.get(i).getId().equals(task.getId())) {
                        listModel.remove(i);
                        break;
                    }
                }
            }

            // Ensure drop index is still valid after all removals
            int adjustedDropIndex = finalDropIndex;
            if (adjustedDropIndex < 0) {
                adjustedDropIndex = 0;
            }
            if (adjustedDropIndex > listModel.getSize()) {
                adjustedDropIndex = listModel.getSize();
            }

            // Insert all tasks at the drop position
            for (int i = 0; i < tasks.size(); i++) {
                listModel.add(adjustedDropIndex + i, tasks.get(i));
            }

            DebugLog.d("performReorder: adjustedDropIndex=%d resultingSize=%d", adjustedDropIndex, listModel.getSize());
            }

            // Persist the new order in the underlying data
            TaskOrderPersister.persist(listModel, checklistName, taskManager);
        });

        return true;
    }

    // Persisting logic moved to TaskOrderPersister for clarity and testability.
    // Determine the nearest top-level parent for the given dropIndex inside the provided model.
    private static String determineTargetParentId(DefaultListModel<Task> listModel, int dropIndex) {
        int size = listModel.getSize();
        if (dropIndex < 0) dropIndex = 0;
        if (dropIndex > size) dropIndex = size;

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
