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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

@SuppressWarnings("serial")
public class TaskTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;
    private JList<Task> list;
    private final DefaultListModel<Task> listModel;
    private transient final TaskManager taskManager;
    private final String checklistName; // null for daily checklists
    private transient final Runnable updateAllPanels;
    private final DefaultListModel<Task> morningListModel;
    private final DefaultListModel<Task> eveningListModel;

    public TaskTransferHandler(JList<Task> list, DefaultListModel<Task> listModel, TaskManager taskManager, String checklistName) {
        this(list, listModel, taskManager, checklistName, null);
    }

    public TaskTransferHandler(JList<Task> list, DefaultListModel<Task> listModel, TaskManager taskManager, String checklistName, Runnable updateAllPanels) {
        this(list, listModel, taskManager, checklistName, updateAllPanels, null, null);
    }

    public TaskTransferHandler(JList<Task> list, DefaultListModel<Task> listModel, TaskManager taskManager, String checklistName, Runnable updateAllPanels, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel) {
        this.list = list;
        this.listModel = listModel;
        this.taskManager = taskManager;
        this.checklistName = checklistName;
        this.updateAllPanels = updateAllPanels;
        this.morningListModel = morningListModel;
        this.eveningListModel = eveningListModel;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        int[] selectedIndices = list.getSelectedIndices();
        if (selectedIndices.length > 0) {
            StringBuilder data = new StringBuilder(checklistName);
            for (int index : selectedIndices) {
                Task task = listModel.get(index);
                data.append(";").append(task.getId());
            }
            return new StringSelection(data.toString());
        }
        return null;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        if (updateAllPanels != null) {
            updateAllPanels.run();
        }
    }

    @Override
    public Image getDragImage() {
        int[] selectedIndices = list.getSelectedIndices();
        if (selectedIndices.length > 0) {
            if (selectedIndices.length == 1) {
                Task task = listModel.get(selectedIndices[0]);
                return createDragImage(task);
            } else {
                // Multiple tasks selected
                return createMultiDragImage(selectedIndices.length);
            }
        }
        return null;
    }

    @Override
    public Point getDragImageOffset() {
        return new Point(10, 10); // Offset from cursor
    }

    private Image createDragImage(Task task) {
        String text = task.getName();
        if (text.length() > 20) {
            text = text.substring(0, 17) + "...";
        }

        // Create a simple text image
        BufferedImage image = new BufferedImage(200, 25, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw background
        g2d.setColor(new java.awt.Color(240, 240, 240, 200)); // Semi-transparent light gray
        g2d.fillRoundRect(0, 0, 200, 25, 5, 5);

        // Draw border
        g2d.setColor(java.awt.Color.GRAY);
        g2d.drawRoundRect(0, 0, 199, 24, 5, 5);

        // Draw text
        g2d.setColor(java.awt.Color.BLACK);
        g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
        g2d.drawString(text, 5, 17);

        g2d.dispose();
        return image;
    }

    private Image createMultiDragImage(int count) {
        String text = count + " tasks";
        if (text.length() > 15) {
            text = count + " items";
        }

        // Create a simple text image for multiple tasks
        BufferedImage image = new BufferedImage(180, 25, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw background
        g2d.setColor(new java.awt.Color(100, 150, 200, 220)); // Semi-transparent blue for multi-select
        g2d.fillRoundRect(0, 0, 180, 25, 5, 5);

        // Draw border
        g2d.setColor(java.awt.Color.BLUE);
        g2d.drawRoundRect(0, 0, 179, 24, 5, 5);

        // Draw text
        g2d.setColor(java.awt.Color.WHITE);
        g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        g2d.drawString(text, 5, 17);

        g2d.dispose();
        return image;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support) || !support.isDrop()) {
            return false;
        }

        try {
            TransferData transferData = extractTransferData(support);
            if (transferData == null) {
                return false;
            }

            if (transferData.sourceChecklistName.equals(checklistName)) {
                return handleSameChecklistReorder(support, transferData, getDropIndex(support));
            } else {
                return handleCrossChecklistMove(support, transferData, getDropIndex(support));
            }
        } catch (UnsupportedFlavorException | IOException e) {
            return false;
        }
    }

    private static class TransferData {
        final String sourceChecklistName;
        final List<Task> tasks;

        TransferData(String sourceChecklistName, List<Task> tasks) {
            this.sourceChecklistName = sourceChecklistName;
            this.tasks = tasks;
        }
    }

    private TransferData extractTransferData(TransferHandler.TransferSupport support)
            throws UnsupportedFlavorException, IOException {
        String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
        String[] parts = data.split(";", -1);
        if (parts.length < 2) {
            return null;
        }

        String sourceChecklistName = parts[0];

        // Extract all task IDs
        List<String> taskIds = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                taskIds.add(parts[i]);
            }
        }

        if (taskIds.isEmpty()) {
            return null;
        }

        // Find all tasks by IDs
        List<Task> tasks = new ArrayList<>();
        for (String taskId : taskIds) {
            Task task = findTaskById(taskId);
            if (task == null) {
                return null;
            }
            tasks.add(task);
        }

        return new TransferData(sourceChecklistName, tasks);
    }

    private boolean handleSameChecklistReorder(TransferHandler.TransferSupport support, TransferData transferData, int dropIndex) {
        List<Task> tasks = transferData.tasks;

        // Ensure drop index is within valid bounds
        JList<?> list = (JList<?>) support.getComponent();
        int originalSize = list.getModel().getSize();
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

        // Defer the list model changes to avoid NPE during cleanup
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
            persistTaskOrder(listModel, checklistName);
        });

        return true;
    }

    private boolean handleCrossChecklistMove(TransferHandler.TransferSupport support, TransferData transferData, int dropIndex) {
        String sourceChecklistName = transferData.sourceChecklistName;
        List<Task> tasks = transferData.tasks;

        // Check if move is allowed
        boolean isSourceDaily = "MORNING".equals(sourceChecklistName) || "EVENING".equals(sourceChecklistName);
        boolean isTargetDaily = "MORNING".equals(checklistName) || "EVENING".equals(checklistName);
        if (isSourceDaily != isTargetDaily) {
            return false; // Disallow moving between daily and custom checklists
        }

        // Ensure drop index is within valid bounds
        JList<?> list = (JList<?>) support.getComponent();
        int maxIndex = list.getModel().getSize();
        if (dropIndex > maxIndex) {
            dropIndex = maxIndex;
        }
        if (dropIndex < 0) {
            dropIndex = 0;
        }

        // Defer the list model changes to avoid NPE during cleanup
        final int finalDropIndex = dropIndex;
        javax.swing.SwingUtilities.invokeLater(() -> {
            // Update the properties for all tasks
            for (Task task : tasks) {
                updateTaskPropertiesForMove(task, sourceChecklistName, checklistName);
                taskManager.updateTask(task);
            }

            // For daily checklists, reorder the tasks to place moved tasks at the correct position
            if ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) {
                reorderTasksForDailyList(checklistName, tasks, finalDropIndex);
            }

            // Restore focus to the target list for custom checklists
            if (!("MORNING".equals(checklistName) || "EVENING".equals(checklistName))) {
                list.requestFocusInWindow();
            }

            // Update all panels to reflect the changes
            if (updateAllPanels != null) {
                updateAllPanels.run();
            }
        });

        return true;
    }

    private void updateTaskPropertiesForMove(Task task, String sourceChecklistName, String targetChecklistName) {
        boolean sourceIsDaily = "MORNING".equals(sourceChecklistName) || "EVENING".equals(sourceChecklistName);
        boolean targetIsDaily = "MORNING".equals(targetChecklistName) || "EVENING".equals(targetChecklistName);

        if (targetIsDaily && !sourceIsDaily) {
            // Moving to daily from custom
            task.setChecklistId(null);
            task.setType("MORNING".equals(targetChecklistName) ? TaskType.MORNING : TaskType.EVENING);
        } else if (!targetIsDaily && sourceIsDaily) {
            // Moving to custom from daily - find the target checklist
            Checklist targetChecklist = taskManager.getCustomChecklists().stream()
                .filter(c -> targetChecklistName.equals(c.getName()))
                .findFirst()
                .orElse(null);
            if (targetChecklist != null) {
                task.setChecklistId(targetChecklist.getId());
                task.setType(TaskType.CUSTOM);
            }
        } else if (sourceIsDaily && targetIsDaily && !sourceChecklistName.equals(targetChecklistName)) {
            // Moving between morning and evening
            task.setChecklistId(null);
            task.setType("MORNING".equals(targetChecklistName) ? TaskType.MORNING : TaskType.EVENING);
        } else if (!sourceIsDaily && !targetIsDaily) {
            // Moving between custom checklists - find the target checklist
            Checklist targetChecklist = taskManager.getCustomChecklists().stream()
                .filter(c -> targetChecklistName.equals(c.getName()))
                .findFirst()
                .orElse(null);
            if (targetChecklist != null) {
                task.setChecklistId(targetChecklist.getId());
            }
        }
    }

    private void persistTaskOrder(DefaultListModel<Task> listModel, String checklistName) {
        List<Task> allTasks = new ArrayList<>(taskManager.getAllTasks());
        
        // Find the tasks that belong to this checklist in their current order
        List<Task> checklistTasks = new ArrayList<>();
        for (Task t : allTasks) {
            boolean belongsToChecklist = false;
            if ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) {
                belongsToChecklist = checklistName.equals(t.getType().toString());
            } else {
                // For custom checklists, find the checklist by name and check ID
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
        // Find the position to insert (after the last task of the same type/checklist)
        int insertIndex = 0;
        for (Task t : allTasks) {
            boolean belongsToChecklist = false;
            if ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) {
                belongsToChecklist = checklistName.equals(t.getType().toString());
            } else {
                // For custom checklists, find the checklist by name and check ID
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

    private void reorderTasksForDailyList(String checklistName, List<Task> movedTasks, int dropIndex) {
        List<Task> allTasks = new ArrayList<>(taskManager.getAllTasks());
        TaskType targetType = "MORNING".equals(checklistName) ? TaskType.MORNING : TaskType.EVENING;
        
        // Get all tasks of the target type in their current order
        List<Task> targetTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.getType() == targetType) {
                targetTasks.add(task);
            }
        }
        
        // Find the insertion point
        int insertIndex = dropIndex;
        if (insertIndex > targetTasks.size()) {
            insertIndex = targetTasks.size();
        }
        
        // Remove moved tasks from their current positions and collect them
        List<Task> tasksToInsert = new ArrayList<>();
        for (Task movedTask : movedTasks) {
            allTasks.removeIf(task -> task.getId().equals(movedTask.getId()));
            tasksToInsert.add(movedTask);
        }
        
        // Find the position in allTasks where target tasks start
        int targetStartIndex = -1;
        for (int i = 0; i < allTasks.size(); i++) {
            if (allTasks.get(i).getType() == targetType) {
                targetStartIndex = i;
                break;
            }
        }
        
        if (targetStartIndex == -1) {
            // No target tasks yet, add at the end
            allTasks.addAll(tasksToInsert);
        } else {
            // Find the insertion point within the target tasks
            int currentTargetIndex = 0;
            int insertionPoint = targetStartIndex;
            
            for (int i = targetStartIndex; i < allTasks.size() && currentTargetIndex < insertIndex; i++) {
                if (allTasks.get(i).getType() == targetType) {
                    currentTargetIndex++;
                    insertionPoint = i + 1;
                }
            }
            
            // Insert the moved tasks at the correct position
            allTasks.addAll(insertionPoint, tasksToInsert);
        }
        
        // Update the task manager with the new order
        taskManager.setTasks(allTasks);
    }

    private Task findTaskById(String taskId) {
        return taskManager.getTaskById(taskId);
    }

    private static int getDropIndex(TransferSupport support) {
        JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
        if (dropLocation == null) {
            return 0;
        }
        int index = dropLocation.getIndex();
        // Ensure index is within valid bounds for the component
        JList<?> list = (JList<?>) support.getComponent();
        int maxIndex = list.getModel().getSize();
        if (index < 0) {
            return 0;
        }
        if (index > maxIndex) {
            return maxIndex;
        }
        return index;
    }
}