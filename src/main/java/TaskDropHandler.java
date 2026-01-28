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
        Task target = targetList.getModel().getElementAt(dropIndex);
        if (!isValidTarget(target)) return false;

        if (wouldCreateSelfParenting(transferData, target)) return false;

        List<Task> toPersist = prepareMovedTasks(transferData, checklistName, taskManager, target);
        // Persist atomically and refresh UI
        taskManager.updateTasks(toPersist);
        if (updateAllPanels != null) updateAllPanels.run();
        return true;
    }

    private static boolean isValidTarget(Task target) {
        return target != null && target.getParentId() == null;
    }

    private static boolean wouldCreateSelfParenting(TransferData transferData, Task target) {
        for (Task moved : transferData.tasks) {
            if (moved.getId().equals(target.getId())) return true;
        }
        return false;
    }

    private static List<Task> prepareMovedTasks(TransferData transferData, String checklistName, TaskManager taskManager, Task target) {
        List<Task> toPersist = new ArrayList<>();
        for (Task t : transferData.tasks) {
            adjustChecklistAndTypeIfNeeded(t, transferData.sourceChecklistName, checklistName, taskManager);
            t.setParentId(target.getId());
            toPersist.add(t);
        }
        return toPersist;
    }

    private static void adjustChecklistAndTypeIfNeeded(Task t, String sourceChecklistName, String targetChecklistName, TaskManager taskManager) {
        if (java.util.Objects.equals(sourceChecklistName, targetChecklistName)) return;

        boolean sourceIsDaily = sourceChecklistName != null && ("MORNING".equals(sourceChecklistName) || "EVENING".equals(sourceChecklistName));
        boolean targetIsDaily = "MORNING".equals(targetChecklistName) || "EVENING".equals(targetChecklistName);

        if (targetIsDaily && !sourceIsDaily) {
            t.setChecklistId(null);
            t.setType("MORNING".equals(targetChecklistName) ? TaskType.MORNING : TaskType.EVENING);
        } else if (!targetIsDaily && sourceIsDaily) {
            Checklist targetChecklist = taskManager.getCustomChecklists().stream()
                .filter(c -> targetChecklistName.equals(c.getName()))
                .findFirst()
                .orElse(null);
            if (targetChecklist != null) {
                t.setChecklistId(targetChecklist.getId());
                t.setType(TaskType.CUSTOM);
            }
        } else if (sourceIsDaily && targetIsDaily && !targetChecklistName.equals(sourceChecklistName)) {
            t.setChecklistId(null);
            t.setType("MORNING".equals(targetChecklistName) ? TaskType.MORNING : TaskType.EVENING);
        } else if (!sourceIsDaily && !targetIsDaily) {
            Checklist targetChecklist = taskManager.getCustomChecklists().stream()
                .filter(c -> targetChecklistName.equals(c.getName()))
                .findFirst()
                .orElse(null);
            if (targetChecklist != null) {
                t.setChecklistId(targetChecklist.getId());
            }
        }
    }
}
