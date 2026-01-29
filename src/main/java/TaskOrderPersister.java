import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;

/**
 * Helper for persisting task order for a checklist.
 */
public final class TaskOrderPersister {
    private TaskOrderPersister() {}

    public static void persist(DefaultListModel<Task> listModel, String checklistName, TaskManager taskManager) {
        List<Task> allTasks = new ArrayList<>(taskManager.getAllTasks());

        // Find the tasks that belong to this checklist in their current order
        List<Task> checklistTasks = new ArrayList<>();
        for (Task t : allTasks) {
            boolean belongsToChecklist;
            if ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) {
                belongsToChecklist = checklistName.equals(t.getType().toString());
            } else {
                Checklist checklist = taskManager.getCustomChecklists().stream()
                    .filter(c -> checklistName.equals(c.getName()))
                    .findFirst()
                    .orElse(null);
                belongsToChecklist = checklist != null && checklist.getId().equals(t.getChecklistId());
            }
            if (belongsToChecklist) {
                checklistTasks.add(t);
            }
        }

        // Reorder checklistTasks to match listModel order. Match by task id
        // to ensure we use the authoritative Task instances from taskManager.
        List<Task> reorderedTasks = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            Task modelTask = listModel.get(i);
            Task authoritative = null;
            for (Task ct : checklistTasks) {
                if (ct.getId().equals(modelTask.getId())) {
                    authoritative = ct;
                    break;
                }
            }
            if (authoritative != null) {
                reorderedTasks.add(authoritative);
            }
        }

        // Replace the tasks in allTasks with the reordered ones.
        int startIndex = findChecklistStartIndex(allTasks, checklistName, taskManager);

        // Remove existing checklist tasks from the global list
        allTasks.removeAll(checklistTasks);

        if (startIndex == -1) {
            // No existing block found: append at end
            allTasks.addAll(reorderedTasks);
        } else {
            // Insert reordered tasks at the previously found start index
            if (startIndex > allTasks.size()) startIndex = allTasks.size();
            allTasks.addAll(startIndex, reorderedTasks);
        }

        // Save the reordered tasks
        taskManager.setTasks(allTasks);
    }

    private static int findChecklistStartIndex(List<Task> allTasks, String checklistName, TaskManager taskManager) {
        for (int i = 0; i < allTasks.size(); i++) {
            Task t = allTasks.get(i);
            boolean belongsToChecklist;
            if ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) {
                belongsToChecklist = checklistName.equals(t.getType().toString());
            } else {
                Checklist checklist = taskManager.getCustomChecklists().stream()
                    .filter(c -> checklistName.equals(c.getName()))
                    .findFirst()
                    .orElse(null);
                belongsToChecklist = checklist != null && checklist.getId().equals(t.getChecklistId());
            }
            if (belongsToChecklist) {
                return i;
            }
        }
        return -1;
    }
}
