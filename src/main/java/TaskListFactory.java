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
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JList;
import javax.swing.ToolTipManager;

public class TaskListFactory {
    private TaskListFactory() {}

    public static JList<Task> createTaskList(DefaultListModel<Task> listModel, TaskManager taskManager, String checklistName, Runnable updateCallback, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel) {
        JList<Task> taskList = new TaskJList(listModel, taskManager);
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

            taskList.addMouseListener(new TaskListMouseHandler(taskList, listModel, taskManager, updateCallback));
            // Ensure ToolTipManager will query our getToolTipText override
            ToolTipManager.sharedInstance().registerComponent(taskList);
            // Ctrl+A: select all items in the list for easy batch deletion (O(n))
            javax.swing.KeyStroke selectAllKs = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.Event.CTRL_MASK);
            taskList.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(selectAllKs, "selectAllItems");
            taskList.getActionMap().put("selectAllItems", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    int size = listModel.getSize();
                    if (size > 0) {
                        taskList.setSelectionInterval(0, size - 1);
                    }
                }
            });
        }
        return taskList;
    }
    
    private static class TaskJList extends JList<Task> {
        private static final long serialVersionUID = 1L;
        private final TaskManager taskManager;
        // Use UiLayout constants

        TaskJList(DefaultListModel<Task> model, TaskManager taskManager) {
            super(model);
            this.taskManager = taskManager;
        }

        @Override
        public String getToolTipText(java.awt.event.MouseEvent e) {
            int idx = locationToIndex(e.getPoint());
            if (idx < 0) return super.getToolTipText(e);
            java.awt.Rectangle cb = getCellBounds(idx, idx);
            if (cb == null) return super.getToolTipText(e);
            int relX = e.getX() - cb.x;
            int cellW = cb.width;

            Task t = getModel().getElementAt(idx);
            String tip = ReminderTooltipProvider.getTooltip(t, relX, cellW, taskManager);
            return tip != null ? tip : super.getToolTipText(e);
        }
    }
}