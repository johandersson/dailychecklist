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
import javax.swing.JList;

/**
 * Handles drop-on-item semantics for task moves.
 * Extracted from TaskTransferHandler to keep single-responsibility.
 */
public final class TaskDropHandler {
    private TaskDropHandler() {}

    public static boolean handleDropOnItem(TransferData transferData, JList<Task> targetList, int dropIndex, int insertOffset, TaskManager taskManager, Runnable updateAllPanels, String checklistName) {
        DebugLog.d("handleDropOnItem: targetIndex=%d insertOffset=%d sourceChecklist=%s tasks=%s", dropIndex, insertOffset, transferData == null ? "<null>" : transferData.sourceChecklistName, transferData == null ? "<null>" : transferData.tasks.toString());
        if (transferData == null || targetList == null || taskManager == null) return false;
        if (dropIndex < 0 || dropIndex >= targetList.getModel().getSize()) return false;
        Task modelTarget = targetList.getModel().getElementAt(dropIndex);
        if (modelTarget == null) return false;

        // Use authoritative target from TaskManager to avoid stale UI instances
        Task target = taskManager.getTaskById(modelTarget.getId());
        if (target == null) {
            target = modelTarget;
        }

        // Only allow dropping onto top-level tasks (no nested subtasks)
        if (target.getParentId() != null) {
            DebugLog.d("handleDropOnItem: rejected target is not top-level (parentId=%s)", target.getParentId());
            return false;
        }

        if (wouldCreateSelfParenting(transferData, target)) return false;

        // Prevent creating grandchildren: do not allow moving a task that has subtasks
        // into a target that already has subtasks (would create nested subtasks).
        if (wouldCreateGrandchildren(transferData, target, taskManager)) {
            DebugLog.d("handleDropOnItem: reject - would create grandchildren target=%s", target.getId());
            return false;
        }

        List<Task> toPersist = prepareMovedTasks(transferData, taskManager, target);

        // Prefer operating on the mutable UI model and then persist ordering centrally.
        if (targetList.getModel() instanceof javax.swing.DefaultListModel) {
            javax.swing.DefaultListModel<Task> model = (javax.swing.DefaultListModel<Task>) targetList.getModel();
            return persistUsingModel(model, toPersist, target, insertOffset, taskManager, updateAllPanels, checklistName);
        }

        // Fallback: operate directly on global task list when model isn't mutable
        return persistUsingFallback(toPersist, target, insertOffset, taskManager, updateAllPanels);
    }

    private static boolean persistUsingModel(javax.swing.DefaultListModel<Task> model, List<Task> toPersist, Task target, int insertOffset, TaskManager taskManager, Runnable updateAllPanels, String checklistName) {
        // Determine insertion index based on the current model BEFORE removals,
        // then remove moved items and adjust the insertion index for those removals.
        int parentModelIndexOrig = findModelIndex(model, target.getId());
        int modelInsertIndexOrig = computeModelInsertIndex(model, parentModelIndexOrig, insertOffset, target);

        // Count how many of the moved items are located before the original insert index
        int removedBefore = 0;
        for (Task t : toPersist) {
            for (int i = 0; i < model.getSize(); i++) {
                if (model.get(i).getId().equals(t.getId())) {
                    if (i < modelInsertIndexOrig) removedBefore++;
                    break;
                }
            }
        }

        DebugLog.d("persistUsingModel: parentModelIndexOrig=%d modelInsertIndexOrig=%d removedBefore=%d toPersist=%s", parentModelIndexOrig, modelInsertIndexOrig, removedBefore, toPersist.toString());

        // Remove moved tasks from the UI model
        removeMovedFromModel(model, toPersist);

        // Adjust insert index after removals
        int modelInsertIndex = modelInsertIndexOrig - removedBefore;
        if (modelInsertIndex < 0) modelInsertIndex = 0;
        if (modelInsertIndex > model.getSize()) modelInsertIndex = model.getSize();

        // Insert into model
        for (int i = 0; i < toPersist.size(); i++) {
            model.add(modelInsertIndex + i, toPersist.get(i));
        }

        // Persist parent/type/checklist changes first
        taskManager.updateTasks(toPersist);

        // Persist full checklist ordering using the model
        TaskOrderPersister.persist(model, checklistName, taskManager);

        if (updateAllPanels != null) updateAllPanels.run();
        return true;
    }

