import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

public class KeyBindingAddAction extends AbstractAction {
    private java.awt.Component parent;
    private TaskManager taskManager;
    private Runnable updateTasks;

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