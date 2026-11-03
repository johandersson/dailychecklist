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

/**
 * Handles Ctrl+N action in the custom checklists tab.
 * - If a checklist is marked: Creates a new CUSTOM task in that checklist
 * - If no checklist is marked: Creates a new custom checklist
 */
public class CtrlNCustomChecklistAction {
    private final TaskManager taskManager;
    private final CustomChecklistContextProvider contextProvider;
    
    public CtrlNCustomChecklistAction(TaskManager taskManager, CustomChecklistContextProvider contextProvider) {
        this.taskManager = taskManager;
        this.contextProvider = contextProvider;
    }
    
    /**
     * Creates a new custom checklist with the given name.
     * @param checklistName The name for the new checklist
     * @return The created Checklist
     */
    public Checklist createNewChecklistWithName(String checklistName) {
        if (checklistName == null || checklistName.trim().isEmpty()) {
            throw new IllegalArgumentException("Checklist name cannot be empty");
        }
        
        Checklist checklist = new Checklist(checklistName);
        taskManager.addChecklist(checklist);
        contextProvider.setSelectedChecklistInUI(checklist);
        
        return checklist;
    }
    
    /**
     * Creates a new CUSTOM task in the selected checklist.
     * @param taskName The name for the new task
     * @return The created Task
     */
    public Task createNewTaskInSelectedChecklist(String taskName) {
        Checklist selected = contextProvider.getSelectedChecklist();
        if (selected == null) {
            throw new IllegalStateException("No checklist is currently selected");
        }
        
        if (taskName == null || taskName.trim().isEmpty()) {
            throw new IllegalArgumentException("Task name cannot be empty");
        }
        
        Task task = new Task(taskName, TaskType.CUSTOM, null, selected.getId(), null);
        taskManager.addTask(task);
        
        return task;
    }
}
