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
        DebugLog.d("TaskOrderPersister.persist: checklist=%s checklistTasksCount=%d", checklistName, checklistTasks.size());
        StringBuilder ctIds = new StringBuilder();
        for (int i = 0; i < checklistTasks.size(); i++) ctIds.append(i == 0 ? "" : ",").append(checklistTasks.get(i).getId());
        DebugLog.d("TaskOrderPersister.persist: checklistTasksIds=%s", ctIds.toString());

        List<Task> reorderedTasks = buildReorderedTasks(listModel, checklistTasks);
        DebugLog.d("TaskOrderPersister.persist: reorderedTasksCount=%d", reorderedTasks.size());
        StringBuilder rtIds = new StringBuilder();
        for (int i = 0; i < reorderedTasks.size(); i++) rtIds.append(i == 0 ? "" : ",").append(reorderedTasks.get(i).getId());
        DebugLog.d("TaskOrderPersister.persist: reorderedTasksIds=%s", rtIds.toString());

        int startIndex = findChecklistStartIndex(allTasks, checklistName, taskManager);
        DebugLog.d("TaskOrderPersister.persist: startIndex=%d allTasksSize=%d", startIndex, allTasks.size());

        removeChecklistTasks(allTasks, checklistTasks);
        DebugLog.d("TaskOrderPersister.persist: after removeChecklistTasks allTasksSize=%d", allTasks.size());
        // Show a small window of surrounding IDs for context
        int contextStart = Math.max(0, (startIndex == -1 ? 0 : startIndex) - 5);
        int contextEnd = Math.min(allTasks.size(), (startIndex == -1 ? allTasks.size() : startIndex) + 10);
        StringBuilder around = new StringBuilder();
        for (int i = contextStart; i < contextEnd; i++) {
            if (around.length() > 0) around.append(",");
            around.append(i).append("=").append(allTasks.get(i).getId());
        }
        DebugLog.d("TaskOrderPersister.persist: aroundStartIndex=%d context=%s", contextStart, around.toString());

        insertReorderedTasks(allTasks, reorderedTasks, startIndex);
        DebugLog.d("TaskOrderPersister.persist: after insert allTasksSize=%d", allTasks.size());

        StringBuilder finalIds = new StringBuilder();
        for (int i = Math.max(0, (startIndex == -1 ? 0 : startIndex) - 5); i < Math.min(allTasks.size(), (startIndex == -1 ? allTasks.size() : startIndex) + reorderedTasks.size() + 5); i++) {
            if (finalIds.length() > 0) finalIds.append(",");
            finalIds.append(i).append("=").append(allTasks.get(i).getId());
        }
        DebugLog.d("TaskOrderPersister.persist: finalContext=%s", finalIds.toString());

        taskManager.setTasks(allTasks);
    }

    private static List<Task> collectChecklistTasks(List<Task> allTasks, String checklistName, TaskManager taskManager) {
        List<Task> checklistTasks = new ArrayList<>();
        for (Task t : allTasks) {
            boolean belongsToChecklist = false;
            if (t.getType() == TaskType.HEADING) {
                // A heading belongs to the same checklist as its referenced parent (if available)
                Task parent = t.getParentId() == null ? null : taskManager.getTaskById(t.getParentId());
                if (parent != null) {
                    if ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) {
                        belongsToChecklist = checklistName.equals(parent.getType().toString());
                    } else {
                        Checklist checklist = taskManager.getCustomChecklists().stream()
                            .filter(c -> checklistName.equals(c.getName()))
                            .findFirst()
                            .orElse(null);
                        belongsToChecklist = checklist != null && checklist.getId().equals(parent.getChecklistId());
                    }
                }
            } else {
                if ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) {
                    belongsToChecklist = checklistName.equals(t.getType().toString());
                } else {
                    Checklist checklist = taskManager.getCustomChecklists().stream()
                        .filter(c -> checklistName.equals(c.getName()))
                        .findFirst()
                        .orElse(null);
                    belongsToChecklist = checklist != null && checklist.getId().equals(t.getChecklistId());
                }
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
            boolean belongsToChecklist = false;
            if (t.getType() == TaskType.HEADING) {
                Task parent = t.getParentId() == null ? null : taskManager.getTaskById(t.getParentId());
                if (parent != null) {
                    if ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) {
                        belongsToChecklist = checklistName.equals(parent.getType().toString());
                    } else {
                        Checklist checklist = taskManager.getCustomChecklists().stream()
                            .filter(c -> checklistName.equals(c.getName()))
                            .findFirst()
                            .orElse(null);
                        belongsToChecklist = checklist != null && checklist.getId().equals(parent.getChecklistId());
                    }
                }
            } else {
                if ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) {
                    belongsToChecklist = checklistName.equals(t.getType().toString());
                } else {
                    Checklist checklist = taskManager.getCustomChecklists().stream()
                        .filter(c -> checklistName.equals(c.getName()))
                        .findFirst()
                        .orElse(null);
                    belongsToChecklist = checklist != null && checklist.getId().equals(t.getChecklistId());
                }
            }
            if (belongsToChecklist) return i;
        }
        return -1;
    }
}
