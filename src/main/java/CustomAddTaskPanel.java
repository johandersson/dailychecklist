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
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

@SuppressWarnings("serial")
public class CustomAddTaskPanel extends BaseAddTaskPanel {
    private static final long serialVersionUID = 1L;
    private final String checklistName;

    public CustomAddTaskPanel(TaskManager taskManager, Runnable updateTasks, String checklistName) {
        super(taskManager, updateTasks);
        this.checklistName = checklistName;
    }

    @Override
    protected void initializeSpecific() {
        // No additional initialization needed for custom
    }

    @Override
    protected ActionListener createAddActionListener() {
        return e -> {
            String[] lines = taskField.getText().split("\\n");
            int addedCount = 0;
            Task lastParentTask = null;
            
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                
                // Check if line starts with tab (subtask)
                if (line.startsWith("\t")) {
                    String subtaskName = line.substring(1).trim(); // Remove tab and trim
                    if (!subtaskName.isEmpty() && lastParentTask != null) {
                        // Create subtask with parent reference
                        Task subtask = new Task(subtaskName, TaskType.CUSTOM, null, checklistName, lastParentTask.getId());
                        taskManager.addTask(subtask);
                        addedCount++;
                    }
                } else {
                    // Parent task (no tab indent)
                    String taskName = line.trim();
                    if (!taskName.isEmpty()) {
                        Task newTask = new Task(taskName, TaskType.CUSTOM, null, checklistName);
                        taskManager.addTask(newTask);
                        lastParentTask = newTask; // Track for potential subtasks
                        addedCount++;
                    }
                }
            }
            
            if (addedCount == 0) {
                JOptionPane.showMessageDialog(this, "No valid tasks to add.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String message = String.format("Added %d tasks to %s successfully.", addedCount, checklistName);
            JOptionPane.showMessageDialog(this, message, "Tasks Added", JOptionPane.INFORMATION_MESSAGE);
            taskField.setText("");
            updateTasks.run();
        };
    }

    @Override
    protected int getButtonRow() {
        return 1;
    }
}