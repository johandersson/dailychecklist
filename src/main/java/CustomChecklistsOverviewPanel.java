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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class CustomChecklistsOverviewPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JList<String> checklistList;
    private DefaultListModel<String> listModel;
    private transient TaskManager taskManager;
    private transient Runnable updateTasks;
    private JTextField newChecklistField;
    private JButton createButton;
    private JSplitPane splitPane;
    private JPanel rightPanel;
    private CustomChecklistPanel currentChecklistPanel;
    private String selectedChecklistName;
    private AddTaskPanel currentAddPanel;
    private Set<String> allChecklistNames;
    private Map<String, CustomChecklistPanel> panelMap = new HashMap<>();

    @SuppressWarnings("this-escape")
    public CustomChecklistsOverviewPanel(TaskManager taskManager, Runnable updateTasks) {
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        this.allChecklistNames = new java.util.HashSet<>();
        initialize();
    }

    private void initialize() {
        listModel = new DefaultListModel<>();
        checklistList = new JList<>(listModel);
        checklistList.setCellRenderer(new ChecklistCellRenderer(taskManager));
        checklistList.setTransferHandler(new ChecklistListTransferHandler(listModel, taskManager, this::updateTasks));
        checklistList.setDropMode(DropMode.ON);
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

        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, new JScrollPane(rightPanel));
        splitPane.setResizeWeight(0.3);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
        
        // Initialize with existing checklist names
        allChecklistNames.addAll(taskManager.getCustomChecklistNames());
    }

    private void createNewChecklist() {
        String rawName = newChecklistField.getText();
        String name = TaskManager.validateInputWithError(rawName, "Checklist name");
        if (name == null) {
            return;
        }
        Set<String> existing = taskManager.getCustomChecklistNames();
        if (existing.contains(name)) {
            JOptionPane.showMessageDialog(this, "Checklist name already exists.");
            return;
        }
        newChecklistField.setText("");
        allChecklistNames.add(name);  // Track the new checklist
        taskManager.addChecklistName(name);  // Persist the checklist name
        updateTasks();  // Update the list model first
        selectChecklist(name);  // Then select it
        updateTasks.run();  // Update other panels
        checklistList.setSelectedValue(name, true);
        // Ensure UI refreshes
        checklistList.revalidate();
        checklistList.repaint();
    }

    /**
     * Public method to select a checklist by name (used by reminders)
     */
    public void selectChecklistByName(String checklistName) {
        selectChecklist(checklistName);
        if (checklistName != null) {
            checklistList.setSelectedValue(checklistName, true);
        }
    }

    private void selectChecklist(String checklistName) {
        selectedChecklistName = checklistName;
        if (currentChecklistPanel != null) {
            rightPanel.remove(currentChecklistPanel);
        }
        if (currentAddPanel != null) {
            rightPanel.remove(currentAddPanel);
        }
        if (checklistName != null) {
            currentChecklistPanel = new CustomChecklistPanel(taskManager, checklistName, this::updateTasks);
            panelMap.put(checklistName, currentChecklistPanel);
            currentChecklistPanel.updateTasks();
            rightPanel.add(currentChecklistPanel);
            currentAddPanel = new AddTaskPanel(taskManager, tasks -> {
                if (currentChecklistPanel != null) {
                    currentChecklistPanel.updateTasks();
                    rightPanel.revalidate();
                    rightPanel.repaint();
                }
                updateTasks.run();
            }, checklistName);
            rightPanel.add(currentAddPanel);
        } else {
            currentChecklistPanel = null;
            currentAddPanel = null;
        }
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    public void updateTasks() {
        listModel.clear();
        Set<String> names = taskManager.getCustomChecklistNames();
        // Add all checklists that have tasks
        for (String name : names) {
            listModel.addElement(name);
            allChecklistNames.add(name);  // Ensure we track all existing checklists
        }
        // Add all known checklists (including empty ones)
        for (String name : allChecklistNames) {
            if (!listModel.contains(name)) {
                listModel.addElement(name);
            }
        }
        if (currentChecklistPanel != null) {
            currentChecklistPanel.updateTasks();
        }
        // Update all panels to reflect any changes
        for (CustomChecklistPanel panel : panelMap.values()) {
            if (panel != null) panel.updateTasks();
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
        JMenuItem addReminderItem = new JMenuItem("Set Reminder");
        addReminderItem.addActionListener(e -> setReminder());
        menu.add(addReminderItem);
        JMenuItem removeReminderItem = new JMenuItem("Remove Reminder");
        removeReminderItem.addActionListener(e -> {
            if (selectedChecklistName == null) return;
            List<Reminder> allReminders = taskManager.getReminders();
            List<Reminder> toRemove = allReminders.stream()
                    .filter(r -> r.getChecklistName().equals(selectedChecklistName))
                    .toList();
            if (toRemove.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No reminders to remove for '" + selectedChecklistName + "'.");
                return;
            }
            int res = JOptionPane.showConfirmDialog(this, "Remove reminder(s) for '" + selectedChecklistName + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                toRemove.forEach(taskManager::removeReminder);
                updateTasks.run();
            }
        });
        menu.add(removeReminderItem);
        menu.show(checklistList, x, y);
    }

    private void renameChecklist() {
        String oldName = selectedChecklistName;
        if (oldName == null) return;
        String rawNewName = JOptionPane.showInputDialog(this, "Enter new name:", oldName);
        String newName = TaskManager.validateInputWithError(rawNewName, "Checklist name");
        if (newName != null && !newName.equals(oldName)) {
            List<Task> allTasks = taskManager.getAllTasks();
            for (Task task : allTasks) {
                if (task.getChecklistName() != null && task.getChecklistName().equals(oldName)) {
                    task.setChecklistName(newName);
                    taskManager.updateTask(task);
                }
            }
            // Also update reminders
            List<Reminder> reminders = taskManager.getReminders();
            for (Reminder r : reminders) {
                if (r.getChecklistName().equals(oldName)) {
                    taskManager.removeReminder(r);
                    Reminder newReminder = new Reminder(newName, r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
                    taskManager.addReminder(newReminder);
                }
            }
            updateTasks.run();
            allChecklistNames.remove(oldName);  // Remove old name
            allChecklistNames.add(newName);     // Add new name
            taskManager.removeChecklistName(oldName);  // Remove old name from persistent storage
            taskManager.addChecklistName(newName);     // Add new name to persistent storage
            selectChecklist(newName);
        }
    }

    private void deleteChecklist() {
        String name = selectedChecklistName;
        if (name == null) return;
        Object[] options = {"Delete list", "Move to morning", "Move to evening", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "What to do with the tasks in '" + name + "'?", "Delete Checklist", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[3]);
        switch (choice) {
            case 0 -> {
                // Delete list
                List<Task> allTasks = taskManager.getAllTasks();
                for (Task task : allTasks) {
                    if (task.getChecklistName() != null && task.getChecklistName().equals(name)) {
                        taskManager.removeTask(task);
                    }
                }
            }
            case 1 -> // Move to morning
                moveTasksToType(name, TaskType.MORNING);
            case 2 -> // Move to evening
                moveTasksToType(name, TaskType.EVENING);
            case 3 -> // Cancel - do nothing
                {
                    return;
                }
            default -> // closed dialog or unexpected value - treat as cancel
                {
                    return;
                }
        }
        
        // Remove all reminders for this checklist
        List<Reminder> allReminders = taskManager.getReminders();
        allReminders.stream()
            .filter(reminder -> Objects.equals(reminder.getChecklistName(), name))
            .forEach(taskManager::removeReminder);
            
        allChecklistNames.remove(name);  // Remove from tracked checklists
        taskManager.removeChecklistName(name);  // Remove from persistent storage
        updateTasks();  // Refresh the local checklist list
        updateTasks.run();  // Update other panels
        // After deletion, select the first checklist if available
        if (listModel.size() > 0) {
            String firstChecklist = listModel.get(0);
            selectChecklist(firstChecklist);
            checklistList.setSelectedValue(firstChecklist, true);
        } else {
            selectChecklist(null);
            checklistList.clearSelection();
        }
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

    private void setReminder() {
        if (selectedChecklistName == null) return;

        // Check if a reminder already exists for this checklist
        List<Reminder> allReminders = taskManager.getReminders();
        Reminder existingReminder = allReminders.stream()
                .filter(r -> r.getChecklistName().equals(selectedChecklistName))
                .findFirst()
                .orElse(null);

        ReminderEditDialog dialog = new ReminderEditDialog(taskManager, selectedChecklistName, existingReminder, () -> {
            // Refresh this overview (updates right panel) and also notify outer panels
            updateTasks();
            updateTasks.run();
            // Ensure the edited checklist remains selected and focused
            if (selectedChecklistName != null) {
                checklistList.setSelectedValue(selectedChecklistName, true);
                checklistList.requestFocusInWindow();
                // Also try to restore focus to the right panel contents
                if (rightPanel != null) {
                    rightPanel.revalidate();
                    rightPanel.repaint();
                    if (rightPanel.getComponentCount() > 0) {
                        java.awt.Component comp = rightPanel.getComponent(0);
                        if (comp != null) comp.requestFocusInWindow();
                    }
                }
            }
        });
        dialog.setVisible(true);
    }
}