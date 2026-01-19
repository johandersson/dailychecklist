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
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

public class CustomChecklistsOverviewPanel extends JPanel {
    private JList<String> checklistList;
    private DefaultListModel<String> listModel;
    private TaskManager taskManager;
    private Runnable updateTasks;
    private JTextField newChecklistField;
    private JButton createButton;
    private JSplitPane splitPane;
    private JPanel rightPanel;
    private CustomChecklistPanel currentChecklistPanel;
    private String selectedChecklistName;
    private JButton addTaskButton;

    public CustomChecklistsOverviewPanel(TaskManager taskManager, Runnable updateTasks) {
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        initialize();
    }

    private void initialize() {
        listModel = new DefaultListModel<>();
        checklistList = new JList<>(listModel);
        checklistList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = checklistList.getSelectedValue();
                selectChecklist(selected);
            }
        });
        checklistList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int index = checklistList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        checklistList.setSelectedIndex(index);
                        selectedChecklistName = checklistList.getSelectedValue();
                        showChecklistPopup(e.getX(), e.getY());
                    }
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int index = checklistList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        checklistList.setSelectedIndex(index);
                        selectedChecklistName = checklistList.getSelectedValue();
                        showChecklistPopup(e.getX(), e.getY());
                    }
                }
            }
        });

        newChecklistField = new JTextField(20);
        createButton = new JButton("Create Checklist");
        createButton.addActionListener(e -> createNewChecklist());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(newChecklistField, BorderLayout.CENTER);
        topPanel.add(createButton, BorderLayout.EAST);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(checklistList), BorderLayout.CENTER);

        rightPanel = new JPanel(new BorderLayout());
        addTaskButton = new JButton("Add Task");
        addTaskButton.addActionListener(e -> addTaskToSelected());
        rightPanel.add(addTaskButton, BorderLayout.SOUTH);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.3);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }

    private void createNewChecklist() {
        String name = newChecklistField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a checklist name.");
            return;
        }
        Set<String> existing = taskManager.getCustomChecklistNames();
        if (existing.contains(name)) {
            JOptionPane.showMessageDialog(this, "Checklist name already exists.");
            return;
        }
        newChecklistField.setText("");
        updateTasks.run();
        selectChecklist(name);
    }

    private void selectChecklist(String checklistName) {
        selectedChecklistName = checklistName;
        if (currentChecklistPanel != null) {
            rightPanel.remove(currentChecklistPanel);
        }
        if (checklistName != null) {
            currentChecklistPanel = new CustomChecklistPanel(taskManager, new TaskUpdater(), checklistName);
            rightPanel.add(currentChecklistPanel, BorderLayout.CENTER);
        } else {
            currentChecklistPanel = null;
        }
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    private void addTaskToSelected() {
        if (selectedChecklistName == null) {
            JOptionPane.showMessageDialog(this, "Please select a checklist first.");
            return;
        }
        String taskName = JOptionPane.showInputDialog(this, "Enter task name:");
        if (taskName != null && !taskName.trim().isEmpty()) {
            Task newTask = new Task(taskName.trim(), TaskType.CUSTOM, null, selectedChecklistName);
            taskManager.addTask(newTask);
            if (currentChecklistPanel != null) {
                currentChecklistPanel.updateTasks();
                rightPanel.revalidate();
                rightPanel.repaint();
            }
            updateTasks.run();
        }
    }

    public void updateTasks() {
        listModel.clear();
        Set<String> names = taskManager.getCustomChecklistNames();
        for (String name : names) {
            listModel.addElement(name);
        }
        if (selectedChecklistName != null && !listModel.contains(selectedChecklistName)) {
            listModel.addElement(selectedChecklistName);
        }
        if (currentChecklistPanel != null) {
            currentChecklistPanel.updateTasks();
        }
    }

    private void showChecklistPopup(int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.addActionListener(e -> renameChecklist());
        menu.add(renameItem);
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> deleteChecklist());
        menu.add(deleteItem);
        menu.show(checklistList, x, y);
    }

    private void renameChecklist() {
        String oldName = selectedChecklistName;
        if (oldName == null) return;
        String newName = JOptionPane.showInputDialog(this, "Enter new name:", oldName);
        if (newName != null && !newName.trim().isEmpty() && !newName.equals(oldName)) {
            List<Task> allTasks = taskManager.getAllTasks();
            for (Task task : allTasks) {
                if (task.getChecklistName() != null && task.getChecklistName().equals(oldName)) {
                    task.setChecklistName(newName);
                    taskManager.updateTask(task);
                }
            }
            updateTasks.run();
            selectChecklist(newName);
        }
    }

    private void deleteChecklist() {
        String name = selectedChecklistName;
        if (name == null) return;
        Object[] options = {"Delete list", "Move to morning", "Move to evening"};
        int choice = JOptionPane.showOptionDialog(this, "What to do with the tasks in '" + name + "'?", "Delete Checklist", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == 0) { // Delete list
            List<Task> allTasks = taskManager.getAllTasks();
            for (Task task : allTasks) {
                if (task.getChecklistName() != null && task.getChecklistName().equals(name)) {
                    taskManager.removeTask(task);
                }
            }
        } else if (choice == 1) { // Move to morning
            moveTasksToType(name, TaskType.MORNING);
        } else if (choice == 2) { // Move to evening
            moveTasksToType(name, TaskType.EVENING);
        }
        updateTasks.run();
        selectChecklist(null);
    }

    private void moveTasksToType(String checklistName, TaskType type) {
        List<Task> allTasks = taskManager.getAllTasks();
        for (Task task : allTasks) {
            if (task.getChecklistName() != null && task.getChecklistName().equals(checklistName)) {
                task.setType(type);
                task.setChecklistName(null);
                task.setDone(false);
                task.setDoneDate(null);
                taskManager.updateTask(task);
            }
        }
    }
}