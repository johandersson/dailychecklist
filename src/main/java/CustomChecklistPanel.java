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
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class CustomChecklistPanel extends JPanel {
    private JList<Task> customTaskList;
    private DefaultListModel<Task> customListModel;
    private TaskUpdater taskUpdater;
    private TaskManager taskManager;
    private String checklistName;

    public CustomChecklistPanel(TaskManager taskManager, TaskUpdater taskUpdater, String checklistName) {
        this.taskManager = taskManager;
        this.taskUpdater = taskUpdater;
        this.checklistName = checklistName;
        initialize();
    }

    private void initialize() {
        customListModel = new DefaultListModel<>();
        customTaskList = createTaskList(customListModel);
        JPanel customPanel = createPanel(checklistName, customTaskList);
        setLayout(new BorderLayout());
        add(customPanel, BorderLayout.CENTER);
    }

    private JList<Task> createTaskList(DefaultListModel<Task> listModel) {
        JList<Task> taskList = new JList<>(listModel);
        taskList.setCellRenderer(new CheckboxListCellRenderer());
        taskList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JList<Task> list = (JList<Task>) e.getSource();
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        showContextMenu(e, list, index);
                    } else if (e.getClickCount() == 2) {
                        // Double-click to edit name
                        Task task = list.getModel().getElementAt(index);
                        String rawNewName = JOptionPane.showInputDialog(CustomChecklistPanel.this, "Enter new name for checklist:", task.getName());
                        String newName = TaskManager.validateInputWithError(rawNewName, "Task name");
                        if (newName != null) {
                            task.setName(newName);
                            taskManager.updateTask(task);
                            list.repaint(list.getCellBounds(index, index));
                        }
                    } else {
                        Task task = list.getModel().getElementAt(index);
                        task.setDone(!task.isDone());
                        if (task.isDone()) {
                            task.setDoneDate(new Date(System.currentTimeMillis()));
                        } else {
                            task.setDoneDate(null);
                        }
                        taskManager.updateTask(task);
                        list.repaint(list.getCellBounds(index, index));
                    }
                }
            }
        });
        return taskList;
    }

    private void showContextMenu(MouseEvent e, JList<Task> list, int index) {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Rename task");
        editItem.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            String rawNewName = JOptionPane.showInputDialog(CustomChecklistPanel.this, "Enter new name for task:", task.getName());
            String newName = TaskManager.validateInputWithError(rawNewName, "Task name");
            if (newName != null) {
                task.setName(newName);
                taskManager.updateTask(task);
                list.repaint(list.getCellBounds(index, index));
            }
        });
        JMenuItem removeItem = new JMenuItem("Remove task");
        removeItem.addActionListener(event -> removeTask(list, index));
        contextMenu.add(editItem);
        contextMenu.add(removeItem);
        contextMenu.show(list, e.getX(), e.getY());
    }

    private void removeTask(JList<Task> list, int index) {
        Task task = list.getModel().getElementAt(index);
        int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove this task?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            taskManager.removeTask(task);
            updateTasks();
        }
    }

    private JPanel createPanel(String title, JList<Task> taskList) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder(title));
        JScrollPane scrollPane = new JScrollPane(taskList);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    public void updateTasks() {
        customListModel.clear();
        List<Task> tasks = taskManager.getTasks(TaskType.CUSTOM, checklistName);
        for (Task task : tasks) {
            customListModel.addElement(task);
        }
    }
}