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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.JRadioButton;

public class AddTaskPanel extends JPanel {
    private final TaskManager taskManager;
    private final Runnable updateTasks;
    private JComboBox<String> weekdayComboBox;
    private String checklistName;
    private JRadioButton addMorningRadioButton;
    private JRadioButton addEveningRadioButton;
    private JRadioButton addCustomRadioButton;
    private JPanel timePanel;

    public AddTaskPanel(TaskManager taskManager, Runnable updateTasks) {
        this(taskManager, updateTasks, null);
    }

    public AddTaskPanel(TaskManager taskManager, Runnable updateTasks, String checklistName) {
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        this.checklistName = checklistName;
        initialize();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextArea taskField = new JTextArea(25, 40);
        taskField.setLineWrap(true);
        taskField.setWrapStyleWord(true);
        JScrollPane taskScrollPane = new JScrollPane(taskField);
        taskScrollPane.setBorder(BorderFactory.createTitledBorder("Task name(s) one per line"));
        taskScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        taskScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        taskScrollPane.setPreferredSize(new Dimension(300, 200));
        taskScrollPane.setMinimumSize(new Dimension(300, 200));
        taskField.setFont(new Font("Yu Gothic UI", Font.PLAIN, 16));
        taskField.setForeground(Color.BLACK);
        taskField.setBackground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(taskScrollPane, gbc);

        if (checklistName == null) {
            addMorningRadioButton = new JRadioButton("Morning");
            addEveningRadioButton = new JRadioButton("Evening");
            addCustomRadioButton = new JRadioButton("Custom checklist");
            ButtonGroup timeGroup = new ButtonGroup();
            timeGroup.add(addMorningRadioButton);
            timeGroup.add(addEveningRadioButton);
            timeGroup.add(addCustomRadioButton);

            timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            timePanel.add(addMorningRadioButton);
            timePanel.add(addEveningRadioButton);
            timePanel.add(addCustomRadioButton);
        }

        if (timePanel != null) {
            JLabel timeLabel = new JLabel("Time of Day:");
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            add(timeLabel, gbc);

            gbc.gridx = 1;
            gbc.gridy = 1;
            add(timePanel, gbc);
        }

        JLabel frequencyLabel = new JLabel("Frequency:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(frequencyLabel, gbc);

        JRadioButton dailyRadioButton = new JRadioButton("Daily");
        JRadioButton weekdayRadioButton = new JRadioButton("Weekday");
        ButtonGroup frequencyGroup = new ButtonGroup();
        frequencyGroup.add(dailyRadioButton);
        frequencyGroup.add(weekdayRadioButton);

        JPanel frequencyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        frequencyPanel.add(dailyRadioButton);
        frequencyPanel.add(weekdayRadioButton);
        gbc.gridx = 1;
        gbc.gridy = 2;
        add(frequencyPanel, gbc);

        JLabel weekdayLabel = new JLabel("Weekday:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(weekdayLabel, gbc);
        String[] weekdays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        weekdayComboBox = new JComboBox<>(weekdays);
        weekdayComboBox.setEnabled(false);
        gbc.gridx = 1;
        gbc.gridy = 3;
        add(weekdayComboBox, gbc);
        weekdayRadioButton.addActionListener(e -> weekdayComboBox.setEnabled(true));
        dailyRadioButton.addActionListener(e -> weekdayComboBox.setEnabled(false));
        if (addCustomRadioButton != null) {
            addCustomRadioButton.addActionListener(e -> {
                frequencyLabel.setVisible(false);
                frequencyPanel.setVisible(false);
                weekdayLabel.setVisible(false);
                weekdayComboBox.setVisible(false);
            });
        }
        if (addMorningRadioButton != null) {
            addMorningRadioButton.addActionListener(e -> {
                frequencyLabel.setVisible(true);
                frequencyPanel.setVisible(true);
                weekdayLabel.setVisible(true);
                weekdayComboBox.setVisible(true);
            });
        }
        if (addEveningRadioButton != null) {
            addEveningRadioButton.addActionListener(e -> {
                frequencyLabel.setVisible(true);
                frequencyPanel.setVisible(true);
                weekdayLabel.setVisible(true);
                weekdayComboBox.setVisible(true);
            });
        }

        JButton addButton = new JButton("Add tasks");
        addButton.addActionListener(createAddMultipleTasksActionListener(taskField, addMorningRadioButton, addEveningRadioButton, addCustomRadioButton));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(addButton, gbc);
    }

    private ActionListener createAddMultipleTasksActionListener(JTextArea taskField, JRadioButton morningRadioButton, JRadioButton eveningRadioButton, JRadioButton customRadioButton) {
        return e -> {
            String[] tasks = taskField.getText().split("\\n");
            for (String taskName : tasks) {
                if (!taskName.trim().isEmpty()) {
                    boolean hasSelection = checklistName != null ||
                        (addMorningRadioButton != null && addMorningRadioButton.isSelected()) ||
                        (addEveningRadioButton != null && addEveningRadioButton.isSelected()) ||
                        (addCustomRadioButton != null && addCustomRadioButton.isSelected());
                    if (!hasSelection) {
                        JOptionPane.showMessageDialog(this, "Please select a type (Morning, Evening, or Custom checklist).", "Validation Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (weekdayComboBox.isEnabled() && weekdayComboBox.getSelectedItem() == null) {
                        JOptionPane.showMessageDialog(this, "Please select a valid weekday.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    TaskType type;
                    if (checklistName != null) {
                        type = TaskType.CUSTOM;
                    } else if (addMorningRadioButton.isSelected()) {
                        type = TaskType.MORNING;
                    } else if (addEveningRadioButton.isSelected()) {
                        type = TaskType.EVENING;
                    } else {
                        type = TaskType.CUSTOM;
                    }
                    String selectedWeekday = (weekdayComboBox.isEnabled() && type != TaskType.CUSTOM) ? (String) weekdayComboBox.getSelectedItem() : null;
                    Task newTask = new Task(taskName.trim(), type, selectedWeekday, checklistName);
                    taskManager.addTask(newTask);
                } else {
                    JOptionPane.showMessageDialog(this, "Task name cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            String taskType;
            if (checklistName != null) {
                taskType = "Custom checklist";
            } else if (addMorningRadioButton.isSelected()) {
                taskType = "Morning";
            } else if (addEveningRadioButton.isSelected()) {
                taskType = "Evening";
            } else {
                taskType = "Custom checklist";
            }
            String frequencyType;
            if (weekdayComboBox.isEnabled() && taskType != "Custom checklist") {
                String selectedWeekday = (String) weekdayComboBox.getSelectedItem();
                frequencyType = "Weekday: " + selectedWeekday;
            } else {
                frequencyType = "Daily";
            }
            String message = String.format("Added %d %s tasks (%s) successfully.", tasks.length, taskType, frequencyType);
            JOptionPane.showMessageDialog(this, message, "Tasks Added", JOptionPane.INFORMATION_MESSAGE);
            taskField.setText("");
            morningRadioButton.setSelected(false);
            eveningRadioButton.setSelected(false);
            customRadioButton.setSelected(false);
            weekdayComboBox.setEnabled(false);
            weekdayComboBox.setSelectedIndex(0);
            updateTasks.run();
        };
    }
}