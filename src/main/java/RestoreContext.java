import java.awt.Component;
import java.util.List;
import java.util.Map;

/**
 * Context object carrying parameters required during a restore operation.
 */
public final class RestoreContext {
    public final Component parent;
    public final TaskManager taskManager;
    public final Runnable updateTasks;
    public final Map<String,String> checklistsCopy;
    public final List<Task> customTasks;
    public final List<Task> morningTasks;
    public final List<Task> eveningTasks;
    public final List<Task> currentTasks;

    public RestoreContext(Component parent, TaskManager taskManager, Runnable updateTasks, Map<String,String> checklistsCopy, List<Task> customTasks, List<Task> morningTasks, List<Task> eveningTasks, List<Task> currentTasks) {
        this.parent = parent;
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        this.checklistsCopy = checklistsCopy;
        this.customTasks = customTasks;
        this.morningTasks = morningTasks;
        this.eveningTasks = eveningTasks;
        this.currentTasks = currentTasks;
    }
}
