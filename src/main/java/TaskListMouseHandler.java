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
            String name = javax.swing.JOptionPane.showInputDialog(taskList, "Subtask name:", "Add Subtask", javax.swing.JOptionPane.PLAIN_MESSAGE);
            if (name != null) {
                name = name.trim();
                if (!name.isEmpty()) {
                    createAndSelectSubtask(t, name);
                }
            }
        }
    }

    private void createAndSelectSubtask(Task t, String name) {
        Task parent = taskManager.getTaskById(t.getId());
        if (parent == null) parent = t;
        Task newTask = new Task(name, parent.getType(), parent.getWeekday(), parent.getChecklistId());
        newTask.setParentId(parent.getId());
        taskManager.addTask(newTask);
        if (updateCallback != null) updateCallback.run();
        // Select new task in list
        for (int i = 0; i < listModel.getSize(); i++) {
            Task cand = listModel.get(i);
            if (cand != null && cand.getId().equals(newTask.getId())) {
                taskList.setSelectedIndex(i);
                taskList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        taskList.setDragEnabled(priorDragEnabled);
    }
}