    private static boolean persistUsingFallback(List<Task> toPersist, Task target, int insertOffset, TaskManager taskManager, Runnable updateAllPanels) {
        java.util.List<Task> all = new java.util.ArrayList<>(taskManager.getAllTasks());
        for (Task t : toPersist) all.removeIf(x -> x.getId().equals(t.getId()));
        int targetIdx = -1;
        for (int i = 0; i < all.size(); i++) if (all.get(i).getId().equals(target.getId())) { targetIdx = i; break; }
        int insertAt;
        if (targetIdx == -1) insertAt = all.size();
        else if (insertOffset >= 0) insertAt = Math.max(0, Math.min(all.size(), targetIdx + 1 + insertOffset));
        else {
            insertAt = targetIdx + 1;
            while (insertAt < all.size() && target.getId().equals(all.get(insertAt).getParentId())) insertAt++;
        }
        DebugLog.d("persistUsingFallback: insertAt=%d toPersist=%s", insertAt, toPersist.toString());
        all.addAll(insertAt, toPersist);
        taskManager.setTasks(all);
        if (updateAllPanels != null) updateAllPanels.run();
        return true;
    }

    private static void removeMovedFromModel(javax.swing.DefaultListModel<Task> model, List<Task> toPersist) {
        for (Task t : toPersist) {
            for (int i = 0; i < model.getSize(); i++) {
                if (model.get(i).getId().equals(t.getId())) { model.remove(i); break; }
            }
        }
    }

    private static int findModelIndex(javax.swing.DefaultListModel<Task> model, String id) {
        for (int i = 0; i < model.getSize(); i++) if (model.get(i).getId().equals(id)) return i;
        return -1;
    }

    private static int computeModelInsertIndex(javax.swing.DefaultListModel<Task> model, int parentModelIndex, int insertOffset, Task target) {
        if (parentModelIndex == -1) return model.getSize();
        if (insertOffset >= 0) {
            int idx = parentModelIndex + 1 + insertOffset;
            if (idx < 0) idx = 0;
            if (idx > model.getSize()) idx = model.getSize();
            return idx;
        }
        int idx = parentModelIndex + 1;
        while (idx < model.getSize() && target.getId().equals(model.get(idx).getParentId())) idx++;
        return idx;
    }

    // removed isValidTarget helper - validation is done with authoritative Task in caller

    private static boolean wouldCreateSelfParenting(TransferData transferData, Task target) {
        for (Task moved : transferData.tasks) {
            if (moved.getId().equals(target.getId())) return true;
        }
        return false;
    }

    private static List<Task> prepareMovedTasks(TransferData transferData, TaskManager taskManager, Task target) {
        List<Task> toPersist = new ArrayList<>();
        for (Task t : transferData.tasks) {
            // Use authoritative task instance to avoid modifying stale copies
            Task authoritative = taskManager.getTaskById(t.getId());
            if (authoritative == null) {
                authoritative = t;
            }
            adjustChecklistAndTypeIfNeeded(authoritative, transferData.sourceChecklistName, target);
            authoritative.setParentId(target.getId());
            toPersist.add(authoritative);
        }
        return toPersist;
    }

    private static boolean wouldCreateGrandchildren(TransferData transferData, Task target, TaskManager taskManager) {
        if (transferData == null || target == null || taskManager == null) return false;
        java.util.List<Task> targetSubs = taskManager.getSubtasks(target.getId());
        if (targetSubs == null || targetSubs.isEmpty()) return false;
        for (Task moved : transferData.tasks) {
            java.util.List<Task> movedSubs = taskManager.getSubtasks(moved.getId());
            if (movedSubs != null && !movedSubs.isEmpty()) return true;
        }
        return false;
    }

    private static void adjustChecklistAndTypeIfNeeded(Task t, String sourceChecklistName, Task target) {
        // If moving within the same checklist/list, nothing to do
        if (java.util.Objects.equals(sourceChecklistName, checklistNameFromTask(target))) return;

        boolean sourceIsDaily = sourceChecklistName != null && ("MORNING".equals(sourceChecklistName) || "EVENING".equals(sourceChecklistName));
        boolean targetIsDaily = target != null && (target.getType() == TaskType.MORNING || target.getType() == TaskType.EVENING);

        if (targetIsDaily && !sourceIsDaily) {
            t.setChecklistId(null);
            t.setType(target.getType());
        } else if (!targetIsDaily && sourceIsDaily) {
            // target is a custom checklist: use target's checklist id
            if (target.getChecklistId() != null) {
                t.setChecklistId(target.getChecklistId());
                t.setType(TaskType.CUSTOM);
            }
        } else if (sourceIsDaily && targetIsDaily && target.getType() != null && target.getType() != t.getType()) {
            // switch between morning/evening
            t.setChecklistId(null);
            t.setType(target.getType());
        } else if (!sourceIsDaily && !targetIsDaily) {
            // custom -> custom: set to target's checklist id
            if (target.getChecklistId() != null) {
                t.setChecklistId(target.getChecklistId());
            }
        }
    }

    private static String checklistNameFromTask(Task t) {
        if (t == null) return null;
        if (t.getType() == TaskType.MORNING) return "MORNING";
        if (t.getType() == TaskType.EVENING) return "EVENING";
        // For custom tasks, we cannot reliably derive the checklist name here; return null
        return null;
    }
}
