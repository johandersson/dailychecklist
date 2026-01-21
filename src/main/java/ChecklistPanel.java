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

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
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
    private JCheckBox showWeekdayTasksCheckbox;
    private transient TaskUpdater taskUpdater;
    private transient TaskManager taskManager;

    @SuppressWarnings("this-escape")
    public ChecklistPanel(TaskManager taskManager, TaskUpdater taskUpdater) {
        this.taskManager = taskManager;
        this.taskUpdater = taskUpdater;
        initialize();
    }

    private void initialize() {
        morningListModel = new DefaultListModel<>();
        eveningListModel = new DefaultListModel<>();
        morningTaskList = createTaskList(morningListModel, "MORNING");
        eveningTaskList = createTaskList(eveningListModel, "EVENING");
        morningPanel = createPanel("Morning", morningTaskList);
        eveningPanel = createPanel("Evening", eveningTaskList);
        setLayout(new BorderLayout());
        JPanel listsPanel = new JPanel();
        listsPanel.setLayout(new BoxLayout(listsPanel, BoxLayout.Y_AXIS));
        listsPanel.add(morningPanel);
        listsPanel.add(eveningPanel);
        add(listsPanel, BorderLayout.CENTER);
        showWeekdayTasksCheckbox = new JCheckBox("Show weekday specific tasks");
        showWeekdayTasksCheckbox.addActionListener(e -> updateTasks());
        JPanel southPanel = new JPanel();
        southPanel.add(showWeekdayTasksCheckbox);
        add(southPanel, BorderLayout.SOUTH);
    }

    private JList<Task> createTaskList(DefaultListModel<Task> listModel, String checklistName) {
        JList<Task> taskList = new JList<>(listModel);
        taskList.setCellRenderer(new CheckboxListCellRenderer());
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            taskList.setDragEnabled(true);
            taskList.setTransferHandler(new TaskTransferHandler(taskList, listModel, taskManager, checklistName, () -> {
                List<Task> allTasks = taskManager.getAllTasks();
                taskUpdater.updateTasks(allTasks, morningListModel, eveningListModel, showWeekdayTasksCheckbox.isSelected());
            }, morningListModel, eveningListModel));
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
        //Change task type item, with under menu of morning/evening
        JMenu changeTypeItem = new JMenu("Change task type");
        JMenuItem eveningItem = getMorningAndEveneingItems(list, index);
        //on editItem, add listener to edit task name inline in the list
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

        //if task is weekday task
        if (list.getModel().getElementAt(index).getWeekday() != null) {
            addMenuItemToChangeFrequencyToDaily(contextMenu, list, index);
        }

        //if task is a weekday task, add menu item to change the weekday
        if (list.getModel().getElementAt(index).getWeekday() != null) {
            addMenuItemToChangeWeekday(contextMenu, list, index);
        }

        //if task is not weekday task, add menu item to change frequency to weekday
        if (list.getModel().getElementAt(index).getWeekday() == null) {
            addMenuItemToChangeWeekday(contextMenu, list, index);
        }
        changeTypeItem.add(eveningItem);
        //Add menu item to edit task
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
        contextMenu.show(e.getComponent(), e.getX(), e.getY());
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
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JScrollPane(taskList), BorderLayout.CENTER);
        return panel;
    }

    public void updateTasks() {
        List<Task> allTasks = taskManager.getAllTasks();
        taskUpdater.updateTasks(allTasks, morningListModel, eveningListModel, showWeekdayTasksCheckbox.isSelected());
        morningTaskList.revalidate();
        morningTaskList.repaint();
        eveningTaskList.revalidate();
        eveningTaskList.repaint();
    }

    public void scrollToAndHighlightTasks(Task[] tasks) {
        if (tasks == null || tasks.length == 0) return;
        
        SwingUtilities.invokeLater(() -> {
            for (Task task : tasks) {
                JList<Task> targetList;
                DefaultListModel<Task> targetModel;
                
                if (task.getType() == TaskType.MORNING) {
                    targetList = morningTaskList;
                    targetModel = morningListModel;
                } else if (task.getType() == TaskType.EVENING) {
                    targetList = eveningTaskList;
                    targetModel = eveningListModel;
                } else {
                    continue; // Skip custom tasks
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
                        highlightTask(list, index);
                        break;
                    }
                }
            }
        });
    }

    private void highlightTask(JList<Task> list, int index) {
        // Simple highlight effect - could be enhanced with more sophisticated animation
        list.requestFocus();
        // The selection already provides visual feedback
    }

    public boolean isShowWeekdayTasks() {
        return showWeekdayTasksCheckbox.isSelected();
    }

    public void setShowWeekdayTasks(boolean selected) {
        showWeekdayTasksCheckbox.setSelected(selected);
    }
}