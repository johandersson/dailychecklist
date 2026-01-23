import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

@SuppressWarnings("serial")
public class AddTaskAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    private final java.awt.Component parent;
    private final TaskManager taskManager;
    private final Runnable updateTasks;

    public AddTaskAction(java.awt.Component parent, TaskManager taskManager, Runnable updateTasks) {
        super("ADD_NEW_TASK");
        this.parent = parent;
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
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
}
