import java.util.ArrayList;
import java.util.List;

public class StubTaskRepository implements TaskRepository {
    @Override
    public void initialize() {
        // do nothing
    }

    @Override
    public List<Task> getDailyTasks() {
        return new ArrayList<>();
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>();
    }

    @Override
    public void updateTask(Task task) {
        // do nothing
    }

    @Override
    public void removeTask(Task task) {
        // do nothing
    }

    @Override
    public boolean hasUndoneTasks() {
        return false;
    }

    @Override
    public void setTasks(List<Task> tasks) {
        // do nothing
    }
}