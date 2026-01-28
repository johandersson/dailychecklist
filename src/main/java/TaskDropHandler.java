import java.util.ArrayList;
import java.util.List;
import javax.swing.JList;

/**
 * Handles drop-on-item semantics for task moves.
 * Extracted from TaskTransferHandler to keep single-responsibility.
 */
public final class TaskDropHandler {
    private TaskDropHandler() {}

    public static boolean handleDropOnItem(TransferData transferData, JList<Task> targetList, int dropIndex, TaskManager taskManager, String checklistName, Runnable updateAllPanels) {
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

        List<Task> toPersist = prepareMovedTasks(transferData, checklistName, taskManager, target);
        // Persist atomically and refresh UI
        taskManager.updateTasks(toPersist);
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

    private static List<Task> prepareMovedTasks(TransferData transferData, String checklistName, TaskManager taskManager, Task target) {
        List<Task> toPersist = new ArrayList<>();
        for (Task t : transferData.tasks) {
            // Use authoritative task instance to avoid modifying stale copies
            Task authoritative = taskManager.getTaskById(t.getId());
            if (authoritative == null) {
                authoritative = t;
            }
            adjustChecklistAndTypeIfNeeded(authoritative, transferData.sourceChecklistName, taskManager, target);
            authoritative.setParentId(target.getId());
            toPersist.add(authoritative);
        }
        return toPersist;
    }

    private static void adjustChecklistAndTypeIfNeeded(Task t, String sourceChecklistName, TaskManager taskManager, Task target) {
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
