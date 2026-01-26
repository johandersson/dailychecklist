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
import java.util.Objects;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
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
                int checkboxX = cellBounds.x + 10;
                int checkboxY = cellBounds.y + cellBounds.height / 2 - 10;
                int checkboxSize = 20;
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
        task.setDone(!task.isDone());
        if (task.isDone()) {
            task.setDoneDate(new Date(System.currentTimeMillis()));
        } else {
            task.setDoneDate(null);
        }
        taskManager.updateTask(task);
        list.repaint(cellBounds);
    }

    // Helper: rename a task using the existing inline prompt
    private void renameTaskInline(JList<Task> list, int index, java.awt.Rectangle cellBounds) {
        Task task = list.getModel().getElementAt(index);
        String rawNewName = JOptionPane.showInputDialog(CustomChecklistPanel.this, "Enter new name for task:", task.getName());
        String newName = TaskManager.validateInputWithError(rawNewName, "Task name");
        if (newName != null) {
            task.setName(newName);
            taskManager.updateTask(task);
            list.repaint(cellBounds);
        }
    }

    private void showContextMenu(MouseEvent e, JList<Task> list, int index) {
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.add(createEditMenuItem(list, index));
        contextMenu.add(createRemoveMenuItem(list, index));
        contextMenu.add(createStartFocusTimerItem(list, index));
        contextMenu.add(createSetTaskReminderItem(list, index));
        contextMenu.add(createRemoveTaskReminderItem(list, index));
        contextMenu.show(list, e.getX(), e.getY());
    }

    private JMenuItem createEditMenuItem(JList<Task> list, int index) {
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
        int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove the task '" + task.getName() + "'?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            taskManager.removeTask(task);
            updateTasks();
        }
    }

    private JPanel createPanel(JList<Task> taskList) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        
        JScrollPane scrollPane = new JScrollPane(taskList);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    

    private void populateReminderPanel(JPanel panel) {
        panel.removeAll();

        // Separate data selection from UI building for improved SOC
        ReminderDisplay display = findReminderToDisplay();
        buildReminderPanelUI(panel, display);
        panel.revalidate();
        panel.repaint();
    }

    /**
     * Holds the reminder and optional extra info (e.g., task name) to display.
     */
    private static final class ReminderDisplay {
        final Reminder reminder;
        final String extraInfo;
        ReminderDisplay(Reminder r, String info) { this.reminder = r; this.extraInfo = info; }
    }

    /**
     * Decide which reminder to show for this checklist: prefer checklist-level reminder,
     * otherwise pick the earliest task-level reminder belonging to tasks in this checklist.
     */
    private ReminderDisplay findReminderToDisplay() {
        java.util.List<Reminder> reminders = taskManager.getReminders();
        // Checklist-level reminder
        Reminder checklistReminder = reminders.stream()
                .filter(r -> Objects.equals(r.getChecklistName(), checklist.getName()) && r.getTaskId() == null)
                .findFirst().orElse(null);
        if (checklistReminder != null) return new ReminderDisplay(checklistReminder, null);

        // Task-level: find earliest upcoming/reminder for tasks in this checklist
        java.util.List<Task> tasks = taskManager.getTasks(TaskType.CUSTOM, checklist);
        java.util.Set<String> taskIds = new java.util.HashSet<>();
        for (Task t : tasks) taskIds.add(t.getId());

        Reminder best = null;
        java.time.LocalDateTime bestTime = null;
        String extraInfo = null;
        for (Reminder r : reminders) {
            if (r.getTaskId() == null) continue;
            if (!taskIds.contains(r.getTaskId())) continue;
            java.time.LocalDateTime rt = java.time.LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
            if (bestTime == null || rt.isBefore(bestTime)) {
                bestTime = rt;
                best = r;
                Task t = tasks.stream().filter(x -> x.getId().equals(r.getTaskId())).findFirst().orElse(null);
                extraInfo = t == null ? null : t.getName();
            }
        }
        if (best == null) return null;
        return new ReminderDisplay(best, extraInfo);
    }

    /**
     * Build the UI for the reminder panel from the prepared display object.
     */
    private void buildReminderPanelUI(JPanel panel, ReminderDisplay display) {
        if (display == null || display.reminder == null) {
            JLabel noReminderLabel = new JLabel("No reminder set");
            noReminderLabel.setFont(FontManager.getSmallFont());
            noReminderLabel.setForeground(java.awt.Color.GRAY);
            panel.add(noReminderLabel, BorderLayout.WEST);
            return;
        }

        Reminder r = display.reminder;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime remTime = java.time.LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
        ReminderClockIcon.State state;
        if (remTime.isBefore(now)) {
            state = java.time.Duration.between(remTime, now).toHours() > 1 ? ReminderClockIcon.State.VERY_OVERDUE : ReminderClockIcon.State.OVERDUE;
        } else if (remTime.isBefore(now.plusMinutes(60))) {
            state = ReminderClockIcon.State.DUE_SOON;
        } else {
            state = ReminderClockIcon.State.FUTURE;
        }

        String labelText = (display.extraInfo != null) ? ("Reminder for: " + display.extraInfo) : "Reminder set";
        javax.swing.JLabel textLabel = new javax.swing.JLabel(labelText);
        textLabel.setFont(FontManager.getSmallMediumFont());

        java.awt.Color dueSoonColor = new java.awt.Color(204, 102, 0);
        switch (state) {
            case OVERDUE -> textLabel.setForeground(java.awt.Color.RED);
            case DUE_SOON -> textLabel.setForeground(dueSoonColor);
            default -> textLabel.setForeground(java.awt.Color.BLUE);
        }

        final Reminder rem = r;
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) showReminderPopup(e, rem);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) showReminderPopup(e, rem);
            }
        };
        textLabel.addMouseListener(ma);
        // Add a clock icon with its own tooltip alongside the text label
        javax.swing.JPanel small = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        small.setOpaque(false);
        javax.swing.Icon icon = IconCache.getReminderClockIcon(r.getHour(), r.getMinute(), state, false);
        javax.swing.JLabel iconLabel = new javax.swing.JLabel(icon);
        // Small HTML tooltip matching help dialog paragraph text size
        String tip = String.format("Reminder: %04d-%02d-%02d %02d:%02d", r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
        iconLabel.setToolTipText("<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>" + tip + "</p></html>");
        // Ensure right-clicks on the icon open same popup
        iconLabel.addMouseListener(ma);
        textLabel.setToolTipText("<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>" + tip + "</p></html>");
        small.add(iconLabel);
        small.add(textLabel);
        panel.add(small, BorderLayout.WEST);
    }

    private void showReminderPopup(MouseEvent e, Reminder reminder) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem removeItem = new JMenuItem("Remove reminder");
        removeItem.addActionListener(ae -> {
            int res = JOptionPane.showConfirmDialog(this, "Remove this reminder?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                taskManager.removeReminder(reminder);
                if (updateAllPanels != null) updateAllPanels.run();
                updateTasks();
            }
        });
        menu.add(removeItem);
        menu.show(e.getComponent(), e.getX(), e.getY());
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
                    TaskUpdater.syncModel(customListModel, tasks);

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