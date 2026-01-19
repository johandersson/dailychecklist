import java.util.ArrayList;
import java.util.List;

public class StubTaskManager extends TaskManager {
    private final List<Task> tasks = new ArrayList<>();

    public StubTaskManager() {
        super(new StubTaskRepository());
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    @Override
    public void updateTask(Task task) {
        System.out.println("Stub: Task updated: " + task.getName());
    }

    @Override
    public void removeTask(Task task) {
        tasks.remove(task);
        System.out.println("Stub: Task removed: " + task.getName());
    }

    public void addTask(Task task) {
        tasks.add(task);
    }
}