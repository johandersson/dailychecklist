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
            // Track parents that already received a heading in this batch
            java.util.Set<String> parentsWithHeading = new java.util.HashSet<>();
            String deferredHeading = null;
            
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                
                // Detect indentation: tab or 2+ leading spaces
                boolean isIndented = line.startsWith("\t") || line.startsWith("  ");
                
                if (isIndented) {
                    // Strip all leading whitespace (tabs or spaces)
                    String content = line.replaceFirst("^[\\t ]+", "");
                    if (!content.isEmpty() && lastParentTask != null) {
                        // Check if it's a heading (starts with #)
                        if (content.startsWith("#")) {
                            String headingText = content.substring(1).trim();
                            if (!headingText.isEmpty()) {
                                boolean alreadyHasHeading = parentsWithHeading.contains(lastParentTask.getId());
                                if (!alreadyHasHeading) {
                                    alreadyHasHeading = hasHeadingForParent(lastParentTask.getId());
                                }
                                if (!alreadyHasHeading) {
                                    Task heading = new Task(headingText, TaskType.HEADING, null, checklistName, lastParentTask.getId());
                                    taskManager.addTask(heading);
                                    parentsWithHeading.add(lastParentTask.getId());
                                    addedCount++;
                                }
                            }
                        } else {
                            // Regular subtask
                            Task subtask = new Task(content, TaskType.CUSTOM, null, checklistName, lastParentTask.getId());
                            taskManager.addTask(subtask);
                            addedCount++;
                        }
                    }
                } else {
                    // Top-level line
                    String trimmed = line.trim();
                    if (trimmed.startsWith("#")) {
                        // Top-level heading annotation — defer and attach to next parent
                        String headingText = trimmed.substring(1).trim();
                        if (!headingText.isEmpty()) {
                            deferredHeading = headingText;
                        }
                    } else if (!trimmed.isEmpty()) {
                        // Parent task
                        Task newTask = new Task(trimmed, TaskType.CUSTOM, null, checklistName);
                        taskManager.addTask(newTask);
                        lastParentTask = newTask;
                        addedCount++;
                        
                        // Attach deferred heading from preceding # line
                        if (deferredHeading != null) {
                            if (!parentsWithHeading.contains(lastParentTask.getId())) {
                                Task heading = new Task(deferredHeading, TaskType.HEADING, null, checklistName, lastParentTask.getId());
                                taskManager.addTask(heading);
                                parentsWithHeading.add(lastParentTask.getId());
                                addedCount++;
                            }
                            deferredHeading = null;
                        }
                    }
                }
            }
            
            if (addedCount == 0) {
                JOptionPane.showMessageDialog(this, "No valid tasks to add.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            taskField.setText("");
            updateTasks.run();
        };
    }

    @Override
    protected int getButtonRow() {
        return 1;
    }

    private boolean hasHeadingForParent(String parentId) {
        for (Task t : taskManager.getAllTasks()) {
            if (t.getType() == TaskType.HEADING && parentId.equals(t.getParentId())) {
                return true;
            }
        }
        return false;
    }
}