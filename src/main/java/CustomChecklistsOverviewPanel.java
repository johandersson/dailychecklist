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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
    private JList<Checklist> checklistList;
    private DefaultListModel<Checklist> listModel;
    private final transient TaskManager taskManager;
    private transient Runnable updateTasks;
    private JTextField newChecklistField;
    private JButton createButton;
    private JSplitPane splitPane;
    private JPanel rightPanel;
    private CustomChecklistPanel currentChecklistPanel;
    private Checklist selectedChecklist;
    private AddTaskPanel currentAddPanel;
    private Set<Checklist> allChecklists;
    private Map<String, CustomChecklistPanel> panelMap = new HashMap<>();

    @SuppressWarnings("this-escape")
    public CustomChecklistsOverviewPanel(TaskManager taskManager, Runnable updateTasks) {
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        this.allChecklists = new java.util.HashSet<>();
        initialize();
        // Listen for model changes and refresh overview
        try {
            taskManager.addTaskChangeListener(() -> javax.swing.SwingUtilities.invokeLater(this::updateTasks));
        } catch (Exception ignore) {}
    }

    private void initialize() {
        listModel = new DefaultListModel<>();
        checklistList = new JList<>(listModel);
        checklistList.setCellRenderer(new ChecklistCellRenderer(taskManager));
        checklistList.setSelectionBackground(new java.awt.Color(184, 207, 229)); // Same as task lists
        checklistList.setSelectionForeground(java.awt.Color.BLACK);
        checklistList.setTransferHandler(new ChecklistListTransferHandler(listModel, taskManager, this::updateTasks));
        checklistList.setDropMode(DropMode.ON);
        checklistList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Checklist selected = checklistList.getSelectedValue();
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
                        selectedChecklist = checklistList.getSelectedValue();
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
                        selectedChecklist = checklistList.getSelectedValue();
                        showChecklistPopup(e.getX(), e.getY());
                    }
                }
            }
        });

        newChecklistField = new JTextField(20);
        newChecklistField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    createNewChecklist();
                }
            }
        });
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
        
        // Initialize with existing checklists
        allChecklists.addAll(taskManager.getCustomChecklists());
        updateChecklistList();
    }

    private void createNewChecklist() {
        String rawName = newChecklistField.getText();
        String name = TaskManager.validateInputWithError(rawName, "Checklist name");
        if (name == null) {
            return;
        }
        Set<Checklist> existing = taskManager.getCustomChecklists();
        boolean nameExists = existing.stream().anyMatch(c -> name.equals(c.getName()));
        if (nameExists) {
            JOptionPane.showMessageDialog(this, "Checklist name already exists.");
            return;
        }
        // Remove any orphaned tasks with this name
        List<Task> allTasks = taskManager.getAllTasks();
        for (Task task : allTasks) {
            if (task.getChecklistId() != null) {
                Checklist taskChecklist = taskManager.getCustomChecklists().stream()
                    .filter(c -> task.getChecklistId().equals(c.getId()))
                    .findFirst().orElse(null);
                if (taskChecklist != null && name.equals(taskChecklist.getName())) {
                    taskManager.removeTask(task);
                }
            }
        }
        newChecklistField.setText("");
        Checklist newChecklist = new Checklist(name);
        allChecklists.add(newChecklist);  // Track the new checklist
        taskManager.addChecklist(newChecklist);  // Persist the checklist
        updateChecklistList();  // Update the list model first
        selectChecklist(newChecklist);  // Then select it
        updateTasks.run();  // Update other panels
        checklistList.setSelectedValue(newChecklist, true);
        // Ensure UI refreshes
        checklistList.revalidate();
        checklistList.repaint();
    }

    /**
     * Public method to select a checklist by name (used by reminders)
     */
    public void selectChecklistByName(String checklistName) {
        // Find checklist by name
        Checklist checklist = null;
        for (Checklist c : allChecklists) {
            if (checklistName.equals(c.getName())) {
                checklist = c;
                break;
            }
        }
        selectChecklist(checklist);
        if (checklist != null) {
            checklistList.setSelectedValue(checklist, true);
        }
    }

    private void selectChecklist(Checklist checklist) {
        selectedChecklist = checklist;
        if (currentChecklistPanel != null) {
            rightPanel.remove(currentChecklistPanel);
        }
        if (currentAddPanel != null) {
            rightPanel.remove(currentAddPanel);
        }
        if (checklist != null) {
            // Reuse existing panel from panelMap if available, otherwise create new one
            currentChecklistPanel = panelMap.get(checklist.getId());
            if (currentChecklistPanel == null) {
                currentChecklistPanel = new CustomChecklistPanel(taskManager, checklist, this::updateTasks);
                panelMap.put(checklist.getId(), currentChecklistPanel);
            }
            currentChecklistPanel.updateTasks();
            rightPanel.add(currentChecklistPanel);
            currentAddPanel = new AddTaskPanel(taskManager, tasks -> {
                if (currentChecklistPanel != null) {
                    currentChecklistPanel.updateTasks();
                    rightPanel.revalidate();
                    rightPanel.repaint();
                }
                updateTasks.run();
            }, selectedChecklist != null ? selectedChecklist.getName() : null);
            rightPanel.add(currentAddPanel);
        } else {
            currentChecklistPanel = null;
            currentAddPanel = null;
        }
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    public void updateTasks() {
        // Preserve selection
        Checklist previousSelection = checklistList.getSelectedValue();

        updateChecklistList();

        // Restore selection
        if (previousSelection != null && listModel.contains(previousSelection)) {
            checklistList.setSelectedValue(previousSelection, true);
        }

        if (currentChecklistPanel != null) {
            currentChecklistPanel.updateTasks();
            currentChecklistPanel.requestSelectionFocus();
        }
        // Update all panels to reflect any changes
        for (CustomChecklistPanel panel : panelMap.values()) {
            if (panel != null) panel.updateTasks();
        }
        checklistList.revalidate();
        checklistList.repaint();
    }

    private void updateChecklistList() {
        listModel.clear();
        Set<Checklist> checklists = taskManager.getCustomChecklists();
        // Add all checklists
        for (Checklist checklist : checklists) {
            listModel.addElement(checklist);
            allChecklists.add(checklist);  // Ensure we track all existing checklists
        }

        // Clean up panelMap - remove panels for checklists that no longer exist
        panelMap.keySet().removeIf(checklistId -> allChecklists.stream().noneMatch(c -> checklistId.equals(c.getId())));
    }

    private void showChecklistPopup(int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.addActionListener(e -> renameChecklist());
        menu.add(renameItem);
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> deleteChecklist());
        menu.add(deleteItem);
        // If a reminder exists for this checklist, label should indicate edit
        boolean hasReminderForSelected = false;
        if (selectedChecklist != null) {
            hasReminderForSelected = taskManager.getReminders().stream().anyMatch(r -> r.getChecklistName().equals(selectedChecklist.getName()));
        }
        JMenuItem addReminderItem = new JMenuItem(hasReminderForSelected ? "Edit Reminder" : "Set Reminder");
        addReminderItem.addActionListener(e -> setReminder());
        menu.add(addReminderItem);
        // Only offer remove when there actually are reminders
        if (selectedChecklist != null && taskManager.getReminders().stream().anyMatch(r -> r.getChecklistName().equals(selectedChecklist.getName()))) {
            JMenuItem removeReminderItem = new JMenuItem("Remove Reminder");
            removeReminderItem.addActionListener(e -> {
                if (selectedChecklist == null) return;
                List<Reminder> allReminders = taskManager.getReminders();
                List<Reminder> toRemove = allReminders.stream()
                        .filter(r -> r.getChecklistName().equals(selectedChecklist.getName()))
                        .toList();
                if (toRemove.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No reminders to remove for '" + selectedChecklist.getName() + "'.");
                    return;
                }
                int res = JOptionPane.showConfirmDialog(this, "Remove reminder(s) for '" + selectedChecklist.getName() + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    toRemove.forEach(taskManager::removeReminder);
                    // Restore focus to the task list
                    if (rightPanel != null && rightPanel.getComponentCount() > 0) {
                        java.awt.Component c = rightPanel.getComponent(0);
                        if (c instanceof CustomChecklistPanel panel) {
                                panel.getTaskList().requestFocusInWindow();
                            }
                    }
                }
            });
            menu.add(removeReminderItem);
        }
        menu.show(checklistList, x, y);
    }

    private void renameChecklist() {
        if (selectedChecklist == null) return;
        String oldName = selectedChecklist.getName();
        String rawNewName = JOptionPane.showInputDialog(this, "Enter new name:", oldName);
        String newName = TaskManager.validateInputWithError(rawNewName, "Checklist name");
        if (newName != null && !newName.equals(oldName)) {
            // Check if name already exists
            boolean nameExists = allChecklists.stream().anyMatch(c -> newName.equals(c.getName()) && !selectedChecklist.getId().equals(c.getId()));
            if (nameExists) {
                JOptionPane.showMessageDialog(this, "Checklist name already exists.");
                return;
            }

            // Update the checklist name
            taskManager.getCustomChecklists().stream()
                .filter(c -> selectedChecklist.getId().equals(c.getId()))
                .findFirst()
                .ifPresent(c -> taskManager.updateChecklistName(c, newName));

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
            // Update panelMap key
            if (panelMap.containsKey(selectedChecklist.getId())) {
                CustomChecklistPanel panel = panelMap.remove(selectedChecklist.getId());
                panelMap.put(selectedChecklist.getId(), panel);
            }
            selectChecklist(selectedChecklist);
        }
    }

    private void deleteChecklist() {
        if (selectedChecklist == null) return;
        String name = selectedChecklist.getName();
        Object[] options = {"Delete list", "Move to morning", "Move to evening", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "What to do with the tasks in '" + name + "'?", "Delete Checklist", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[3]);
        switch (choice) {
            case 0 -> {
                // Delete list
                List<Task> allTasks = taskManager.getAllTasks();
                for (Task task : allTasks) {
                    if (task.getChecklistId() != null && task.getChecklistId().equals(selectedChecklist.getId())) {
                        taskManager.removeTask(task);
                    }
                }
            }
            case 1 -> // Move to morning
                moveTasksToType(selectedChecklist.getId(), TaskType.MORNING);
            case 2 -> // Move to evening
                moveTasksToType(selectedChecklist.getId(), TaskType.EVENING);
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

        allChecklists.remove(selectedChecklist);  // Remove from tracked checklists
        taskManager.removeChecklist(selectedChecklist);  // Remove from persistent storage
        panelMap.remove(selectedChecklist.getId());  // Remove panel from cache
        updateTasks();  // Refresh the local checklist list
        updateTasks.run();  // Update other panels
        // After deletion, select the first checklist if available
        if (listModel.size() > 0) {
            Checklist firstChecklist = listModel.get(0);
            selectChecklist(firstChecklist);
            checklistList.setSelectedValue(firstChecklist, true);
        } else {
            selectChecklist(null);
            checklistList.clearSelection();
        }
    }

    private void moveTasksToType(String checklistId, TaskType type) {
        List<Task> allTasks = taskManager.getAllTasks();
        for (Task task : allTasks) {
            if (task.getChecklistId() != null && task.getChecklistId().equals(checklistId)) {
                task.setType(type);
                task.setChecklistId(null);
                task.setDone(false);
                task.setDoneDate(null);
                taskManager.updateTask(task);
            }
        }
    }

    private void setReminder() {
        if (selectedChecklist == null) return;

        // Check if a reminder already exists for this checklist
        List<Reminder> allReminders = taskManager.getReminders();
        Reminder existingReminder = allReminders.stream()
                .filter(r -> r.getChecklistName().equals(selectedChecklist.getName()))
                .findFirst()
                .orElse(null);

        // Save logical selection state
        final Checklist checklistToRestore = selectedChecklist;
        final String selectedTaskId;
        String _tmpSelectedTaskId = null;
                if (rightPanel != null && rightPanel.getComponentCount() > 0) {
            java.awt.Component c = rightPanel.getComponent(0);
                if (c instanceof CustomChecklistPanel panel) {
                    try {
                        Task sel = panel.getTaskList().getSelectedValue();
                        _tmpSelectedTaskId = sel == null ? null : sel.getId();
                    } catch (Exception ignore) {}
                }
            }
        selectedTaskId = _tmpSelectedTaskId;

        ReminderEditDialog dialog = new ReminderEditDialog(taskManager, selectedChecklist.getName(), existingReminder, null);
        dialog.setVisible(true);

        // After dialog returns (modal), reapply selection and focus
        if (checklistToRestore != null) {
            checklistList.setSelectedValue(checklistToRestore, true);
            // Restore selection and focus on right panel's task list if available
                if (rightPanel != null && rightPanel.getComponentCount() > 0) {
                java.awt.Component c = rightPanel.getComponent(0);
                if (c instanceof CustomChecklistPanel panel) {
                    JList<Task> list = panel.getTaskList();
                    if (selectedTaskId != null) {
                        // Try to restore selected task by id
                        for (int i = 0; i < list.getModel().getSize(); i++) {
                            Task t = list.getModel().getElementAt(i);
                            if (t != null && t.getId() != null && t.getId().equals(selectedTaskId)) {
                                list.setSelectedIndex(i);
                                list.ensureIndexIsVisible(i);
                                break;
                            }
                        }
                    }
                    list.requestFocusInWindow();
                }
            }
        }
    }

    public Map<String, CustomChecklistPanel> getPanelMap() {
        return panelMap;
    }
}