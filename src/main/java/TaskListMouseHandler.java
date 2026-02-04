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

import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JList;

public class TaskListMouseHandler extends MouseAdapter {
    private final JList<Task> taskList;
    private final DefaultListModel<Task> listModel;
    private final TaskManager taskManager;
    private final Runnable updateCallback;
    // Use UiLayout constants for icon areas

    private boolean priorDragEnabled = true;

    public TaskListMouseHandler(JList<Task> taskList, DefaultListModel<Task> listModel, TaskManager taskManager, Runnable updateCallback) {
        this.taskList = taskList;
        this.listModel = listModel;
        this.taskManager = taskManager;
        this.updateCallback = updateCallback;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int idx = taskList.locationToIndex(e.getPoint());
        if (idx < 0) return;
        Rectangle cb = taskList.getCellBounds(idx, idx);
        if (cb == null) return;
        Task t = taskList.getModel().getElementAt(idx);
        int checkboxX = cb.x + UiLayout.CHECKBOX_X + (t != null && t.getParentId() != null ? UiLayout.SUBTASK_INDENT : 0);
        int checkboxY = cb.y + cb.height / 2 - UiLayout.CHECKBOX_SIZE / 2;
        int checkboxSize = UiLayout.CHECKBOX_SIZE;
        boolean onCheckbox = e.getX() >= checkboxX && e.getX() <= checkboxX + checkboxSize &&
                             e.getY() >= checkboxY && e.getY() <= checkboxY + checkboxSize;
        if (onCheckbox) {
            priorDragEnabled = taskList.getDragEnabled();
            taskList.setDragEnabled(false);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        handleAddSubtaskClick(e);
    }

    private void handleAddSubtaskClick(MouseEvent e) {
        int idx = taskList.locationToIndex(e.getPoint());
        if (idx < 0) return;
        Rectangle cb = taskList.getCellBounds(idx, idx);
        if (cb == null) return;
        int relX = e.getX() - cb.x;
        int cellW = cb.width;

        int reminderStart = cellW - UiLayout.WEEKDAY_ICON_AREA - UiLayout.REMINDER_ICON_AREA;
        int addIconX = reminderStart - UiLayout.ADD_SUBTASK_OFFSET; // matches renderer spacing

        Task t = taskList.getModel().getElementAt(idx);
        if (t == null) return;
        if (t.getParentId() != null) return; // top-level only

        Icon addIcon = IconCache.getAddSubtaskIcon();
        int aw = addIcon.getIconWidth();

        if (relX >= addIconX && relX <= addIconX + aw) {
            Task parentForDialog = (taskManager != null) ? taskManager.getTaskById(t.getId()) : t;
            
            // Show multi-line dialog for adding multiple subtasks
            java.util.List<String> subtaskNames = MultiSubtaskDialog.show(taskList, 
                parentForDialog != null ? parentForDialog.getName() : t.getName());
            
            // Create and insert all subtasks at once (batch operation)
            if (!subtaskNames.isEmpty()) {
                createAndInsertSubtasks(t, subtaskNames);
            }
        }
    }

    private void createAndInsertSubtasks(Task parent, java.util.List<String> subtaskNames) {
        Task p = taskManager.getTaskById(parent.getId());
        if (p == null) p = parent;
        
        // Create all subtask objects
        java.util.List<Task> newSubtasks = new java.util.ArrayList<>();
        for (String name : subtaskNames) {
            Task newTask = new Task(name, p.getType(), p.getWeekday(), p.getChecklistId());
            newTask.setParentId(p.getId());
            newSubtasks.add(newTask);
        }
        
        // Find the parent's position in the list
        int parentIndex = -1;
        for (int i = 0; i < listModel.getSize(); i++) {
            Task cand = listModel.get(i);
            if (cand != null && cand.getId().equals(p.getId())) {
                parentIndex = i;
                break;
            }
        }
        
        if (parentIndex >= 0) {
            try {
                // Begin batch operation to prevent race conditions with other panels
                taskManager.beginBatchOperation();
                
                // Find position: after parent and after any existing subtasks
                // Calculate this BEFORE adding any tasks to avoid model inconsistency
                int insertIndex = parentIndex + 1;
                while (insertIndex < listModel.getSize()) {
                    Task candidate = listModel.get(insertIndex);
                    if (candidate.getParentId() == null || !candidate.getParentId().equals(p.getId())) {
                        break;
                    }
                    insertIndex++;
                }
                
                // Precompute display data once for all new subtasks (batch operation)
                DisplayPrecomputer.precomputeForList(newSubtasks, taskManager, true);
                
                // Add all subtasks to TaskManager first (batch persistence)
                for (Task subtask : newSubtasks) {
                    taskManager.addTask(subtask);
                }
                
                // Then insert all into the model at the calculated position
                for (Task subtask : newSubtasks) {
                    listModel.add(insertIndex, subtask);
                    insertIndex++;
                }
                
                // Select the last added subtask - use invokeLater to ensure UI updates complete
                final int finalIndex = insertIndex - 1;
                javax.swing.SwingUtilities.invokeLater(() -> {
                    taskList.setSelectedIndex(finalIndex);
                    taskList.ensureIndexIsVisible(finalIndex);
                    taskList.repaint();
                });
            } finally {
                taskManager.endBatchOperation();
            }
        } else {
            // Fallback: add all to TaskManager and trigger full reload
            try {
                taskManager.beginBatchOperation();
                for (Task subtask : newSubtasks) {
                    taskManager.addTask(subtask);
                }
            } finally {
                taskManager.endBatchOperation();
            }
            if (updateCallback != null) updateCallback.run();
        }
    }

    private void createAndSelectSubtask(Task t, String name) {
        // Legacy method - now calls batch version with single item
        java.util.List<String> names = new java.util.ArrayList<>();
        names.add(name);
        createAndInsertSubtasks(t, names);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        taskList.setDragEnabled(priorDragEnabled);
    }
}
