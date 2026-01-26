/*
 * Daily Checklist
 */
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;

public class TaskReorderHandler {
    public static boolean performReorder(DefaultListModel<Task> listModel, TaskManager taskManager, String checklistName, List<Task> tasks, int dropIndex) {
        // Ensure drop index is within valid bounds
        int originalSize = listModel.getSize();
        if (dropIndex > originalSize) {
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

        final int finalDropIndex = dropIndex;
        javax.swing.SwingUtilities.invokeLater(() -> {
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

            // Persist the new order in the underlying data
            persistTaskOrder(listModel, checklistName, taskManager);
        });

        return true;
    }

    private static void persistTaskOrder(DefaultListModel<Task> listModel, String checklistName, TaskManager taskManager) {
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

        // Reorder checklistTasks to match listModel order
        List<Task> reorderedTasks = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            Task task = listModel.get(i);
            int indexInChecklistTasks = checklistTasks.indexOf(task);
            if (indexInChecklistTasks >= 0) {
                reorderedTasks.add(checklistTasks.get(indexInChecklistTasks));
            }
        }

        // Replace the tasks in allTasks with the reordered ones
        allTasks.removeAll(checklistTasks);
        int insertIndex = 0;
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
                insertIndex = allTasks.indexOf(t) + 1;
            }
        }
        allTasks.addAll(insertIndex, reorderedTasks);

        // Save the reordered tasks
        taskManager.setTasks(allTasks);
    }
}
