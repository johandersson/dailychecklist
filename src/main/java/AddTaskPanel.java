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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

@SuppressWarnings("serial")
public class AddTaskPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private transient final TaskManager taskManager;
    private transient final Consumer<Task[]> onTasksAdded;
    private JComboBox<String> weekdayComboBox;
    private String checklistName;
    private JRadioButton addMorningRadioButton;
    private JRadioButton addEveningRadioButton;
    private JPanel timePanel;

    public AddTaskPanel(TaskManager taskManager, Runnable updateTasks) {
        this(taskManager, tasks -> updateTasks.run(), null);
    }

    public AddTaskPanel(TaskManager taskManager, Runnable updateTasks, String checklistName) {
        this(taskManager, tasks -> updateTasks.run(), checklistName);
    }

    @SuppressWarnings("this-escape")
    public AddTaskPanel(TaskManager taskManager, Consumer<Task[]> onTasksAdded, String checklistName) {
        this.taskManager = taskManager;
        this.onTasksAdded = onTasksAdded;
        this.checklistName = checklistName;
        initialize();
    }

    public AddTaskPanel(TaskManager taskManager, Consumer<Task[]> onTasksAdded) {
        this(taskManager, onTasksAdded, null);
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        // Allow components (task area) to expand to fill available space
        gbc.fill = GridBagConstraints.BOTH;

        JTextArea taskField = new JTextArea(25, 40);
        taskField.setLineWrap(true);
        taskField.setWrapStyleWord(true);
        
        // Add undo/redo support
        final javax.swing.undo.UndoManager undoManager = new javax.swing.undo.UndoManager();
        taskField.getDocument().addUndoableEditListener(undoManager);
        
        // Bind Ctrl+Z for undo and Ctrl+Y for redo
        taskField.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "undo");
        taskField.getActionMap().put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });
        
        taskField.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "redo");
        taskField.getActionMap().put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        });
        
        JScrollPane taskScrollPane = new JScrollPane(taskField);
        taskScrollPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        taskScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        taskScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        taskScrollPane.setPreferredSize(new Dimension(300, 200));
        taskScrollPane.setMinimumSize(new Dimension(200, 120));
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        taskField.setFont(new Font("Yu Gothic UI", Font.PLAIN, 16));
        taskField.setForeground(Color.BLACK);
        taskField.setBackground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(taskScrollPane, gbc);

        // Restore fill and weights for the following controls so they keep normal heights
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;

        if (checklistName == null) {
            addMorningRadioButton = new JRadioButton("Morning");
            addEveningRadioButton = new JRadioButton("Evening");
            ButtonGroup timeGroup = new ButtonGroup();
            timeGroup.add(addMorningRadioButton);
            timeGroup.add(addEveningRadioButton);

            timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            timePanel.add(addMorningRadioButton);
            timePanel.add(addEveningRadioButton);
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

        if (checklistName == null) {
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
            String currentWeekday = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            weekdayComboBox.setSelectedItem(currentWeekday);
            weekdayComboBox.setEnabled(false);
            gbc.gridx = 1;
            gbc.gridy = 3;
            add(weekdayComboBox, gbc);
            weekdayRadioButton.addActionListener(e -> weekdayComboBox.setEnabled(true));
            dailyRadioButton.addActionListener(e -> weekdayComboBox.setEnabled(false));
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
        }

        JButton addButton = new JButton("Add tasks");
        addButton.addActionListener(createAddMultipleTasksActionListener(taskField, addMorningRadioButton, addEveningRadioButton));
        gbc.gridx = 0;
        int buttonRow = (checklistName == null) ? 4 : 1;
        gbc.gridy = buttonRow;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        // Button row should not take extra vertical space
        gbc.weighty = 0.0;
        add(addButton, gbc);
        
        // Add Ctrl+S key binding to task field to trigger add button
        taskField.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke("control S"), "addTasks");
        taskField.getActionMap().put("addTasks", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addButton.doClick();
            }
        });
    }

    private ActionListener createAddMultipleTasksActionListener(JTextArea taskField, JRadioButton morningRadioButton, JRadioButton eveningRadioButton) {
        return e -> {
            String[] lines = taskField.getText().split("\\n");
            
            // Limit to prevent excessive batch additions
            final int MAX_BATCH_TASKS = 1000;
            if (lines.length > MAX_BATCH_TASKS) {
                JOptionPane.showMessageDialog(this, 
                    "Too many lines (" + lines.length + "). Maximum allowed is " + MAX_BATCH_TASKS + " tasks at once.",
                    "Limit Exceeded", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            java.util.List<Task> addedTasks = new java.util.ArrayList<>();
            Task lastParentTask = null;
            
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                
                // Validate selection before processing any tasks
                boolean hasSelection = checklistName != null ||
                    (addMorningRadioButton != null && addMorningRadioButton.isSelected()) ||
                    (addEveningRadioButton != null && addEveningRadioButton.isSelected());
                if (!hasSelection) {
                    JOptionPane.showMessageDialog(this, "Please select Morning or Evening.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (weekdayComboBox != null && weekdayComboBox.isEnabled() && weekdayComboBox.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(this, "Please select a valid weekday.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Determine task type
                TaskType type;
                if (checklistName != null) {
                    type = TaskType.CUSTOM;
                } else if (addMorningRadioButton.isSelected()) {
                    type = TaskType.MORNING;
                } else if (addEveningRadioButton.isSelected()) {
                    type = TaskType.EVENING;
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid selection.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String selectedWeekday = (weekdayComboBox != null && weekdayComboBox.isEnabled() && type != TaskType.CUSTOM) ? (String) weekdayComboBox.getSelectedItem() : null;
                
                // Resolve checklist ID if needed
                String checklistIdForTask = null;
                if (checklistName != null) {
                    Checklist found = taskManager.getCustomChecklists().stream()
                            .filter(c -> checklistName.equals(c.getName()))
                            .findFirst()
                            .orElse(null);
                    if (found != null) checklistIdForTask = found.getId();
                }
                
                // Check if line starts with tab (subtask or heading)
                if (line.startsWith("\t")) {
                    String content = line.substring(1).trim(); // Remove tab and trim
                    if (!content.isEmpty() && lastParentTask != null) {
                        // Check if it's a heading (starts with #)
                        if (content.startsWith("#")) {
                            String headingText = content.substring(1).trim();
                            if (!headingText.isEmpty()) {
                                // Check if parent already has a heading
                                boolean alreadyHasHeading = false;
                                for (Task existingSubtask : taskManager.getSubtasks(lastParentTask.getId())) {
                                    if (existingSubtask.getType() == TaskType.HEADING) {
                                        alreadyHasHeading = true;
                                        break;
                                    }
                                }
                                if (!alreadyHasHeading) {
                                    Task heading = new Task(headingText, TaskType.HEADING, null, checklistIdForTask, lastParentTask.getId());
                                    taskManager.addTask(heading);
                                    addedTasks.add(heading);
                                }
                            }
                        } else {
                            // Regular subtask
                            Task subtask = new Task(content, type, selectedWeekday, checklistIdForTask, lastParentTask.getId());
                            taskManager.addTask(subtask);
                            addedTasks.add(subtask);
                        }
                    }
                } else {
                    // Parent task (no tab indent)
                    String taskName = line.trim();
                    if (!taskName.isEmpty()) {
                        Task newTask = new Task(taskName, type, selectedWeekday, checklistIdForTask);
                        taskManager.addTask(newTask);
                        addedTasks.add(newTask);
                        lastParentTask = newTask; // Track for potential subtasks
                    }
                }
            }
            
            if (!addedTasks.isEmpty()) {
                onTasksAdded.accept(addedTasks.toArray(Task[]::new));
            }
            // Success message removed - replaced with scrolling and highlighting
            taskField.setText("");
            if (morningRadioButton != null) morningRadioButton.setSelected(false);
            if (eveningRadioButton != null) eveningRadioButton.setSelected(false);
            if (weekdayComboBox != null) {
                weekdayComboBox.setEnabled(false);
                weekdayComboBox.setSelectedIndex(0);
            }
        };
    }
}