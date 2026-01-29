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
import java.util.Objects;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class CustomChecklistPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JList<Task> customTaskList;
    private DefaultListModel<Task> customListModel;
    private transient TaskManager taskManager;
    private Checklist checklist;
    private transient Runnable updateAllPanels;
    

    public CustomChecklistPanel(TaskManager taskManager, Checklist checklist) {
        this(taskManager, checklist, null);
    }

    @SuppressWarnings("this-escape")
    public CustomChecklistPanel(TaskManager taskManager, Checklist checklist, Runnable updateAllPanels) {
        this.taskManager = taskManager;
        this.checklist = checklist;
        this.updateAllPanels = updateAllPanels;
        initialize();
        // Listen for model changes and refresh UI
        taskManager.addTaskChangeListener(() -> {
            java.awt.Component focused = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            boolean wasListFocused = focused == customTaskList;
            updateTasks();
            if (wasListFocused || (focused != null && SwingUtilities.isDescendingFrom(focused, this))) {
                requestSelectionFocus();
            } else if (focused != null && focused.isShowing() && focused.isFocusable()) {
                focused.requestFocusInWindow();
            }
        });
    }

    private void initialize() {
        customListModel = new DefaultListModel<>();
        customTaskList = createTaskList(customListModel);
        JPanel customPanel = createPanel(customTaskList);
        setLayout(new BorderLayout());
        add(customPanel, BorderLayout.CENTER);
    }

    private JList<Task> createTaskList(DefaultListModel<Task> listModel) {
        JList<Task> taskList = TaskListFactory.createTaskList(listModel, taskManager, checklist.getName(), updateAllPanels, null, null);
        installTaskListMouseListener(taskList);
        // Allow deleting multiple selected tasks via Delete key
        taskList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    deleteSelectedTasks(taskList);
                }
            }
        });
        return taskList;
    }

    private void installTaskListMouseListener(JList<Task> taskList) {
        taskList.addMouseListener(new MouseAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void mouseClicked(MouseEvent e) {
                JList<Task> list = (JList<Task>) e.getSource();
                int index = list.locationToIndex(e.getPoint());
                if (index < 0) return;

                java.awt.Rectangle cellBounds = list.getCellBounds(index, index);
                Task task = list.getModel().getElementAt(index);
                int checkboxX = cellBounds.x + UiLayout.CHECKBOX_X + (task.getParentId() != null ? UiLayout.SUBTASK_INDENT : 0);
                int checkboxY = cellBounds.y + cellBounds.height / 2 - UiLayout.CHECKBOX_SIZE / 2;
                int checkboxSize = UiLayout.CHECKBOX_SIZE;
                boolean onCheckbox = e.getPoint().x >= checkboxX && e.getPoint().x <= checkboxX + checkboxSize &&
                                     e.getPoint().y >= checkboxY && e.getPoint().y <= checkboxY + checkboxSize;

                if (SwingUtilities.isRightMouseButton(e)) {
                    ensureSelection(list, index);
                    showContextMenu(e, list, index);
                } else if (onCheckbox && e.getClickCount() == 1) {
                    toggleTaskDone(list, index, cellBounds);
                } else if (e.getClickCount() == 2) {
                    renameTaskInline(list, index, cellBounds);
                }
            }
        });
    }

    // Helper: ensure the clicked index is selected before showing context menu
    private void ensureSelection(JList<Task> list, int index) {
        if (!list.isSelectedIndex(index)) list.setSelectedIndex(index);
    }

    // Helper: toggle done state for a task at the given index
    private void toggleTaskDone(JList<Task> list, int index, java.awt.Rectangle cellBounds) {
        Task task = list.getModel().getElementAt(index);
        TaskDoneToggler.toggle(taskManager, task, this::updateTasks);
        list.repaint(cellBounds);
    }

    // Helper: rename a task using the existing inline prompt
    private void renameTaskInline(JList<Task> list, int index, java.awt.Rectangle cellBounds) {
        Task task = list.getModel().getElementAt(index);
        String prompt = (task.getParentId() != null) ? "Enter new name for subtask:" : "Enter new name for task:";
        String rawNewName = JOptionPane.showInputDialog(CustomChecklistPanel.this, prompt, task.getName());
        String newName = TaskManager.validateInputWithError(rawNewName, "Task name");
        if (newName != null) {
            task.setName(newName);
            taskManager.updateTask(task);
            list.repaint(cellBounds);
        }
    }

    private void showContextMenu(MouseEvent e, JList<Task> list, int index) {
        JPopupMenu contextMenu = new JPopupMenu();
        int[] selected = list.getSelectedIndices();
        if (selected.length > 1) {
            // When multiple items are selected, only show the multi-select delete option
            addMultiSelectionMenuItems(contextMenu, list);
            contextMenu.show(list, e.getX(), e.getY());
            return;
        }

        // Single selection: show full context menu
        contextMenu.add(createAddSubtaskMenuItem(list, index));
        contextMenu.add(createEditMenuItem(list, index));
        contextMenu.add(createRemoveMenuItem(list, index));
        contextMenu.add(createStartFocusTimerItem(list, index));
        contextMenu.add(createSetTaskReminderItem(list, index));
        contextMenu.add(createRemoveTaskReminderItem(list, index));
        contextMenu.show(list, e.getX(), e.getY());
    }

    // Adds delete selected when multiple items are selected
    private void addMultiSelectionMenuItems(JPopupMenu menu, JList<Task> list) {
        int[] selectedIndices = list.getSelectedIndices();
        if (selectedIndices.length > 1) {
            JMenuItem deleteSelectedItem = new JMenuItem("Delete Selected Tasks");
            deleteSelectedItem.addActionListener(event -> deleteSelectedTasks(list));
            menu.add(deleteSelectedItem);
            menu.addSeparator();
        }
    }

    private void deleteSelectedTasks(JList<Task> list) {
        java.util.List<Task> tasksToDelete = getTasksFromSelection(list);
        if (tasksToDelete.isEmpty()) return;

        if (!DeleteConfirmationDialog.showConfirm(this, taskManager, tasksToDelete)) return;

        java.util.List<Task> extraSubs = collectExtraSubtasks(tasksToDelete);
        performDeletion(tasksToDelete, extraSubs);
    }

    private java.util.List<Task> getTasksFromSelection(JList<Task> list) {
        int[] selectedIndices = list.getSelectedIndices();
        java.util.List<Task> tasks = new java.util.ArrayList<>();
        for (int index : selectedIndices) {
            tasks.add(list.getModel().getElementAt(index));
        }
        return tasks;
    }

    private java.util.List<Task> collectExtraSubtasks(java.util.List<Task> tasksToDelete) {
        java.util.Set<String> selectedIds = new java.util.HashSet<>();
        for (Task t : tasksToDelete) selectedIds.add(t.getId());
        java.util.List<Task> extraSubs = new java.util.ArrayList<>();
        for (Task t : tasksToDelete) {
            java.util.List<Task> subs = taskManager.getSubtasks(t.getId());
            if (subs != null) {
                for (Task s : subs) {
                    if (!selectedIds.contains(s.getId())) extraSubs.add(s);
                }
            }
        }
        return extraSubs;
    }

    private void performDeletion(java.util.List<Task> tasksToDelete, java.util.List<Task> extraSubs) {
        for (Task t : extraSubs) taskManager.removeTask(t);
        for (Task task : tasksToDelete) taskManager.removeTask(task);
        updateTasks();
    }

    // Confirmation handled by shared DeleteConfirmationDialog helper

    // Place at the end of the class, before the final closing brace
    private JMenuItem createAddSubtaskMenuItem(JList<Task> list, int index) {
        JMenuItem addSubtaskItem = new JMenuItem("Add Subtask");
        // Use authoritative task instance from TaskManager to avoid stale model objects
        Task modelParent = list.getModel().getElementAt(index);
        Task parent = taskManager.getTaskById(modelParent.getId());
        if (parent == null) parent = modelParent;
        if (parent.getParentId() != null) {
            // Disable adding a subtask to a subtask (only one level supported)
            addSubtaskItem.setEnabled(false);
            addSubtaskItem.setToolTipText("<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>Cannot add a subtask to a subtask</p></html>");
        } else {
            addSubtaskItem.addActionListener(event -> {
                Task p = taskManager.getTaskById(modelParent.getId());
                if (p == null) p = modelParent;
                Task pForDialog = (p != null) ? p : modelParent;
                String prompt = "Subtask to " + (pForDialog != null ? pForDialog.getName() : "");
                String subtaskName = JOptionPane.showInputDialog(this, prompt);
                if (subtaskName != null && !subtaskName.trim().isEmpty()) {
                    Task subtask = new Task(subtaskName.trim(), p.getType(), p.getWeekday(), p.getChecklistId(), p.getId());
                    taskManager.addTask(subtask);
                    updateTasks();
                }
            });
        }
        return addSubtaskItem;
    }

    private JMenuItem createEditMenuItem(JList<Task> list, int index) {
        JMenuItem editItem = new JMenuItem("Rename task");
        editItem.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            String prompt = (task.getParentId() != null) ? "Enter new name for subtask:" : "Enter new name for task:";
            String rawNewName = JOptionPane.showInputDialog(CustomChecklistPanel.this, prompt, task.getName());
            String newName = TaskManager.validateInputWithError(rawNewName, "Task name");
            if (newName != null) {
                task.setName(newName);
                taskManager.updateTask(task);
                list.repaint(list.getCellBounds(index, index));
            }
        });
        return editItem;
    }
                
    private JMenuItem createRemoveMenuItem(JList<Task> list, int index) {
        JMenuItem removeItem = new JMenuItem("Remove task");
        removeItem.addActionListener(event -> removeTask(list, index));
        return removeItem;
    }

    private JMenuItem createStartFocusTimerItem(JList<Task> list, int index) {
        JMenuItem item = new JMenuItem("Start Focus Timer on task");
        item.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            FocusTimer.getInstance().startFocusTimer(task.getName(), "5 minutes");
        });
        return item;
    }

    private JMenuItem createSetTaskReminderItem(JList<Task> list, int index) {
        JMenuItem item = new JMenuItem("Set task reminder");
        item.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            Reminder existing = taskManager.getReminders().stream()
                .filter(r -> Objects.equals(r.getChecklistName(), checklist.getName()))
                .filter(r -> Objects.equals(r.getTaskId(), task.getId()))
                .findFirst().orElse(null);
            ReminderEditDialog dialog = new ReminderEditDialog(taskManager, checklist.getName(), existing, () -> {
                if (updateAllPanels != null) updateAllPanels.run();
                updateTasks();
            }, task.getId());
            dialog.setVisible(true);
        });
        return item;
    }

    private JMenuItem createRemoveTaskReminderItem(JList<Task> list, int index) {
        JMenuItem item = new JMenuItem("Remove task reminder");
        item.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            Reminder existing = taskManager.getReminders().stream()
                .filter(r -> Objects.equals(r.getChecklistName(), checklist.getName()))
                .filter(r -> Objects.equals(r.getTaskId(), task.getId()))
                .findFirst().orElse(null);
            if (existing != null) {
                int res = JOptionPane.showConfirmDialog(this, "Remove reminder for task '" + task.getName() + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    taskManager.removeReminder(existing);
                    if (updateAllPanels != null) updateAllPanels.run();
                    updateTasks();
                }
            } else {
                JOptionPane.showMessageDialog(this, "No reminder set for this task.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        return item;
    }

    private void removeTask(JList<Task> list, int index) {
        Task task = list.getModel().getElementAt(index);
        java.util.List<Task> subs = taskManager.getSubtasks(task.getId());
        if (subs != null && !subs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("The task '").append(task.getName()).append("' has ").append(subs.size()).append(" subtask(s):\n\n");
            int shown = 0;
            for (Task s : subs) {
                if (shown++ >= 10) break;
                sb.append(" - ").append(s.getName()).append("\n");
            }
            if (subs.size() > 10) sb.append("... and ").append(subs.size() - 10).append(" more\n");
            sb.append("\nDeleting the parent will also delete these subtasks. Continue?");
            Object[] options = {"Delete task and subtasks", "Cancel"};
            int res = JOptionPane.showOptionDialog(this, sb.toString(), "Confirm Deletion",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (res == 0) {
                // delete subtasks first, then parent
                for (Task s : subs) taskManager.removeTask(s);
                taskManager.removeTask(task);
                updateTasks();
            }
        } else {
            int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove the task '" + task.getName() + "'?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                taskManager.removeTask(task);
                updateTasks();
            }
        }
    }

    private JPanel createPanel(JList<Task> taskList) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        
        JScrollPane scrollPane = new JScrollPane(taskList);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
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
        // Preserve selections before updating
        java.util.List<Task> selectedTasks = customTaskList.getSelectedValuesList();

        // Load checklist tasks off the EDT and sync model incrementally
        javax.swing.SwingWorker<List<Task>, Void> worker = new javax.swing.SwingWorker<>() {
            @Override
            protected List<Task> doInBackground() throws Exception {
                return taskManager.getTasks(TaskType.CUSTOM, checklist);
            }

            @Override
            protected void done() {
                try {
                        List<Task> tasks = get();
                        // For custom checklists we want parents followed by their direct subtasks.
                        // Use TaskManager's cached, pre-sorted subtasks to avoid per-update sorting.
                        java.util.List<Task> parents = new java.util.ArrayList<>();
                        for (Task t : tasks) {
                            if (t.getParentId() == null) {
                                parents.add(t);
                            }
                        }
                        java.util.List<Task> desired = new java.util.ArrayList<>();
                        for (Task p : parents) {
                            desired.add(p);
                            java.util.List<Task> subs = taskManager.getSubtasksSorted(p.getId());
                            if (subs != null && !subs.isEmpty()) {
                                // Filter to this checklist to be safe (subtasks should normally share checklist)
                                for (Task s : subs) {
                                    if (p.getChecklistId() == null) {
                                        if (s.getChecklistId() == null) desired.add(s);
                                    } else if (p.getChecklistId().equals(s.getChecklistId())) {
                                        desired.add(s);
                                    }
                                }
                            }
                        }
                        // Precompute display strings (include checklist info for custom lists)
                        DisplayPrecomputer.precomputeForList(desired, taskManager, true);
                        TaskUpdater.syncModel(customListModel, desired);

                    // Restore selections after updating
                    for (Task selectedTask : selectedTasks) {
                        for (int i = 0; i < customListModel.getSize(); i++) {
                            if (customListModel.getElementAt(i).equals(selectedTask)) {
                                customTaskList.addSelectionInterval(i, i);
                                break;
                            }
                        }
                    }

                    // Reminder status panel was removed; nothing to populate here
                    customTaskList.revalidate();
                    customTaskList.repaint();
                } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                    java.util.logging.Logger.getLogger(CustomChecklistPanel.class.getName()).log(java.util.logging.Level.SEVERE, "Error loading custom checklist tasks", e);
                }
            }
        };
        worker.execute();
    }

    /**
     * Request focus for the task list so the selected checklist keeps focus after external actions.
     */
    public void requestSelectionFocus() {
        if (customTaskList != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    boolean ok = customTaskList.requestFocusInWindow();
                    if (!ok) customTaskList.grabFocus();
                    if (customTaskList.getSelectedIndex() >= 0) {
                        customTaskList.ensureIndexIsVisible(customTaskList.getSelectedIndex());
                    }
                } catch (RuntimeException ex) {
                    try { customTaskList.requestFocusInWindow(); } catch (RuntimeException ignore) {}
                }
            });
        }
    }

    public void scrollToTask(Task task) {
        for (int i = 0; i < customListModel.getSize(); i++) {
            if (customListModel.getElementAt(i).getId().equals(task.getId())) {
                customTaskList.setSelectedIndex(i);
                customTaskList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    /**
     * Expose the internal task list for diagnostics and targeted focus checks.
     */
    public JList<Task> getTaskList() {
        return customTaskList;
    }
}