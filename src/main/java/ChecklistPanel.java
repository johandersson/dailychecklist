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
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class ChecklistPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JPanel morningPanel;
    private JPanel eveningPanel;
    private JList<Task> morningTaskList;
    private JList<Task> eveningTaskList;
    private DefaultListModel<Task> morningListModel;
    private DefaultListModel<Task> eveningListModel;
    private final transient TaskManager taskManager;
    private final transient TaskUpdater taskUpdater;
    private JCheckBox showWeekdayTasksCheckbox;

    @SuppressWarnings("this-escape")
    public ChecklistPanel(TaskManager taskManager, TaskUpdater taskUpdater) {
        this.taskManager = taskManager;
        this.taskUpdater = taskUpdater;
        taskManager.addTaskChangeListener(() -> {
            java.awt.Component focused = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            updateTasks();
            if (focused != null && focused.isShowing() && focused.isFocusable()) {
                focused.requestFocusInWindow();
            }
        });
        initialize();
    }

    private void initialize() {
        showWeekdayTasksCheckbox = new JCheckBox("Show all weekday tasks");
        showWeekdayTasksCheckbox.addActionListener(e -> updateTasks());
        morningListModel = new DefaultListModel<>();
        eveningListModel = new DefaultListModel<>();
        morningTaskList = createTaskList(morningListModel, "MORNING");
        eveningTaskList = createTaskList(eveningListModel, "EVENING");
        morningPanel = createPanel("Morning", morningTaskList);
        eveningPanel = createPanel("Evening", eveningTaskList);
        setLayout(new BorderLayout());
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(showWeekdayTasksCheckbox);
        add(topPanel, BorderLayout.NORTH);
        JPanel listsPanel = new JPanel();
        listsPanel.setLayout(new BoxLayout(listsPanel, BoxLayout.Y_AXIS));
        listsPanel.add(morningPanel);
        listsPanel.add(eveningPanel);
        add(listsPanel, BorderLayout.CENTER);
    }

    private JList<Task> createTaskList(DefaultListModel<Task> listModel, String checklistName) {
        Runnable updateCallback = () -> {
            List<Task> allTasks = taskManager.getAllTasks();
            taskUpdater.updateTasks(allTasks, morningListModel, eveningListModel, showWeekdayTasksCheckbox.isSelected(), taskManager);
        };
        JList<Task> taskList = TaskListFactory.createTaskList(listModel, taskManager, checklistName, updateCallback, morningListModel, eveningListModel);
        taskList.addMouseListener(new MouseAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void mouseClicked(MouseEvent e) {
                JList<Task> list = (JList<Task>) e.getSource();
                handleTaskListMouseClicked(e, list);
            }
        });
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

    // Extracted helper to keep createTaskList small
    private void handleTaskListMouseClicked(MouseEvent e, JList<Task> list) {
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
            if (!list.isSelectedIndex(index)) list.setSelectedIndex(index);
            showContextMenu(e, list, index);
            return;
        }

        if (onCheckbox && e.getClickCount() == 1) {
            Task t = list.getModel().getElementAt(index);
            if (t.getType() != TaskType.HEADING) {
                toggleTaskDone(list, index);
                list.repaint(cellBounds);
            }
            return;
        }

        if (e.getClickCount() == 2) {
            Task t = list.getModel().getElementAt(index);
            if (t.getType() == TaskType.HEADING) {
                // Inline rename for heading
                String raw = javax.swing.JOptionPane.showInputDialog(this, "Enter heading text:", t.getName());
                String newName = TaskManager.validateInputWithError(raw, "Heading text");
                if (newName != null) {
                    t.setName(newName);
                    taskManager.updateTask(t);
                    list.repaint(cellBounds);
                }
            } else {
                toggleTaskDone(list, index);
                list.repaint(cellBounds);
            }
        }
    }

    private void toggleTaskDone(JList<Task> list, int index) {
        Task task = list.getModel().getElementAt(index);
        TaskDoneToggler.toggle(taskManager, task, this::updateTasks);
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
        for (int index : selectedIndices) tasks.add(list.getModel().getElementAt(index));
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

    private void showContextMenu(MouseEvent e, JList<Task> list, int index) {
        JPopupMenu contextMenu = new JPopupMenu();
        int[] selected = list.getSelectedIndices();
        if (selected.length > 1) {
            // Only show multi-select delete when multiple items are selected
            addMultiSelectionMenuItems(contextMenu, list);
            contextMenu.show(e.getComponent(), e.getX(), e.getY());
            return;
        }

        // Single selection: show full context menu
        contextMenu.add(createEditMenuItem(list, index));
        contextMenu.add(createCreateHeadingMenuItem(list, index));
        addTypeChangeMenu(contextMenu, list, index);
        addFrequencyMenuIfNeeded(contextMenu, list, index);
        addTaskActionItems(contextMenu, list, index);

        contextMenu.show(e.getComponent(), e.getX(), e.getY());
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

    private JMenuItem createEditMenuItem(JList<Task> list, int index) {
        JMenuItem editItem = new JMenuItem("Rename task");
        editItem.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            String prompt;
            if (task.getType() == TaskType.HEADING) prompt = "Enter heading text:";
            else prompt = (task.getParentId() != null) ? "Enter new name for subtask:" : "Enter new name for task:";
            String rawNewName = javax.swing.JOptionPane.showInputDialog(this, prompt, task.getName());
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
        // Determine authoritative parent and disable the action when inappropriate
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
        boolean disabled = parent.getType() == TaskType.HEADING || alreadyHasHeading;
        if (disabled) {
            item.setEnabled(false);
            item.setToolTipText(parent.getType() == TaskType.HEADING ? "Cannot create a heading above a heading" : "A heading already exists for this parent");
            return item;
        }

        item.addActionListener(e -> {
            String raw = javax.swing.JOptionPane.showInputDialog(this, "Enter heading text:", "");
            String name = TaskManager.validateInputWithError(raw, "Heading text");
            if (name == null) return;
            Task heading = new Task(name, TaskType.HEADING, null, parent.getChecklistId(), parent.getId());
            taskManager.addTask(heading);
            updateTasks();
        });
        return item;
    }

    private void addTypeChangeMenu(JPopupMenu menu, JList<Task> list, int index) {
        JMenu changeTypeItem = new JMenu("Change task type");
        JMenuItem eveningItem = getMorningAndEveneingItems(list, index);
        changeTypeItem.add(eveningItem);
        menu.add(changeTypeItem);
    }

    private void addFrequencyMenuIfNeeded(JPopupMenu menu, JList<Task> list, int index) {
        Task t = list.getModel().getElementAt(index);
        if (t.getWeekday() != null) {
            addMenuItemToChangeFrequencyToDaily(menu, list, index);
            addMenuItemToChangeWeekday(menu, list, index);
        } else {
            addMenuItemToChangeWeekday(menu, list, index);
        }
    }

    private void addTaskActionItems(JPopupMenu menu, JList<Task> list, int index) {
        menu.add(createAddSubtaskMenuItem(list, index));
        menu.add(createRemoveMenuItem(list, index));
        menu.add(createStartFocusTimerMenuItem(list, index));
        menu.add(createSetTaskReminderMenuItem(list, index));
        menu.add(createRemoveTaskReminderMenuItem(list, index));
    }

    // Place at the end of the class, before the final closing brace
    private JMenuItem createAddSubtaskMenuItem(JList<Task> list, int index) {
        JMenuItem addSubtaskItem = new JMenuItem("Add Subtask");
        // Use authoritative task from TaskManager to avoid stale model objects
        Task modelParent = list.getModel().getElementAt(index);
        Task parent = taskManager.getTaskById(modelParent.getId());
        if (parent == null) parent = modelParent;
        if (parent.getParentId() != null) {
            // Disable adding a subtask to a subtask (one-level only)
            addSubtaskItem.setEnabled(false);
            addSubtaskItem.setToolTipText("<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>Cannot add a subtask to a subtask</p></html>");
        } else {
            addSubtaskItem.addActionListener(event -> {
                Task p = taskManager.getTaskById(modelParent.getId());
                if (p == null) p = modelParent;
                Task pForDialog = (p != null) ? p : modelParent;
                String prompt = "Subtask to " + (pForDialog != null ? pForDialog.getName() : "");
                String subtaskName = javax.swing.JOptionPane.showInputDialog(this, prompt);
                if (subtaskName != null && !subtaskName.trim().isEmpty()) {
                    Task subtask = new Task(subtaskName.trim(), p.getType(), p.getWeekday(), p.getChecklistId(), p.getId());
                    taskManager.addTask(subtask);
                    updateTasks();
                }
            });
        }
        return addSubtaskItem;
    }

    private JMenuItem createRemoveMenuItem(JList<Task> list, int index) {
        JMenuItem removeItem = new JMenuItem("Remove task");
        removeItem.addActionListener(event -> removeTask(list, index));
        return removeItem;
    }

    private JMenuItem createStartFocusTimerMenuItem(JList<Task> list, int index) {
        JMenuItem startFocusTimerItem = new JMenuItem("Start Focus Timer on task");
        startFocusTimerItem.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            FocusTimer.getInstance().startFocusTimer(task.getName(), "5 minutes");
        });
        return startFocusTimerItem;
    }

    private JMenuItem createSetTaskReminderMenuItem(JList<Task> list, int index) {
        JMenuItem setTaskReminderItem = new JMenuItem("Set task reminder");
        setTaskReminderItem.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            Reminder existing = taskManager.getReminders().stream()
                .filter(r -> Objects.equals(r.getTaskId(), task.getId()))
                .findFirst().orElse(null);
            String checklistName = task.getType() == TaskType.MORNING ? "MORNING" : task.getType() == TaskType.EVENING ? "EVENING" : null;
            ReminderEditDialog dialog = new ReminderEditDialog(taskManager, checklistName, existing, () -> updateTasks(), task.getId());
            dialog.setVisible(true);
        });
        return setTaskReminderItem;
    }

    private JMenuItem createRemoveTaskReminderMenuItem(JList<Task> list, int index) {
        JMenuItem removeTaskReminderItem = new JMenuItem("Remove task reminder");
        removeTaskReminderItem.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            Reminder existing = taskManager.getReminders().stream()
                .filter(r -> Objects.equals(r.getTaskId(), task.getId()))
                .findFirst().orElse(null);
            if (existing != null) {
                int res = javax.swing.JOptionPane.showConfirmDialog(this, "Remove reminder for task '" + task.getName() + "'?", "Confirm", javax.swing.JOptionPane.YES_NO_OPTION);
                if (res == javax.swing.JOptionPane.YES_OPTION) {
                    taskManager.removeReminder(existing);
                    updateTasks();
                }
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "No reminder set for this task.", "Info", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            }
        });
        return removeTaskReminderItem;
    }

    private JMenuItem getMorningAndEveneingItems(JList<Task> list, int index) {
        JMenuItem morningItem = new JMenuItem("Morning");
        JMenuItem eveningItem = new JMenuItem("Evening");
        morningItem.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            task.setType(TaskType.MORNING);
            taskManager.updateTask(task);
            DefaultListModel<Task> sourceListModel = (task.getType() == TaskType.MORNING) ? eveningListModel : morningListModel;
            DefaultListModel<Task> targetListModel = (task.getType() == TaskType.MORNING) ? morningListModel : eveningListModel;
            sourceListModel.removeElement(task);
            targetListModel.addElement(task);
        });
        eveningItem.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            task.setType(TaskType.EVENING);
            taskManager.updateTask(task);
            DefaultListModel<Task> sourceListModel = (task.getType() == TaskType.MORNING) ? eveningListModel : morningListModel;
            DefaultListModel<Task> targetListModel = (task.getType() == TaskType.MORNING) ? morningListModel : eveningListModel;
            sourceListModel.removeElement(task);
            targetListModel.addElement(task);
        });
        return eveningItem;
    }

    private void addMenuItemToChangeFrequencyToDaily(JPopupMenu contextMenu, JList<Task> list, int index) {
        JMenuItem changeFrequencyToDailyItem = new JMenuItem("Change frequency to Daily");
        changeFrequencyToDailyItem.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            task.setWeekday(null);
            taskManager.updateTask(task);
            list.repaint(list.getCellBounds(index, index));
        });
        contextMenu.add(changeFrequencyToDailyItem);
    }

    private void addMenuItemToChangeWeekday(JPopupMenu contextMenu, JList<Task> list, int index) {
        JMenu changeWeekdayItem = new JMenu("Change frequency to weekday");
        String[] weekdays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (String weekday : weekdays) {
            JMenuItem weekdayItem = new JMenuItem(weekday);
            weekdayItem.addActionListener(event -> {
                Task task = list.getModel().getElementAt(index);
                task.setWeekday(weekday.toLowerCase());
                taskManager.updateTask(task);
                list.repaint(list.getCellBounds(index, index));
            });
            changeWeekdayItem.add(weekdayItem);
        }
        contextMenu.add(changeWeekdayItem);
    }

    //remove task from list
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
            int res = javax.swing.JOptionPane.showOptionDialog(this, sb.toString(), "Confirm Deletion",
                    javax.swing.JOptionPane.DEFAULT_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (res == 0) {
                for (Task s : subs) taskManager.removeTask(s);
                taskManager.removeTask(task);
                updateTasks();
            }
        } else {
            int response = javax.swing.JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to remove the task '" + task.getName() + "'?",
                    "Confirm Removal",
                    javax.swing.JOptionPane.YES_NO_OPTION);
            if (response != javax.swing.JOptionPane.YES_OPTION) {
                return;
            }
            //remove from checklist manager
            taskManager.removeTask(task);
            updateTasks();
        }
    }

    private JPanel createPanel(String title, JList<Task> taskList) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // Header: styled title consistent with app
        javax.swing.JLabel header = new javax.swing.JLabel(title);
        header.setFont(FontManager.getHeader2Font());
        header.setBorder(BorderFactory.createEmptyBorder(2, 2, 6, 2));

        // Reminder/status panel (kept below the heading)
        JPanel reminderPanel = createReminderStatusPanelForType(title);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(reminderPanel, BorderLayout.SOUTH);

        panel.add(north, BorderLayout.NORTH);
        panel.add(new JScrollPane(taskList), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReminderStatusPanelForType(String title) {
        // Delegate reminder UI to ReminderStatusPanel to reduce ChecklistPanel responsibilities
        return new ReminderStatusPanel(taskManager, title);
    }

    // Reminder UI responsibilities have been moved to ReminderStatusPanel and ReminderSelector

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
        java.util.List<Task> selectedMorningTasks = morningTaskList.getSelectedValuesList();
        java.util.List<Task> selectedEveningTasks = eveningTaskList.getSelectedValuesList();

        // Load tasks off the EDT to avoid blocking UI; update models on EDT when ready
        javax.swing.SwingWorker<List<Task>, Void> worker = new javax.swing.SwingWorker<>() {
            @Override
            protected List<Task> doInBackground() throws Exception {
                return taskManager.getAllTasks();
            }

            @Override
            protected void done() {
                try {
                    List<Task> allTasks = get();
                    taskUpdater.updateTasks(allTasks, morningListModel, eveningListModel, showWeekdayTasksCheckbox.isSelected(), taskManager);

                    // Restore selections after updating
                    restoreSelections(morningTaskList, morningListModel, selectedMorningTasks);
                    restoreSelections(eveningTaskList, eveningListModel, selectedEveningTasks);

                    morningTaskList.revalidate();
                    morningTaskList.repaint();
                    eveningTaskList.revalidate();
                    eveningTaskList.repaint();
                } catch (Exception e) {
                    java.util.logging.Logger.getLogger(ChecklistPanel.class.getName()).log(java.util.logging.Level.SEVERE, "Error loading tasks in background", e);
                }
            }
        };
        worker.execute();
    }

    private void restoreSelections(JList<Task> taskList, DefaultListModel<Task> listModel, java.util.List<Task> selectedTasks) {
        for (Task selectedTask : selectedTasks) {
            for (int i = 0; i < listModel.getSize(); i++) {
                if (listModel.getElementAt(i).equals(selectedTask)) {
                    taskList.addSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    public void setShowWeekdayTasks(boolean show) {
        showWeekdayTasksCheckbox.setSelected(show);
        updateTasks();
    }

    public boolean isShowWeekdayTasks() {
        return showWeekdayTasksCheckbox.isSelected();
    }

    public void scrollToAndHighlightTasks(Task[] tasks) {
        if (tasks == null || tasks.length == 0) return;
        
        SwingUtilities.invokeLater(() -> {
            for (Task task : tasks) {
                JList<Task> targetList;
                DefaultListModel<Task> targetModel;
                
                switch (task.getType()) {
                    case MORNING -> {
                        targetList = morningTaskList;
                        targetModel = morningListModel;
                    }
                    case EVENING -> {
                        targetList = eveningTaskList;
                        targetModel = eveningListModel;
                    }
                    default -> { continue; } // Skip custom tasks
                }
                
                // Find the task in the model
                for (int i = 0; i < targetModel.getSize(); i++) {
                    if (targetModel.get(i).equals(task)) {
                        final int index = i;
                        final JList<Task> list = targetList;
                        
                        // Scroll to the task
                        list.ensureIndexIsVisible(index);
                        list.setSelectedIndex(index);
                        
                        // Highlight with animation effect
                        highlightTask(list);
                        break;
                    }
                }
            }
        });
    }

    public void scrollToTask(Task task) {
        JList<Task> list;
        DefaultListModel<Task> model;
        switch (task.getType()) {
            case MORNING -> {
                list = morningTaskList;
                model = morningListModel;
            }
            default -> {
                list = eveningTaskList;
                model = eveningListModel;
            }
        }
        for (int i = 0; i < model.getSize(); i++) {
                if (model.getElementAt(i).getId().equals(task.getId())) {
                    list.setSelectedIndex(i);
                    list.ensureIndexIsVisible(i);
                    highlightTask(list);
                    break;
                }
        }
    }

    private void highlightTask(JList<Task> list) {
        // Simple highlight effect - could be enhanced with more sophisticated animation
        list.requestFocus();
        // The selection already provides visual feedback
    }

}