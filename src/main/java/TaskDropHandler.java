import java.util.ArrayList;
import java.util.List;
import javax.swing.JList;

/**
 * Handles drop-on-item semantics for task moves.
 * Extracted from TaskTransferHandler to keep single-responsibility.
 */
public final class TaskDropHandler {
    private TaskDropHandler() {}

    public static boolean handleDropOnItem(TransferData transferData, JList<Task> targetList, int dropIndex, int insertOffset, TaskManager taskManager, Runnable updateAllPanels) {
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
        if (target.getParentId() != null) return false;

        if (wouldCreateSelfParenting(transferData, target)) return false;

        List<Task> toPersist = prepareMovedTasks(transferData, taskManager, target);

        // Persist field changes for moved tasks
        // Additionally, update the global task order so the moved tasks appear
        // directly after the target and its existing subtask block (append into parent's subtask block).
        java.util.List<Task> all = new java.util.ArrayList<>(taskManager.getAllTasks());

        // Remove moved tasks from the global list if present
        for (Task t : toPersist) {
            all.removeIf(x -> x.getId().equals(t.getId()));
        }

        // Find the authoritative target index
        int targetIdx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(target.getId())) { targetIdx = i; break; }
        }

        int insertAt;
        if (targetIdx == -1) {
            // If target not found, append at end
            insertAt = all.size();
        } else if (insertOffset >= 0) {
            // Insert at a specific position inside the parent's subtask block
            insertAt = targetIdx + 1 + insertOffset;
            if (insertAt < 0) insertAt = 0;
            if (insertAt > all.size()) insertAt = all.size();
        } else {
            // Insert after the parent's existing subtask block (default behavior)
            insertAt = targetIdx + 1;
            while (insertAt < all.size() && target.getId().equals(all.get(insertAt).getParentId())) {
                insertAt++;
            }
        }

        // Insert moved tasks at computed position
        all.addAll(insertAt, toPersist);

        // Persist both structural (parentId) and order changes atomically
        taskManager.setTasks(all);

        if (updateAllPanels != null) updateAllPanels.run();
        return true;
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
