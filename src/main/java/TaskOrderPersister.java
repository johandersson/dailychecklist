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

        List<Task> checklistTasks = collectChecklistTasks(allTasks, checklistName, taskManager);
        List<Task> reorderedTasks = buildReorderedTasks(listModel, checklistTasks);

        int startIndex = findChecklistStartIndex(allTasks, checklistName, taskManager);
        removeChecklistTasks(allTasks, checklistTasks);
        insertReorderedTasks(allTasks, reorderedTasks, startIndex);

        taskManager.setTasks(allTasks);
    }

    private static List<Task> collectChecklistTasks(List<Task> allTasks, String checklistName, TaskManager taskManager) {
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
            if (belongsToChecklist) checklistTasks.add(t);
        }
        return checklistTasks;
    }

    private static List<Task> buildReorderedTasks(DefaultListModel<Task> listModel, List<Task> checklistTasks) {
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
            if (authoritative != null) reorderedTasks.add(authoritative);
        }
        return reorderedTasks;
    }

    private static void removeChecklistTasks(List<Task> allTasks, List<Task> checklistTasks) {
        allTasks.removeAll(checklistTasks);
    }

    private static void insertReorderedTasks(List<Task> allTasks, List<Task> reorderedTasks, int startIndex) {
        if (startIndex == -1) {
            allTasks.addAll(reorderedTasks);
        } else {
            if (startIndex > allTasks.size()) startIndex = allTasks.size();
            allTasks.addAll(startIndex, reorderedTasks);
        }
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
