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
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/**
 * Context-aware Ctrl+N action that handles both daily and custom checklists.
 * 
 * When in custom checklists tab:
 * - If a checklist is marked: Creates a new CUSTOM task in that checklist
 * - If no checklist is marked: Creates a new custom checklist
 * 
 * When in daily checklist tab:
 * - Creates a new daily task (MORNING or EVENING)
 */
@SuppressWarnings("serial")
public class AddTaskActionContextAware extends AbstractAction {
    private static final long serialVersionUID = 1L;
    private final java.awt.Component parent;
    private final TaskManager taskManager;
    private final Runnable updateTasks;
    private final CustomChecklistContextProvider contextProvider;

    public AddTaskActionContextAware(java.awt.Component parent, TaskManager taskManager, 
                                     Runnable updateTasks, CustomChecklistContextProvider contextProvider) {
        super("ADD_NEW_TASK");
        this.parent = parent;
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        this.contextProvider = contextProvider;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Check if we're in the custom checklists tab
        if (contextProvider.isInCustomChecklistsTab()) {
            handleCustomChecklistTab();
        } else {
            handleDailyChecklistTab();
        }
    }

    /**
     * Handles Ctrl+N in the custom checklists tab.
     * Creates a task in marked checklist or a new checklist if none is marked.
     */
    private void handleCustomChecklistTab() {
        Checklist selectedChecklist = contextProvider.getSelectedChecklist();
        
        if (selectedChecklist != null) {
            // A checklist is marked: create a new task in it
            createTaskInChecklist(selectedChecklist);
        } else {
            // No checklist is marked: create a new checklist
            createNewChecklist();
        }
        updateTasks.run();
    }

    /**
     * Handles Ctrl+N in the daily checklist tab.
     * Creates a new MORNING or EVENING task.
     */
    private void handleDailyChecklistTab() {
        String taskName;
        int timeOfDay;
        if (GraphicsEnvironment.isHeadless()) {
            taskName = "Test Task";
            timeOfDay = 0;
        } else {
            String rawTaskName = JOptionPane.showInputDialog(parent, "Enter new task name:", "Add New Task", JOptionPane.PLAIN_MESSAGE);
            taskName = TaskManager.validateInputWithError(rawTaskName, "Task name");
            if (taskName == null) {
                return;
            }
            String[] options = {"Morning", "Evening"};
            timeOfDay = JOptionPane.showOptionDialog(parent, "Select time of day for the task:", "Time of Day",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        }
        TaskType type = (timeOfDay == 0) ? TaskType.MORNING : TaskType.EVENING;
        Task newTask = new Task(taskName, type, null);
        taskManager.addTask(newTask);
        updateTasks.run();
    }

    /**
     * Creates a new CUSTOM task in the specified checklist.
     */
    private void createTaskInChecklist(Checklist checklist) {
        if (GraphicsEnvironment.isHeadless()) {
            Task task = new Task("Test Task", TaskType.CUSTOM, null, checklist.getId(), null);
            taskManager.addTask(task);
            return;
        }
        
        String rawTaskName = JOptionPane.showInputDialog(parent, "Enter new task name:", "Add New Task", JOptionPane.PLAIN_MESSAGE);
        String taskName = TaskManager.validateInputWithError(rawTaskName, "Task name");
        if (taskName == null) {
            return;
        }
        
        Task task = new Task(taskName, TaskType.CUSTOM, null, checklist.getId(), null);
        taskManager.addTask(task);
    }

    /**
     * Creates a new custom checklist and selects it.
     */
    private void createNewChecklist() {
        if (GraphicsEnvironment.isHeadless()) {
            Checklist checklist = new Checklist("Test Checklist");
            taskManager.addChecklist(checklist);
            contextProvider.setSelectedChecklistInUI(checklist);
            return;
        }
        
        String rawName = JOptionPane.showInputDialog(parent, "Enter checklist name:", "New Checklist", JOptionPane.PLAIN_MESSAGE);
        String checklistName = TaskManager.validateInputWithError(rawName, "Checklist name");
        if (checklistName == null) {
            return;
        }
        
        Checklist checklist = new Checklist(checklistName);
        taskManager.addChecklist(checklist);
        contextProvider.setSelectedChecklistInUI(checklist);
    }
}
