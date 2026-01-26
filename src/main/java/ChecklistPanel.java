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
import java.util.Date;
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
            taskUpdater.updateTasks(allTasks, morningListModel, eveningListModel, showWeekdayTasksCheckbox.isSelected());
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
        int checkboxX = cellBounds.x + 10;
        int checkboxY = cellBounds.y + cellBounds.height / 2 - 10;
        int checkboxSize = 20;
        boolean onCheckbox = e.getPoint().x >= checkboxX && e.getPoint().x <= checkboxX + checkboxSize &&
                            e.getPoint().y >= checkboxY && e.getPoint().y <= checkboxY + checkboxSize;

        if (SwingUtilities.isRightMouseButton(e)) {
            if (!list.isSelectedIndex(index)) list.setSelectedIndex(index);
            showContextMenu(e, list, index);
            return;
        }

        if (onCheckbox && e.getClickCount() == 1) {
            toggleTaskDone(list, index);
            list.repaint(cellBounds);
            return;
        }

        if (e.getClickCount() == 2) {
            toggleTaskDone(list, index);
            list.repaint(cellBounds);
        }
    }

    private void toggleTaskDone(JList<Task> list, int index) {
        Task task = list.getModel().getElementAt(index);
        task.setDone(!task.isDone());
        if (task.isDone()) {
            task.setDoneDate(new Date(System.currentTimeMillis()));
        } else {
            task.setDoneDate(null);
        }
        taskManager.updateTask(task);
    }

    private void deleteSelectedTasks(JList<Task> list) {
        int[] selectedIndices = list.getSelectedIndices();
        if (selectedIndices.length == 0) return;
        java.util.List<Task> tasksToDelete = new java.util.ArrayList<>();
        for (int index : selectedIndices) {
            tasksToDelete.add(list.getModel().getElementAt(index));
        }

        if (confirmDeleteTasks(tasksToDelete)) {
            for (Task task : tasksToDelete) {
                taskManager.removeTask(task);
            }
            updateTasks();
        }
    }

    // Extracted confirmation dialog for deletion to keep deleteSelectedTasks concise
    private boolean confirmDeleteTasks(java.util.List<Task> tasksToDelete) {
        javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), "Confirm Deletion", true);
        dialog.setLayout(new java.awt.BorderLayout());
        dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);

        javax.swing.JLabel label = new javax.swing.JLabel("Are you sure you want to delete the following tasks?");
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 4, 6));
        dialog.add(label, java.awt.BorderLayout.NORTH);

        javax.swing.JList<Task> taskList = new javax.swing.JList<>(tasksToDelete.toArray(Task[]::new));
        taskList.setCellRenderer(new CheckboxListCellRenderer(taskManager));
        taskList.setEnabled(false);
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(taskList);
        scrollPane.setPreferredSize(new java.awt.Dimension(300, 150));
        dialog.add(scrollPane, java.awt.BorderLayout.CENTER);

        javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.FlowLayout());
        javax.swing.JButton yesButton = new javax.swing.JButton("Yes, Delete");
        javax.swing.JButton noButton = new javax.swing.JButton("No, Cancel");
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        dialog.add(buttonPanel, java.awt.BorderLayout.SOUTH);

        final boolean[] confirmed = {false};
        yesButton.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });
        noButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return confirmed[0];
    }

    private void showContextMenu(MouseEvent e, JList<Task> list, int index) {
        JPopupMenu contextMenu = new JPopupMenu();

        addMultiSelectionMenuItems(contextMenu, list);
        contextMenu.add(createEditMenuItem(list, index));
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
            String rawNewName = javax.swing.JOptionPane.showInputDialog(this, "Enter new name for task:", task.getName());
            String newName = TaskManager.validateInputWithError(rawNewName, "Task name");
            if (newName != null) {
                task.setName(newName);
                taskManager.updateTask(task);
                list.repaint(list.getCellBounds(index, index));
            }
        });
        return editItem;
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
        menu.add(createRemoveMenuItem(list, index));
        menu.add(createStartFocusTimerMenuItem(list, index));
        menu.add(createSetTaskReminderMenuItem(list, index));
        menu.add(createRemoveTaskReminderMenuItem(list, index));
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
        //ask user for confirmation, include name of task
        Task task = list.getModel().getElementAt(index);
        int response = javax.swing.JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove the task '" + task.getName() + "'?",
                "Confirm Removal",
                javax.swing.JOptionPane.YES_NO_OPTION);
        if (response != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }
        //remove from checklist manager and list
        taskManager.removeTask(task);
        DefaultListModel<Task> listModel = (DefaultListModel<Task>) list.getModel();
        listModel.removeElementAt(index);
    }

    private JPanel createPanel(String title, JList<Task> taskList) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        // Add a reminder status panel similar to custom checklists
        JPanel reminderPanel = createReminderStatusPanelForType(title);
        panel.add(reminderPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(taskList), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReminderStatusPanelForType(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));
        // Remove timestamp/clock icon above the Morning list (not needed)
        if (title.equalsIgnoreCase("Morning")) {
            JPanel empty = new JPanel();
            empty.setOpaque(false);
            empty.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));
            return empty;
        }

        Reminder display = selectReminderForType(title);
        return buildReminderPanelForType(display);
    }

    // Helper: select the most relevant reminder for a Morning/Evening type
    private Reminder selectReminderForType(String title) {
        java.util.List<Reminder> reminders = taskManager.getReminders();
        Reminder checklistLevel = findChecklistLevelReminder(reminders, title);
        if (checklistLevel != null) return checklistLevel;
        return findEarliestTaskReminderForType(reminders, title);
    }

    private Reminder findChecklistLevelReminder(java.util.List<Reminder> reminders, String title) {
        for (Reminder r : reminders) {
            if (r.getTaskId() == null && title.equalsIgnoreCase(r.getChecklistName())) {
                return r;
            }
        }
        return null;
    }

    private Reminder findEarliestTaskReminderForType(java.util.List<Reminder> reminders, String title) {
        java.time.LocalDateTime best = null;
        Reminder display = null;
        java.util.List<Task> allTasks = taskManager.getAllTasks();
        for (Reminder r : reminders) {
            Task t = taskForReminder(r, allTasks);
            if (t == null) continue;
            if (!reminderMatchesType(t, title)) continue;
            java.time.LocalDateTime rt = reminderDateTime(r);
            if (best == null || rt.isBefore(best)) {
                best = rt;
                display = r;
            }
        }
        return display;
    }

    private Task taskForReminder(Reminder r, java.util.List<Task> allTasks) {
        if (r.getTaskId() == null) return null;
        return allTasks.stream().filter(x -> r.getTaskId().equals(x.getId())).findFirst().orElse(null);
    }

    private boolean reminderMatchesType(Task t, String title) {
        return (title.equalsIgnoreCase("Morning") && t.getType() == TaskType.MORNING) || (title.equalsIgnoreCase("Evening") && t.getType() == TaskType.EVENING);
    }

    private java.time.LocalDateTime reminderDateTime(Reminder r) {
        return java.time.LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
    }

    // Helper: build the UI panel given a selected reminder (or null)
    private JPanel buildReminderPanelForType(Reminder display) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));

        if (display == null) {
            javax.swing.JLabel noReminderLabel = new javax.swing.JLabel("No reminder set");
            noReminderLabel.setFont(FontManager.getSmallFont());
            noReminderLabel.setForeground(java.awt.Color.GRAY);
            panel.add(noReminderLabel, BorderLayout.WEST);
            return panel;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime remTime = reminderDateTime(display);
        ReminderClockIcon.State state = remTime.isBefore(now)
                ? (java.time.Duration.between(remTime, now).toHours() > 1 ? ReminderClockIcon.State.VERY_OVERDUE : ReminderClockIcon.State.OVERDUE)
                : (remTime.isBefore(now.plusMinutes(60)) ? ReminderClockIcon.State.DUE_SOON : ReminderClockIcon.State.FUTURE);

        javax.swing.Icon icon = IconCache.getReminderClockIcon(display.getHour(), display.getMinute(), state, false);
        String text = String.format("%04d-%02d-%02d %02d:%02d", display.getYear(), display.getMonth(), display.getDay(), display.getHour(), display.getMinute());

        javax.swing.JPanel small = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        small.setOpaque(false);
        javax.swing.JLabel iconLabel = new javax.swing.JLabel(icon);
        javax.swing.JLabel textLabel = new javax.swing.JLabel(text);
        textLabel.setFont(FontManager.getSmallMediumFont());
        small.add(iconLabel);
        small.add(textLabel);
        panel.add(small, BorderLayout.WEST);

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
                    taskUpdater.updateTasks(allTasks, morningListModel, eveningListModel, showWeekdayTasksCheckbox.isSelected());

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