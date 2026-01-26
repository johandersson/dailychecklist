/*
 * Daily Checklist
 */
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JList;

public class TaskMoveHandler {
    public static boolean performMove(JList<Task> list, DefaultListModel<Task> listModel, TaskManager taskManager, String checklistName, Runnable updateAllPanels, List<Task> tasks, int dropIndex) {
        // Check if move is allowed
        boolean isTargetDaily = "MORNING".equals(checklistName) || "EVENING".equals(checklistName);
        // Ensure drop index is within valid bounds
        int maxIndex = listModel.getSize();
        if (dropIndex > maxIndex) {
            dropIndex = maxIndex;
        }
        if (dropIndex < 0) {
            dropIndex = 0;
        }

        final int finalDropIndex = dropIndex;
        javax.swing.SwingUtilities.invokeLater(() -> {
            // Update the properties for all tasks
            for (Task task : tasks) {
                updateTaskPropertiesForMove(task, task.getChecklistId(), checklistName, taskManager);
                taskManager.updateTask(task);
            }

            // For daily checklists, reorder the tasks to place moved tasks at the correct position
            if ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) {
                reorderTasksForDailyList(checklistName, tasks, finalDropIndex, taskManager);
            }

            // Restore focus to the target list for custom checklists
            if (!isTargetDaily) {
                list.requestFocusInWindow();
            }

            // Update all panels to reflect the changes
            if (updateAllPanels != null) {
                updateAllPanels.run();
            }

            // After panels are updated, ensure the moved tasks are selected in the target list
            // and scrolled into view. Delegate to helper to keep performMove concise.
            handlePostMoveSelection(list, listModel, checklistName, tasks, isTargetDaily);
        });

        return true;
    }

    private static void handlePostMoveSelection(JList<Task> list, DefaultListModel<Task> listModel, String checklistName, List<Task> tasks, boolean isTargetDaily) {
        try {
            if (!isTargetDaily) {
                final int[] attempts = {0};
                final int maxAttempts = 10;
                javax.swing.Timer retryTimer = new javax.swing.Timer(50, null);
                retryTimer.addActionListener(evt -> {
                    attempts[0]++;
                    try {
                        java.util.List<Integer> indices = new java.util.ArrayList<>();
                        for (Task t : tasks) {
                            for (int i = 0; i < listModel.getSize(); i++) {
                                Task modelTask = listModel.get(i);
                                if (modelTask != null && modelTask.getId().equals(t.getId())) {
                                    indices.add(i);
                                    break;
                                }
                            }
                        }
                        if (!indices.isEmpty()) {
                            retryTimer.stop();
                            int[] idxArr = indices.stream().mapToInt(Integer::intValue).toArray();
                            list.setSelectedIndices(idxArr);
                            list.ensureIndexIsVisible(idxArr[0]);
                            list.requestFocusInWindow();
                            try {
                                DailyChecklist app = DailyChecklist.getInstance();
                                if (app != null && !tasks.isEmpty()) {
                                    app.showCustomChecklist(checklistName);
                                    app.jumpToTask(tasks.get(0));
                                }
                            } catch (Exception ignore) {}
                        } else if (attempts[0] >= maxAttempts) {
                            retryTimer.stop();
                            list.requestFocusInWindow();
                        }
                    } catch (Exception ignore) {
                        if (attempts[0] >= maxAttempts) retryTimer.stop();
                    }
                });
                retryTimer.setRepeats(true);
                retryTimer.start();
            } else {
                java.util.List<Integer> indices = new java.util.ArrayList<>();
                for (Task t : tasks) {
                    for (int i = 0; i < listModel.getSize(); i++) {
                        Task modelTask = listModel.get(i);
                        if (modelTask != null && modelTask.getId().equals(t.getId())) {
                            indices.add(i);
                            break;
                        }
                    }
                }
                if (!indices.isEmpty()) {
                    int[] idxArr = indices.stream().mapToInt(Integer::intValue).toArray();
                    list.setSelectedIndices(idxArr);
                    list.ensureIndexIsVisible(idxArr[0]);
                    list.requestFocusInWindow();
                    try {
                        DailyChecklist app = DailyChecklist.getInstance();
                        if (app != null && !tasks.isEmpty()) {
                            app.jumpToTask(tasks.get(0));
                        }
                    } catch (Exception ignore) {}
                } else {
                    list.requestFocusInWindow();
                }
            }
        } catch (Exception ignore) {}
    }

    private static void updateTaskPropertiesForMove(Task task, String sourceChecklistName, String targetChecklistName, TaskManager taskManager) {
        boolean sourceIsDaily = sourceChecklistName != null && ("MORNING".equals(sourceChecklistName) || "EVENING".equals(sourceChecklistName));
        boolean targetIsDaily = "MORNING".equals(targetChecklistName) || "EVENING".equals(targetChecklistName);

        if (targetIsDaily && !sourceIsDaily) {
            task.setChecklistId(null);
            task.setType("MORNING".equals(targetChecklistName) ? TaskType.MORNING : TaskType.EVENING);
        } else if (!targetIsDaily && sourceIsDaily) {
            Checklist targetChecklist = taskManager.getCustomChecklists().stream()
                .filter(c -> targetChecklistName.equals(c.getName()))
                .findFirst()
                .orElse(null);
            if (targetChecklist != null) {
                task.setChecklistId(targetChecklist.getId());
                task.setType(TaskType.CUSTOM);
            }
        } else if (sourceIsDaily && targetIsDaily && !targetChecklistName.equals(sourceChecklistName)) {
            task.setChecklistId(null);
            task.setType("MORNING".equals(targetChecklistName) ? TaskType.MORNING : TaskType.EVENING);
        } else if (!sourceIsDaily && !targetIsDaily) {
            Checklist targetChecklist = taskManager.getCustomChecklists().stream()
                .filter(c -> targetChecklistName.equals(c.getName()))
                .findFirst()
                .orElse(null);
            if (targetChecklist != null) {
                task.setChecklistId(targetChecklist.getId());
            }
        }
    }

    private static void reorderTasksForDailyList(String checklistName, List<Task> movedTasks, int dropIndex, TaskManager taskManager) {
        List<Task> allTasks = new ArrayList<>(taskManager.getAllTasks());
        TaskType targetType = "MORNING".equals(checklistName) ? TaskType.MORNING : TaskType.EVENING;

        // Get all tasks of the target type in their current order
        List<Task> targetTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.getType() == targetType) {
                targetTasks.add(task);
            }
        }

        int insertIndex = dropIndex;
        if (insertIndex > targetTasks.size()) {
            insertIndex = targetTasks.size();
        }

        List<Task> tasksToInsert = new ArrayList<>();
        for (Task movedTask : movedTasks) {
            allTasks.removeIf(task -> task.getId().equals(movedTask.getId()));
            tasksToInsert.add(movedTask);
        }

        int targetStartIndex = -1;
        for (int i = 0; i < allTasks.size(); i++) {
            if (allTasks.get(i).getType() == targetType) {
                targetStartIndex = i;
                break;
            }
        }

        if (targetStartIndex == -1) {
            allTasks.addAll(tasksToInsert);
        } else {
            int currentTargetIndex = 0;
            int insertionPoint = targetStartIndex;
            for (int i = targetStartIndex; i < allTasks.size() && currentTargetIndex < insertIndex; i++) {
                if (allTasks.get(i).getType() == targetType) {
                    currentTargetIndex++;
                    insertionPoint = i + 1;
                }
            }
            allTasks.addAll(insertionPoint, tasksToInsert);
        }

        taskManager.setTasks(allTasks);
    }
}
