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
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

@SuppressWarnings("serial")
public class KeyBindingAddAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    private transient java.awt.Component parent;
    private transient TaskManager taskManager;
    private transient Runnable updateTasks;

    public KeyBindingAddAction(java.awt.Component parent, TaskManager taskManager, Runnable updateTasks) {
        this.parent = parent;
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            String rawTaskName = JOptionPane.showInputDialog(parent, "Enter new task name:", "Add New Task", JOptionPane.PLAIN_MESSAGE);
            String taskName = TaskManager.validateInputWithError(rawTaskName, "Task name");
            if (taskName != null) {
                String[] options = {"Morning", "Evening"};
                int timeOfDay = JOptionPane.showOptionDialog(parent, "Select time of day for the task:", "Time of Day",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
                TaskType type = (timeOfDay == 0) ? TaskType.MORNING : TaskType.EVENING;
                Task newTask = new Task(taskName, type, null);
                taskManager.addTask(newTask);
                updateTasks.run();
            }
        }
    }
}