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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import javax.swing.JList;
import javax.swing.SwingUtilities;

public class TaskListMouseAdapter extends MouseAdapter {
    private final TaskManager taskManager;

    public TaskListMouseAdapter(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void mouseClicked(MouseEvent e) {
        JList<Task> list = (JList<Task>) e.getSource();
        int index = list.locationToIndex(e.getPoint());
        if (index >= 0) {
            java.awt.Rectangle cellBounds = list.getCellBounds(index, index);
            Task task = list.getModel().getElementAt(index);
            int checkboxX = cellBounds.x + UiLayout.CHECKBOX_X + (task != null && task.getParentId() != null ? UiLayout.SUBTASK_INDENT : 0);
            int checkboxY = cellBounds.y + cellBounds.height / 2 - UiLayout.CHECKBOX_SIZE / 2;
            int checkboxSize = UiLayout.CHECKBOX_SIZE;
            boolean onCheckbox = e.getPoint().x >= checkboxX && e.getPoint().x <= checkboxX + checkboxSize &&
                                 e.getPoint().y >= checkboxY && e.getPoint().y <= checkboxY + checkboxSize;

            if (SwingUtilities.isRightMouseButton(e)) {
                // Right-click: ensure the item is selected
                if (!list.isSelectedIndex(index)) {
                    list.setSelectedIndex(index);
                }
                // showContextMenu is handled by subclasses or passed
            } else if (onCheckbox && e.getClickCount() == 1) {
                // Single-click on checkbox: toggle done
                task.setDone(!task.isDone());
                if (task.isDone()) {
                    task.setDoneDate(new Date(System.currentTimeMillis()));
                } else {
                    task.setDoneDate(null);
                }
                taskManager.updateTask(task);
                list.repaint(cellBounds);
            } else if (e.getClickCount() == 2) {
                // Double-click: edit task
                // This can be overridden
            }
        }
    }
}