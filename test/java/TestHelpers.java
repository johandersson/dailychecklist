import java.util.*;

/**
 * Lightweight in-memory TaskRepository for deterministic unit tests.
 */
public class TestHelpers {
    public static class InMemoryTaskRepository implements TaskRepository {
        private final Map<String, Task> map = new LinkedHashMap<>();
        private final Set<Checklist> checklists = new HashSet<>();

        @Override public void initialize() {}
        @Override public List<Task> getDailyTasks() { return new ArrayList<>(map.values()); }
        @Override public List<Task> getAllTasks() { return new ArrayList<>(map.values()); }
        @Override public void addTask(Task task) { map.put(task.getId(), task); }
        @Override public void updateTask(Task task) { map.put(task.getId(), task); }
        @Override public void removeTask(Task task) { map.remove(task.getId()); }
        @Override public boolean hasUndoneTasks() { return map.values().stream().anyMatch(t -> !t.isDone()); }
        @Override public void setTasks(List<Task> tasks) { map.clear(); if (tasks!=null) tasks.forEach(t->map.put(t.getId(), t)); }

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
}
