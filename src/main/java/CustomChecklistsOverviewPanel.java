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
    private final transient Runnable updateTasks;
    private JTextField newChecklistField;
    private JButton createButton;
    private JSplitPane splitPane;
    private JPanel rightPanel;
    private CustomChecklistPanel currentChecklistPanel;
    private Checklist selectedChecklist;
    private AddTaskPanel currentAddPanel;
    private final Set<Checklist> allChecklists;
    private final Map<String, CustomChecklistPanel> panelMap = new HashMap<>();

    @SuppressWarnings("this-escape")
    public CustomChecklistsOverviewPanel(TaskManager taskManager, Runnable updateTasks) {
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        this.allChecklists = new java.util.HashSet<>();
        initialize();
        // Listen for model changes and refresh overview
        taskManager.addTaskChangeListener(() -> javax.swing.SwingUtilities.invokeLater(this::updateTasks));
    }

    private void initialize() {
        listModel = new DefaultListModel<>();
        checklistList = new JList<>(listModel) {
            private static final long serialVersionUID = 1L;
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int idx = locationToIndex(e.getPoint());
                if (idx < 0) return super.getToolTipText(e);
                java.awt.Rectangle cb = getCellBounds(idx, idx);
                if (cb == null) return super.getToolTipText(e);
                int relX = e.getX() - cb.x;
                int cellW = cb.width;
                // IconListCellRenderer reserves RIGHT_ICON_SPACE on right
                int rightAreaStart = cellW - IconListCellRenderer.RIGHT_ICON_SPACE;
                Checklist c = getModel().getElementAt(idx);
                if (c != null) {
                    // Find nearest reminder for this checklist
                    Reminder nearest = null;
                    if (taskManager != null) {
                        java.util.List<Reminder> reminders = taskManager.getReminders();
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        long bestDiff = Long.MAX_VALUE;
                        for (Reminder r : reminders) {
                            if (!java.util.Objects.equals(r.getChecklistName(), c.getName())) continue;
                            java.time.LocalDateTime dt = java.time.LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
                            long diff = Math.abs(java.time.Duration.between(now, dt).toMinutes());
                            if (diff < bestDiff) { bestDiff = diff; nearest = r; }
                        }
                    }
                    if (nearest != null) {
                        // Compute actual icon bounds so tooltip triggers where the icon is painted
                        javax.swing.Icon icon = IconCache.getReminderClockIcon(nearest.getHour(), nearest.getMinute(), ReminderClockIcon.State.FUTURE, true);
                        int iconW = icon != null ? icon.getIconWidth() : IconListCellRenderer.RIGHT_ICON_SPACE;
                        int iconStart = cellW - iconW - 6;
                        if (relX >= iconStart) {
                            String txt = String.format("Reminder: %04d-%02d-%02d %02d:%02d", nearest.getYear(), nearest.getMonth(), nearest.getDay(), nearest.getHour(), nearest.getMinute());
                            return "<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>" + txt + "</p></html>";
                        }
                    }
                }
                return super.getToolTipText(e);
            }
        };
        checklistList.setCellRenderer(new ChecklistCellRenderer(taskManager));
        javax.swing.ToolTipManager.sharedInstance().registerComponent(checklistList);
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
        // Flatter look: reduce extra titled/etched borders and padding
        topPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(topPanel, BorderLayout.NORTH);
        javax.swing.JScrollPane leftScroll = new JScrollPane(checklistList);
        leftScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        leftPanel.add(leftScroll, BorderLayout.CENTER);
        leftPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));

        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));

        javax.swing.JScrollPane rightScroll = new JScrollPane(rightPanel);
        rightScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightScroll);
        splitPane.setResizeWeight(0.3);
        splitPane.setDividerSize(4);
        splitPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
        
        // Initialize with existing checklists (load off EDT)
        loadChecklistsInBackground();
    }

    private void loadChecklistsInBackground() {
        javax.swing.SwingWorker<java.util.Set<Checklist>, Void> worker = new javax.swing.SwingWorker<>() {
            @Override
            protected java.util.Set<Checklist> doInBackground() throws Exception {
                return taskManager.getCustomChecklists();
            }

            @Override
            protected void done() {
                try {
                    java.util.Set<Checklist> checklists = get();
                    if (checklists != null) {
                        allChecklists.clear();
                        allChecklists.addAll(checklists);
                    }
                    updateChecklistList();
                } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                    java.util.logging.Logger.getLogger(CustomChecklistsOverviewPanel.class.getName()).log(java.util.logging.Level.SEVERE, "Error loading checklists", e);
                }
            }
        };
        worker.execute();
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
        scheduleUpdate();
    }

    private volatile boolean updateScheduled = false;

    private void scheduleUpdate() {
        if (updateScheduled) return;
        updateScheduled = true;
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                doUpdateTasks();
            } finally {
                updateScheduled = false;
            }
        });
    }

    private void doUpdateTasks() {
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
        java.util.List<Checklist> desired = new java.util.ArrayList<>(allChecklists);
        TaskUpdater.syncModel(listModel, desired);

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

        // Determine if checklist contains any tasks. Only offer "Move to" options when there are tasks.
        boolean hasTasks = taskManager.getAllTasks().stream().anyMatch(t -> selectedChecklist.getId().equals(t.getChecklistId()));

        Object[] options;
        if (hasTasks) {
            options = new Object[]{"Delete list", "Move to morning", "Move to evening", "Cancel"};
        } else {
            options = new Object[]{"Delete list", "Cancel"};
        }

        int defaultOption = options.length - 1; // Cancel index
        int choice = JOptionPane.showOptionDialog(this, "What to do with the tasks in '" + name + "'?", "Delete Checklist", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[defaultOption]);

        if (choice < 0 || choice == defaultOption) {
            // User closed dialog or chose Cancel
            return;
        }

        if (!hasTasks) {
            // Only possible choice is Delete list (index 0)
            if (choice == 0) {
                // Delete list: remove any tasks (none expected) defensively
                List<Task> allTasks = taskManager.getAllTasks();
                for (Task task : allTasks) {
                    if (task.getChecklistId() != null && task.getChecklistId().equals(selectedChecklist.getId())) {
                        taskManager.removeTask(task);
                    }
                }
            }
        } else {
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
                default -> {
                    return;
                }
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