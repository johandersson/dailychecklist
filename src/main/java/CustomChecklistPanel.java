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
    private String checklistName;
    private transient Runnable updateAllPanels;

    public CustomChecklistPanel(TaskManager taskManager, String checklistName) {
        this(taskManager, checklistName, null);
    }

    @SuppressWarnings("this-escape")
    public CustomChecklistPanel(TaskManager taskManager, String checklistName, Runnable updateAllPanels) {
        this.taskManager = taskManager;
        this.checklistName = checklistName;
        this.updateAllPanels = updateAllPanels;
        initialize();
    }

    private void initialize() {
        customListModel = new DefaultListModel<>();
        customTaskList = createTaskList(customListModel);
        JPanel customPanel = createPanel(checklistName, customTaskList);
        setLayout(new BorderLayout());
        add(customPanel, BorderLayout.CENTER);
    }

    private JList<Task> createTaskList(DefaultListModel<Task> listModel) {
        JList<Task> taskList = new JList<>(listModel);
        taskList.setCellRenderer(new CheckboxListCellRenderer());
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            taskList.setDragEnabled(true);
            taskList.setTransferHandler(new TaskTransferHandler(taskList, listModel, taskManager, checklistName, updateAllPanels, null, null));
            taskList.setDropMode(DropMode.INSERT);
        }
        taskList.addMouseListener(new MouseAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void mouseClicked(MouseEvent e) {
                JList<Task> list = (JList<Task>) e.getSource();
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        showContextMenu(e, list, index);
                    } else if (e.getClickCount() == 2) {
                        // Double-click to edit name
                        Task task = list.getModel().getElementAt(index);
                        String rawNewName = JOptionPane.showInputDialog(CustomChecklistPanel.this, "Enter new name for checklist:", task.getName());
                        String newName = TaskManager.validateInputWithError(rawNewName, "Task name");
                        if (newName != null) {
                            task.setName(newName);
                            taskManager.updateTask(task);
                            list.repaint(list.getCellBounds(index, index));
                        }
                    } else {
                        Task task = list.getModel().getElementAt(index);
                        task.setDone(!task.isDone());
                        if (task.isDone()) {
                            task.setDoneDate(new Date(System.currentTimeMillis()));
                        } else {
                            task.setDoneDate(null);
                        }
                        taskManager.updateTask(task);
                        list.repaint(list.getCellBounds(index, index));
                    }
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
        int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove this task?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            taskManager.removeTask(task);
            updateTasks();
        }
    }

    private JPanel createPanel(String title, JList<Task> taskList) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder(title));

        // Add reminder status panel at the top
        JPanel topPanel = createReminderStatusPanel();
        panel.add(topPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(taskList);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReminderStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Check if there's a reminder for this checklist
        boolean hasReminder = taskManager.getReminders().stream()
                .anyMatch(r -> r.getChecklistName().equals(checklistName));

        if (hasReminder) {
            Reminder reminder = taskManager.getReminders().stream()
                    .filter(r -> r.getChecklistName().equals(checklistName))
                    .findFirst()
                    .orElse(null);

                String reminderText = String.format("Reminder: %04d-%02d-%02d at %02d:%02d",
                    reminder.getYear(), reminder.getMonth(), reminder.getDay(),
                    reminder.getHour(), reminder.getMinute());

            // Compute reminder state (overdue, due soon within 60 minutes, or future)
            Calendar now = Calendar.getInstance();
            Calendar remCal = Calendar.getInstance();
            remCal.set(reminder.getYear(), reminder.getMonth() - 1, reminder.getDay(), reminder.getHour(), reminder.getMinute(), 0);
            long diff = remCal.getTimeInMillis() - now.getTimeInMillis();
            ReminderClockIcon.State state = diff < 0
                    ? ReminderClockIcon.State.OVERDUE
                    : (diff <= 60L * 60L * 1000L ? ReminderClockIcon.State.DUE_SOON : ReminderClockIcon.State.FUTURE);

            // Create icon without its own time text; we'll show date then time in the label
            ReminderClockIcon icon = new ReminderClockIcon(reminder.getHour(), reminder.getMinute(), state, false);
            String dateText = String.format("%04d-%02d-%02d", reminder.getYear(), reminder.getMonth(), reminder.getDay());
            String timeText = String.format("%02d:%02d", reminder.getHour(), reminder.getMinute());
            JLabel reminderLabel = new JLabel(dateText + "  " , icon, JLabel.LEADING);
            reminderLabel.setIconTextGap(6);
            // Append the time after the icon
            reminderLabel.setText(dateText + " " + timeText);
            reminderLabel.setFont(reminderLabel.getFont().deriveFont(11.0f));
            if (state == ReminderClockIcon.State.OVERDUE) {
                reminderLabel.setForeground(java.awt.Color.RED);
            } else if (state == ReminderClockIcon.State.DUE_SOON) {
                reminderLabel.setForeground(java.awt.Color.ORANGE);
            } else {
                reminderLabel.setForeground(java.awt.Color.BLUE);
            }

            // Add right-click menu to remove the reminder
            reminderLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        showReminderPopup(e, reminder);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        showReminderPopup(e, reminder);
                    }
                }
            });

            panel.add(reminderLabel, BorderLayout.WEST);
        } else {
            JLabel noReminderLabel = new JLabel("No reminder set");
            noReminderLabel.setFont(noReminderLabel.getFont().deriveFont(10.0f));
            noReminderLabel.setForeground(java.awt.Color.GRAY);
            panel.add(noReminderLabel, BorderLayout.WEST);
        }

        return panel;
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
        customListModel.clear();
        List<Task> tasks = taskManager.getTasks(TaskType.CUSTOM, checklistName);
        for (Task task : tasks) {
            customListModel.addElement(task);
        }
    }
}