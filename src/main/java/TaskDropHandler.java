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
        if (target == null) return false;
        // Only allow making a task a subtask of a parent (parent must not itself be a subtask)
        if (target.getParentId() != null) return false;

        // Prevent making a task a subtask of itself
        for (Task moved : transferData.tasks) {
            if (moved.getId().equals(target.getId())) {
                return false;
            }
        }

        List<Task> toPersist = new ArrayList<>();
        for (Task t : transferData.tasks) {
            // Adjust checklist/type if moving across checklists
            if (!java.util.Objects.equals(transferData.sourceChecklistName, checklistName)) {
                boolean sourceIsDaily = transferData.sourceChecklistName != null && ("MORNING".equals(transferData.sourceChecklistName) || "EVENING".equals(transferData.sourceChecklistName));
                boolean targetIsDaily = "MORNING".equals(checklistName) || "EVENING".equals(checklistName);

                if (targetIsDaily && !sourceIsDaily) {
                    t.setChecklistId(null);
                    t.setType("MORNING".equals(checklistName) ? TaskType.MORNING : TaskType.EVENING);
                } else if (!targetIsDaily && sourceIsDaily) {
                    Checklist targetChecklist = taskManager.getCustomChecklists().stream()
                        .filter(c -> checklistName.equals(c.getName()))
                        .findFirst()
                        .orElse(null);
                    if (targetChecklist != null) {
                        t.setChecklistId(targetChecklist.getId());
                        t.setType(TaskType.CUSTOM);
                    }
                } else if (sourceIsDaily && targetIsDaily && !checklistName.equals(transferData.sourceChecklistName)) {
                    t.setChecklistId(null);
                    t.setType("MORNING".equals(checklistName) ? TaskType.MORNING : TaskType.EVENING);
                } else if (!sourceIsDaily && !targetIsDaily) {
                    Checklist targetChecklist = taskManager.getCustomChecklists().stream()
                        .filter(c -> checklistName.equals(c.getName()))
                        .findFirst()
                        .orElse(null);
                    if (targetChecklist != null) {
                        t.setChecklistId(targetChecklist.getId());
                    }
                }
            }

            t.setParentId(target.getId());
            toPersist.add(t);
        }

        // Persist atomically and refresh UI
        taskManager.updateTasks(toPersist);
        if (updateAllPanels != null) updateAllPanels.run();
        return true;
    }
}
