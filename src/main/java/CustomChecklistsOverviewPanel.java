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
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
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
        selectChecklist(name);
        updateTasks.run();
        updateTasks();
        checklistList.setSelectedValue(name, true);
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
        if (checklistName != null) {
            currentChecklistPanel = new CustomChecklistPanel(taskManager, new TaskUpdater(), checklistName);
            currentChecklistPanel.updateTasks();
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
        String rawTaskName = JOptionPane.showInputDialog(this, "Enter task name:");
        String taskName = TaskManager.validateInputWithError(rawTaskName, "Task name");
        if (taskName != null) {
            Task newTask = new Task(taskName, TaskType.CUSTOM, null, selectedChecklistName);
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
        JMenuItem addReminderItem = new JMenuItem("Add Reminder");
        addReminderItem.addActionListener(e -> addReminder());
        menu.add(addReminderItem);
        JMenuItem editRemindersItem = new JMenuItem("View/Edit Reminders");
        editRemindersItem.addActionListener(e -> editReminders());
        menu.add(editRemindersItem);
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

    private void addReminder() {
        if (selectedChecklistName == null) return;

        JDialog dialog = new JDialog();
        dialog.setTitle("Add Reminder for " + selectedChecklistName);
        dialog.setModal(true);
        dialog.setLayout(new GridLayout(6, 2, 5, 5));

        LocalDateTime now = LocalDateTime.now();
        JComboBox<Integer> yearBox = new JComboBox<>(IntStream.rangeClosed(now.getYear(), now.getYear() + 10).boxed().toArray(Integer[]::new));
        yearBox.setSelectedItem(now.getYear());
        JComboBox<Integer> monthBox = new JComboBox<>(IntStream.rangeClosed(1, 12).boxed().toArray(Integer[]::new));
        monthBox.setSelectedItem(now.getMonthValue());
        JComboBox<Integer> dayBox = new JComboBox<>(IntStream.rangeClosed(1, 31).boxed().toArray(Integer[]::new));
        dayBox.setSelectedItem(now.getDayOfMonth());
        JComboBox<Integer> hourBox = new JComboBox<>(IntStream.rangeClosed(0, 23).boxed().toArray(Integer[]::new));
        hourBox.setSelectedItem(now.getHour());
        JComboBox<Integer> minuteBox = new JComboBox<>(IntStream.rangeClosed(0, 59).boxed().toArray(Integer[]::new));
        minuteBox.setSelectedItem(now.getMinute());

        dialog.add(new JLabel("Year:"));
        dialog.add(yearBox);
        dialog.add(new JLabel("Month:"));
        dialog.add(monthBox);
        dialog.add(new JLabel("Day:"));
        dialog.add(dayBox);
        dialog.add(new JLabel("Hour:"));
        dialog.add(hourBox);
        dialog.add(new JLabel("Minute:"));
        dialog.add(minuteBox);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            try {
                int year = (Integer) yearBox.getSelectedItem();
                int month = (Integer) monthBox.getSelectedItem();
                int day = (Integer) dayBox.getSelectedItem();
                int hour = (Integer) hourBox.getSelectedItem();
                int minute = (Integer) minuteBox.getSelectedItem();

                // Validate date
                java.time.LocalDateTime.of(year, month, day, hour, minute);

                Reminder reminder = new Reminder(selectedChecklistName, year, month, day, hour, minute);
                taskManager.addReminder(reminder);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Reminder added successfully.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date/time. Please check your input.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(okButton);
        dialog.add(cancelButton);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void editReminders() {
        if (selectedChecklistName == null) return;

        List<Reminder> allReminders = taskManager.getReminders();
        List<Reminder> checklistReminders = allReminders.stream()
                .filter(r -> r.getChecklistName().equals(selectedChecklistName))
                .toList();

        if (checklistReminders.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No reminders found for this checklist.", "No Reminders", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog();
        dialog.setTitle("Edit Reminders for " + selectedChecklistName);
        dialog.setModal(true);
        dialog.setLayout(new BorderLayout());

        DefaultListModel<String> reminderModel = new DefaultListModel<>();
        for (Reminder r : checklistReminders) {
            reminderModel.addElement(String.format("%d-%02d-%02d %02d:%02d",
                    r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute()));
        }

        JList<String> reminderList = new JList<>(reminderModel);
        JScrollPane scrollPane = new JScrollPane(reminderList);

        JPanel buttonPanel = new JPanel();
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton closeButton = new JButton("Close");

        editButton.addActionListener(e -> {
            int index = reminderList.getSelectedIndex();
            if (index >= 0) {
                Reminder reminder = checklistReminders.get(index);
                editReminderDialog(reminder, () -> {
                    // Refresh the list after editing
                    List<Reminder> updatedReminders = taskManager.getReminders().stream()
                            .filter(r -> r.getChecklistName().equals(selectedChecklistName))
                            .toList();
                    reminderModel.clear();
                    for (Reminder r : updatedReminders) {
                        reminderModel.addElement(String.format("%d-%02d-%02d %02d:%02d",
                                r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute()));
                    }
                });
            }
        });

        deleteButton.addActionListener(e -> {
            int index = reminderList.getSelectedIndex();
            if (index >= 0) {
                Reminder reminder = checklistReminders.get(index);
                int choice = JOptionPane.showConfirmDialog(dialog, "Delete this reminder?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    taskManager.removeReminder(reminder);
                    reminderModel.remove(index);
                    checklistReminders.remove(index);
                }
            }
        });

        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(300, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void editReminderDialog(Reminder reminder, Runnable onSave) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Edit Reminder");
        dialog.setModal(true);
        dialog.setLayout(new GridLayout(6, 2, 5, 5));

        JComboBox<Integer> yearBox = new JComboBox<>(IntStream.rangeClosed(2020, 2030).boxed().toArray(Integer[]::new));
        yearBox.setSelectedItem(reminder.getYear());
        JComboBox<Integer> monthBox = new JComboBox<>(IntStream.rangeClosed(1, 12).boxed().toArray(Integer[]::new));
        monthBox.setSelectedItem(reminder.getMonth());
        JComboBox<Integer> dayBox = new JComboBox<>(IntStream.rangeClosed(1, 31).boxed().toArray(Integer[]::new));
        dayBox.setSelectedItem(reminder.getDay());
        JComboBox<Integer> hourBox = new JComboBox<>(IntStream.rangeClosed(0, 23).boxed().toArray(Integer[]::new));
        hourBox.setSelectedItem(reminder.getHour());
        JComboBox<Integer> minuteBox = new JComboBox<>(IntStream.rangeClosed(0, 59).boxed().toArray(Integer[]::new));
        minuteBox.setSelectedItem(reminder.getMinute());

        dialog.add(new JLabel("Year:"));
        dialog.add(yearBox);
        dialog.add(new JLabel("Month:"));
        dialog.add(monthBox);
        dialog.add(new JLabel("Day:"));
        dialog.add(dayBox);
        dialog.add(new JLabel("Hour:"));
        dialog.add(hourBox);
        dialog.add(new JLabel("Minute:"));
        dialog.add(minuteBox);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            try {
                int year = (Integer) yearBox.getSelectedItem();
                int month = (Integer) monthBox.getSelectedItem();
                int day = (Integer) dayBox.getSelectedItem();
                int hour = (Integer) hourBox.getSelectedItem();
                int minute = (Integer) minuteBox.getSelectedItem();

                // Validate date
                java.time.LocalDateTime.of(year, month, day, hour, minute);

                // Remove old reminder and add new one
                taskManager.removeReminder(reminder);
                Reminder newReminder = new Reminder(reminder.getChecklistName(), year, month, day, hour, minute);
                taskManager.addReminder(newReminder);

                dialog.dispose();
                onSave.run();
                JOptionPane.showMessageDialog(this, "Reminder updated successfully.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date/time. Please check your input.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(saveButton);
        dialog.add(cancelButton);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}