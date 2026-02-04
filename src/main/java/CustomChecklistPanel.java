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
    private volatile boolean suppressTaskChangeListener = false;
    

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
            // Skip update if we've already handled it optimally
            if (suppressTaskChangeListener) return;
            
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
                    Task t = list.getModel().getElementAt(index);
                    if (t.getType() != TaskType.HEADING) toggleTaskDone(list, index, cellBounds);
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
        contextMenu.add(createCreateHeadingMenuItem(list, index));
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
        // Preserve selections before updating
        java.util.List<Task> selectedTasks = customTaskList.getSelectedValuesList();

        for (Task t : extraSubs) taskManager.removeTask(t);
        for (Task task : tasksToDelete) taskManager.removeTask(task);

        // Update synchronously
        List<Task> customs = taskManager.getTasks(TaskType.CUSTOM, checklist);
        List<Task> headings = taskManager.getTasks(TaskType.HEADING, checklist);
        java.util.Map<String, Task> headingByParent = new java.util.HashMap<>();
        if (headings != null) {
            for (Task h : headings) {
                if (h.getParentId() != null) headingByParent.put(h.getParentId(), h);
            }
        }

        // For custom checklists we want parents followed by their direct subtasks.
        java.util.List<Task> parents = new java.util.ArrayList<>();
        for (Task t : customs) {
            if (t.getParentId() == null) {
                parents.add(t);
            }
        }
        java.util.List<Task> desired = new java.util.ArrayList<>();
        for (Task p : parents) {
            Task heading = headingByParent.get(p.getId());
            if (heading != null) desired.add(heading);
            desired.add(p);
            java.util.List<Task> subs = taskManager.getSubtasksSorted(p.getId());
            if (subs != null && !subs.isEmpty()) {
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

        customTaskList.revalidate();
        customTaskList.repaint();
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
                
                // Show multi-line dialog for adding multiple subtasks
                java.util.List<String> subtaskNames = MultiSubtaskDialog.show(this, pForDialog.getName());
                
                if (!subtaskNames.isEmpty()) {
                    // Create subtask objects
                    java.util.List<Task> newSubtasks = new java.util.ArrayList<>();
                    for (String subtaskName : subtaskNames) {
                        Task subtask = new Task(subtaskName, p.getType(), p.getWeekday(), p.getChecklistId(), p.getId());
                        newSubtasks.add(subtask);
                    }
                    
                    if (!newSubtasks.isEmpty()) {
                            try {
                                // Suppress TaskChangeListener to avoid full reload
                                suppressTaskChangeListener = true;
                                
                                // Find parent index once
                                int parentIndex = -1;
                                for (int i = 0; i < customListModel.getSize(); i++) {
                                    Task cand = customListModel.get(i);
                                    if (cand != null && cand.getId().equals(p.getId())) {
                                        parentIndex = i;
                                        break;
                                    }
                                }
                                
                                // Find insertion point: after parent and existing subtasks
                                int insertIndex = parentIndex + 1;
                                while (insertIndex < customListModel.getSize()) {
                                    Task candidate = customListModel.get(insertIndex);
                                    if (candidate.getParentId() == null || !candidate.getParentId().equals(p.getId())) {
                                        break;
                                    }
                                    insertIndex++;
                                }
                                
                                // Precompute display data once for all new subtasks (batch operation)
                                DisplayPrecomputer.precomputeForList(newSubtasks, taskManager, true);
                                
                                // Add all subtasks
                                for (Task subtask : newSubtasks) {
                                    // Add to TaskManager (persists to repository)
                                    taskManager.addTask(subtask);
                                    
                                    // Insert at the correct position
                                    customListModel.add(insertIndex, subtask);
                                    insertIndex++;
                                }
                                
                                // Select the last added subtask
                                customTaskList.setSelectedIndex(insertIndex - 1);
                                customTaskList.ensureIndexIsVisible(insertIndex - 1);
                                list.repaint();
                            } finally {
                                // Re-enable TaskChangeListener after direct insertion
                                suppressTaskChangeListener = false;
                            }
                    }
                }
            });
        }
        return addSubtaskItem;
    }

    private JMenuItem createEditMenuItem(JList<Task> list, int index) {
        JMenuItem editItem = new JMenuItem("Rename task");
        editItem.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            String prompt;
            if (task.getType() == TaskType.HEADING) prompt = "Enter heading text:";
            else prompt = (task.getParentId() != null) ? "Enter new name for subtask:" : "Enter new name for task:";
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

    private JMenuItem createCreateHeadingMenuItem(JList<Task> list, int index) {
        JMenuItem item = new JMenuItem("Create Heading Above");
        // Determine authoritative parent and disable when not appropriate
        Task modelParent = list.getModel().getElementAt(index);
        Task tmpParent = taskManager.getTaskById(modelParent.getId());
        final Task parent = (tmpParent == null) ? modelParent : tmpParent;
        boolean alreadyHasHeading = false;
        try {
            java.util.List<Task> subs = taskManager.getSubtasks(parent.getId());
            for (Task s : subs) {
                if (s.getType() == TaskType.HEADING) { alreadyHasHeading = true; break; }
            }
        } catch (Exception ignored) {}
        boolean isSubtask = parent.getParentId() != null;
        boolean disabled = parent.getType() == TaskType.HEADING || alreadyHasHeading || isSubtask;
        if (disabled) {
            item.setEnabled(false);
            if (parent.getType() == TaskType.HEADING) {
                item.setToolTipText("Cannot create a heading above a heading");
            } else if (isSubtask) {
                item.setToolTipText("Cannot create a heading above a subtask");
            } else {
                item.setToolTipText("A heading already exists for this parent");
            }
            return item;
        }

        item.addActionListener(e -> {
            String raw = JOptionPane.showInputDialog(this, "Enter heading text:", "");
            String name = TaskManager.validateInputWithError(raw, "Heading text");
            if (name == null) return;
            Task heading = new Task(name, TaskType.HEADING, null, parent.getChecklistId(), parent.getId());
            taskManager.addTask(heading);
            updateTasks();
        });
        return item;
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

        // Check if we need a progress dialog (only for large checklists)
        boolean showProgress = false;
        try {
            List<Task> existingTasks = taskManager.getTasks(TaskType.CUSTOM, checklist);
            showProgress = existingTasks != null && existingTasks.size() > 30; // Show progress for lists with 30+ items
        } catch (Exception e) {
            // If we can't check, assume small
        }

        Runnable backgroundTask = () -> loadAndUpdateTasks(selectedTasks);

        if (showProgress) {
            // Show progress dialog for loading and updating large checklists
            RestoreProgressDialog progressDlg = new RestoreProgressDialog(SwingUtilities.getWindowAncestor(this), "Loading checklist");
            progressDlg.runTask(backgroundTask);
        } else {
            // Load directly without progress dialog for small checklists
            backgroundTask.run();
        }
    }

    private void loadAndUpdateTasks(java.util.List<Task> selectedTasks) {
        // Load checklist tasks (and headings) in background to avoid blocking EDT
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            class ChecklistBundle { List<Task> customs; List<Task> headings; ChecklistBundle(List<Task> c, List<Task> h) { this.customs = c; this.headings = h; } }
            ChecklistBundle bundle = null;
            try {
                List<Task> customs = taskManager.getTasks(TaskType.CUSTOM, checklist);
                List<Task> headings = taskManager.getTasks(TaskType.HEADING, checklist);
                bundle = new ChecklistBundle(customs, headings);
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(CustomChecklistPanel.class.getName()).log(java.util.logging.Level.SEVERE, "Error loading custom checklist tasks", e);
                return null;
            }
            return bundle;
        }).thenAccept(bundle -> {
            try {
                if (bundle == null) {
                    return;
                }
                
                // Continue on EDT with loaded data
                final List<Task> finalTasks = bundle.customs;
                final List<Task> finalHeadings = bundle.headings;
                SwingUtilities.invokeLater(() -> {
                    try {
                        List<Task> tasks = finalTasks;
                        List<Task> headings = finalHeadings;
                        java.util.Map<String, Task> headingByParent = new java.util.HashMap<>();
                        if (headings != null) {
                            for (Task h : headings) {
                                if (h.getParentId() != null) headingByParent.put(h.getParentId(), h);
                            }
                        }

                        // For custom checklists we want parents followed by their direct subtasks.
                        java.util.List<Task> parents = new java.util.ArrayList<>();
                        for (Task t : tasks) {
                            if (t.getParentId() == null) {
                                parents.add(t);
                            }
                        }
                        java.util.List<Task> desired = new java.util.ArrayList<>();
                        for (Task p : parents) {
                            Task heading = headingByParent.get(p.getId());
                            if (heading != null) desired.add(heading);
                            desired.add(p);
                            java.util.List<Task> subs = taskManager.getSubtasksSorted(p.getId());
                            if (subs != null && !subs.isEmpty()) {
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

                        customTaskList.revalidate();
                        customTaskList.repaint();
                    } catch (Exception e) {
                        java.util.logging.Logger.getLogger(CustomChecklistPanel.class.getName()).log(java.util.logging.Level.SEVERE, "Error updating custom checklist", e);
                    }
                });
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(CustomChecklistPanel.class.getName()).log(java.util.logging.Level.SEVERE, "Error in async update", e);
            }
        }).exceptionally(throwable -> {
            // Log async failure
            java.util.logging.Logger.getLogger(CustomChecklistPanel.class.getName()).log(java.util.logging.Level.SEVERE, "Async task loading failed", throwable);
            return null;
        });
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