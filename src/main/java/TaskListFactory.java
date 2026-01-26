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
import javax.swing.DropMode;
import javax.swing.JList;

public class TaskListFactory {
    private TaskListFactory() {}

    public static JList<Task> createTaskList(DefaultListModel<Task> listModel, TaskManager taskManager, String checklistName, Runnable updateCallback, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel) {
        JList<Task> taskList = new JList<>(listModel);
        taskList.setCellRenderer(new CheckboxListCellRenderer(taskManager));
        taskList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        taskList.setSelectionBackground(new java.awt.Color(184, 207, 229)); // Consistent selection color
        taskList.setSelectionForeground(java.awt.Color.BLACK);
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            // Enable drag by default for all lists. To avoid accidental drag when clicking
            // the checkbox area, we temporarily disable drag while the mouse is pressed
            // inside the checkbox bounds and re-enable it on release.
            taskList.setDragEnabled(true);
            taskList.setTransferHandler(new TaskTransferHandler(taskList, listModel, taskManager, checklistName, updateCallback, morningListModel, eveningListModel));
            taskList.setDropMode(DropMode.INSERT);

            taskList.addMouseListener(new MouseAdapter() {
                private boolean priorDragEnabled = true;

                @Override
                public void mousePressed(MouseEvent e) {
                    int idx = taskList.locationToIndex(e.getPoint());
                    if (idx < 0) return;
                    Rectangle cb = taskList.getCellBounds(idx, idx);
                    int checkboxX = cb.x + 10;
                    int checkboxY = cb.y + cb.height / 2 - 10;
                    int checkboxSize = 20;
                    boolean onCheckbox = e.getX() >= checkboxX && e.getX() <= checkboxX + checkboxSize &&
                                         e.getY() >= checkboxY && e.getY() <= checkboxY + checkboxSize;
                    if (onCheckbox) {
                        priorDragEnabled = taskList.getDragEnabled();
                        taskList.setDragEnabled(false);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    taskList.setDragEnabled(priorDragEnabled);
                }
            });
        }
        return taskList;
    }
}