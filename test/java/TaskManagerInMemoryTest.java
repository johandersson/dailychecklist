import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.*;

public class TaskManagerInMemoryTest {
    private TaskManager taskManager;

    static class InMemoryTaskRepository implements TaskRepository {
        private final Map<String, Task> map = new LinkedHashMap<>();
        private final Set<Checklist> checklists = new HashSet<>();

        @Override public void initialize() {}
        @Override public List<Task> getDailyTasks() { return new ArrayList<>(map.values()); }
        @Override public List<Task> getAllTasks() { return new ArrayList<>(map.values()); }
        @Override public void addTask(Task task) { map.put(task.getId(), task);} 
        @Override public void updateTask(Task task) { map.put(task.getId(), task);} 
        @Override public void removeTask(Task task) { map.remove(task.getId()); }
        @Override public boolean hasUndoneTasks() { return map.values().stream().anyMatch(t -> !t.isDone()); }
        @Override public void setTasks(List<Task> tasks) { map.clear(); if (tasks!=null) tasks.forEach(t->map.put(t.getId(), t)); }

        // Reminders / checklists minimal implementations
        @Override public List<Reminder> getReminders(){ return Collections.emptyList(); }
        @Override public void addReminder(Reminder r){}
        @Override public void removeReminder(Reminder r){}
        @Override public List<Reminder> getDueReminders(int minutesAhead, java.util.Set<String> openedChecklists){ return Collections.emptyList(); }
        @Override public java.time.LocalDateTime getNextReminderTime(java.util.Set<String> openedChecklists){ return null; }
        @Override public java.util.Set<Checklist> getChecklists(){ return checklists; }
        @Override public void addChecklist(Checklist checklist){ checklists.add(checklist); }
        @Override public void removeChecklist(Checklist checklist){ checklists.remove(checklist); }
        @Override public void updateChecklistName(Checklist checklist, String newName){}
        @Override public void shutdown(){}
    }

    @Before
    public void setUp() {
        InMemoryTaskRepository repo = new InMemoryTaskRepository();
        taskManager = new TaskManager(repo);
    }

    @Test
    public void testAddAndGetTask() {
        Task t = new Task("InMem", TaskType.MORNING, "MONDAY", "cid", null);
        taskManager.addTask(t);
        Task out = taskManager.getTaskById(t.getId());
        assertNotNull(out);
        assertEquals(t.getName(), out.getName());
    }

    @Test
    public void testSubtasksFlow() {
        Task parent = new Task("Parent", TaskType.CUSTOM, null, "cid", null);
        taskManager.addTask(parent);
        Task sub = new Task("Sub", TaskType.CUSTOM, null, "cid", parent.getId());
        taskManager.addTask(sub);
        List<Task> subs = taskManager.getSubtasks(parent.getId());
        assertEquals(1, subs.size());
        assertEquals(parent.getId(), subs.get(0).getParentId());
    }

    @Test
    public void testUpdateAndRemove() {
        Task t = new Task("X", TaskType.MORNING, "MONDAY", "cid", null);
        taskManager.addTask(t);
        t.setName("Y");
        taskManager.updateTask(t);
        Task got = taskManager.getTaskById(t.getId());
        assertEquals("Y", got.getName());
        taskManager.removeTask(t);
        assertNull(taskManager.getTaskById(t.getId()));
    }
}
