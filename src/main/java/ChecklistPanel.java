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
        JList<Task> taskList = new JList<>(listModel);
        taskList.setCellRenderer(new CheckboxListCellRenderer());
        taskList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        taskList.setSelectionBackground(new java.awt.Color(184, 207, 229)); // Consistent selection color
        taskList.setSelectionForeground(java.awt.Color.BLACK);
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
                        // Double-click: toggle done
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
                    // Single click elsewhere: let JList handle selection normally
                }
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

    private void deleteSelectedTasks(JList<Task> list) {
        int[] selectedIndices = list.getSelectedIndices();
        if (selectedIndices.length == 0) return;
        java.util.List<Task> tasksToDelete = new java.util.ArrayList<>();
        for (int index : selectedIndices) {
            tasksToDelete.add(list.getModel().getElementAt(index));
        }

        // Create custom confirmation dialog
        javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), "Confirm Deletion", true);
        dialog.setLayout(new java.awt.BorderLayout());
        dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);

        // Label
        javax.swing.JLabel label = new javax.swing.JLabel("Are you sure you want to delete the following tasks?");
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 4, 6));
        dialog.add(label, java.awt.BorderLayout.NORTH);

        // List of tasks
        javax.swing.JList<Task> taskList = new javax.swing.JList<>(tasksToDelete.toArray(Task[]::new));
        taskList.setCellRenderer(new CheckboxListCellRenderer());
        taskList.setEnabled(false); // Read-only
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(taskList);
        scrollPane.setPreferredSize(new java.awt.Dimension(300, 150));
        dialog.add(scrollPane, java.awt.BorderLayout.CENTER);

        // Buttons
        javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.FlowLayout());
        javax.swing.JButton yesButton = new javax.swing.JButton("Yes, Delete");
        javax.swing.JButton noButton = new javax.swing.JButton("No, Cancel");
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        dialog.add(buttonPanel, java.awt.BorderLayout.SOUTH);

        // Action listeners
        final boolean[] confirmed = {false};
        yesButton.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });
        noButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (confirmed[0]) {
            for (Task task : tasksToDelete) {
                taskManager.removeTask(task);
            }
            // Update the UI
            updateTasks();
        }
    }

    private void showContextMenu(MouseEvent e, JList<Task> list, int index) {
        JPopupMenu contextMenu = new JPopupMenu();

        // Check for multiple selection
        int[] selectedIndices = list.getSelectedIndices();
        if (selectedIndices.length > 1) {
            JMenuItem deleteSelectedItem = new JMenuItem("Delete Selected Tasks");
            deleteSelectedItem.addActionListener(event -> deleteSelectedTasks(list));
            contextMenu.add(deleteSelectedItem);
            contextMenu.addSeparator(); // Separator before individual actions
        }

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
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(new JScrollPane(taskList), BorderLayout.CENTER);
        return panel;
    }

    public void updateTasks() {
        // Preserve selections before updating
        java.util.List<Task> selectedMorningTasks = morningTaskList.getSelectedValuesList();
        java.util.List<Task> selectedEveningTasks = eveningTaskList.getSelectedValuesList();
        
        List<Task> allTasks = taskManager.getAllTasks();
        taskUpdater.updateTasks(allTasks, morningListModel, eveningListModel, showWeekdayTasksCheckbox.isSelected());
        
        // Restore selections after updating
        restoreSelections(morningTaskList, morningListModel, selectedMorningTasks);
        restoreSelections(eveningTaskList, eveningListModel, selectedEveningTasks);
        
        morningTaskList.revalidate();
        morningTaskList.repaint();
        eveningTaskList.revalidate();
        eveningTaskList.repaint();
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