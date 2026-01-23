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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
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
    private JPanel reminderStatusPanel;

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
        try {
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
        } catch (Exception ignore) {}
    }

    private void initialize() {
        customListModel = new DefaultListModel<>();
        customTaskList = createTaskList(customListModel);
        JPanel customPanel = createPanel(checklist.getName(), customTaskList);
        setLayout(new BorderLayout());
        add(customPanel, BorderLayout.CENTER);
    }

    private JList<Task> createTaskList(DefaultListModel<Task> listModel) {
        JList<Task> taskList = new JList<>(listModel);
        taskList.setCellRenderer(new CheckboxListCellRenderer());
        taskList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        taskList.setSelectionBackground(new java.awt.Color(184, 207, 229)); // Same as checklist list
        taskList.setSelectionForeground(java.awt.Color.BLACK);
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            taskList.setDragEnabled(true);
            taskList.setTransferHandler(new TaskTransferHandler(taskList, listModel, taskManager, checklist.getName(), updateAllPanels, null, null));
            taskList.setDropMode(DropMode.INSERT);
        }
        taskList.addMouseListener(new MouseAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void mouseClicked(MouseEvent e) {
                JList<Task> list = (JList<Task>) e.getSource();
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) {
                    java.awt.Rectangle cellBounds = list.getCellBounds(index, index);
                    int checkboxX = cellBounds.x + 10;
                    int checkboxY = cellBounds.y + cellBounds.height / 2 - 10;
                    int checkboxSize = 20;
                    boolean onCheckbox = e.getPoint().x >= checkboxX && e.getPoint().x <= checkboxX + checkboxSize &&
                                         e.getPoint().y >= checkboxY && e.getPoint().y <= checkboxY + checkboxSize;

                    if (SwingUtilities.isRightMouseButton(e)) {
                        // Right-click: ensure the item is selected
                        if (!list.isSelectedIndex(index)) {
                            list.setSelectedIndex(index);
                        }
                        showContextMenu(e, list, index);
                    } else if (onCheckbox && e.getClickCount() == 1) {
                        // Single-click on checkbox: toggle done
                        Task task = list.getModel().getElementAt(index);
                        task.setDone(!task.isDone());
                        if (task.isDone()) {
                            task.setDoneDate(new Date(System.currentTimeMillis()));
                        } else {
                            task.setDoneDate(null);
                        }
                        taskManager.updateTask(task);
                        list.repaint(cellBounds);
                    } else if (e.getClickCount() == 2) {
                        // Double-click: edit name
                        Task task = list.getModel().getElementAt(index);
                        String rawNewName = JOptionPane.showInputDialog(CustomChecklistPanel.this, "Enter new name for task:", task.getName());
                        String newName = TaskManager.validateInputWithError(rawNewName, "Task name");
                        if (newName != null) {
                            task.setName(newName);
                            taskManager.updateTask(task);
                            list.repaint(cellBounds);
                        }
                    }
                    // Single click elsewhere: let JList handle selection normally
                }
            }
        });
        return taskList;
    }

    private void showContextMenu(MouseEvent e, JList<Task> list, int index) {
        JPopupMenu contextMenu = new JPopupMenu();
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
        JMenuItem removeItem = new JMenuItem("Remove task");
        //Start FocusTimer window item
        JMenuItem startFocusTimerItem = new JMenuItem("Start Focus Timer on task");

        removeItem.addActionListener(event -> removeTask(list, index));
        startFocusTimerItem.addActionListener(event -> {
            Task task = list.getModel().getElementAt(index);
            FocusTimer.getInstance().startFocusTimer(task.getName(), "5 minutes");
        });
        contextMenu.add(editItem);
        contextMenu.add(removeItem);
        contextMenu.add(startFocusTimerItem);
        contextMenu.show(list, e.getX(), e.getY());
    }

    private void removeTask(JList<Task> list, int index) {
        Task task = list.getModel().getElementAt(index);
        int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove the task '" + task.getName() + "'?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            taskManager.removeTask(task);
            updateTasks();
        }
    }

    private JPanel createPanel(String title, JList<Task> taskList) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // Add reminder status panel at the top
        reminderStatusPanel = createReminderStatusPanel();
        panel.add(reminderStatusPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(taskList);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReminderStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));
        populateReminderPanel(panel);
        return panel;
    }

    private void populateReminderPanel(JPanel panel) {
        panel.removeAll();

        // Check if there's a reminder for this checklist
        boolean hasReminder = taskManager.getReminders().stream()
                .anyMatch(r -> r.getChecklistName().equals(checklist.getName()));

        if (hasReminder) {
            Reminder reminder = taskManager.getReminders().stream()
                    .filter(r -> r.getChecklistName().equals(checklist.getName()))
                    .findFirst()
                    .orElse(null);

            // Compute reminder state (overdue, due soon within 60 minutes, or future)
            Calendar now = Calendar.getInstance();
            Calendar remCal = Calendar.getInstance();
            remCal.set(reminder.getYear(), reminder.getMonth() - 1, reminder.getDay(), reminder.getHour(), reminder.getMinute(), 0);
            long diff = remCal.getTimeInMillis() - now.getTimeInMillis();
            ReminderClockIcon.State state = diff < 0
                ? (Math.abs(diff) > 60L * 60L * 1000L ? ReminderClockIcon.State.VERY_OVERDUE : ReminderClockIcon.State.OVERDUE)
                : (diff <= 60L * 60L * 1000L ? ReminderClockIcon.State.DUE_SOON : ReminderClockIcon.State.FUTURE);

            // Create icon without its own time text; use cached instance
            javax.swing.Icon icon = IconCache.getReminderClockIcon(reminder.getHour(), reminder.getMinute(), state, false);
            String dateText = String.format("%04d-%02d-%02d", reminder.getYear(), reminder.getMonth(), reminder.getDay());
            String timeText = String.format("%02d:%02d", reminder.getHour(), reminder.getMinute());

            javax.swing.JPanel small = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
            small.setOpaque(false);
            javax.swing.JLabel iconLabel = new javax.swing.JLabel(icon);
            javax.swing.JLabel textLabel = new javax.swing.JLabel(dateText + " " + timeText);
            textLabel.setFont(FontManager.getSmallMediumFont());

            // Choose a darker orange for readability
            java.awt.Color dueSoonColor = new java.awt.Color(204, 102, 0);
            switch (state) {
                case OVERDUE -> textLabel.setForeground(java.awt.Color.RED);
                case DUE_SOON -> textLabel.setForeground(dueSoonColor);
                default -> textLabel.setForeground(java.awt.Color.BLUE);
            }

            // Add right-click menu to remove the reminder on the whole small panel
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) showReminderPopup(e, reminder);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) showReminderPopup(e, reminder);
                }
            };
            iconLabel.addMouseListener(ma);
            textLabel.addMouseListener(ma);
            small.add(iconLabel);
            small.add(textLabel);
            panel.add(small, BorderLayout.WEST);
        } else {
            JLabel noReminderLabel = new JLabel("No reminder set");
            noReminderLabel.setFont(FontManager.getSmallFont());
            noReminderLabel.setForeground(java.awt.Color.GRAY);
            panel.add(noReminderLabel, BorderLayout.WEST);
        }

        panel.revalidate();
        panel.repaint();
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
        // Preserve selections before updating
        java.util.List<Task> selectedTasks = customTaskList.getSelectedValuesList();
        
        customListModel.clear();
        List<Task> tasks = taskManager.getTasks(TaskType.CUSTOM, checklist);
        for (Task task : tasks) {
            customListModel.addElement(task);
        }
        
        // Restore selections after updating
        for (Task selectedTask : selectedTasks) {
            for (int i = 0; i < customListModel.getSize(); i++) {
                if (customListModel.getElementAt(i).equals(selectedTask)) {
                    customTaskList.addSelectionInterval(i, i);
                    break;
                }
            }
        }
        
        if (reminderStatusPanel != null) {
            populateReminderPanel(reminderStatusPanel);
        }
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
                } catch (Exception ex) {
                    try { customTaskList.requestFocusInWindow(); } catch (Exception ignore) {}
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