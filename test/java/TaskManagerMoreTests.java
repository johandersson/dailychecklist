import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class TaskManagerMoreTests {

    @Test
    public void subtasksRemovedWhenParentRemoved() {
        TestHelpers.InMemoryTaskRepository repo = new TestHelpers.InMemoryTaskRepository();
        TaskManager mgr = new TaskManager(repo);

        Task parent = new Task("Parent", TaskType.CUSTOM, null, "chk-1");
        Task child1 = new Task("Child1", TaskType.CUSTOM, null, "chk-1", parent.getId());
        Task child2 = new Task("Child2", TaskType.CUSTOM, null, "chk-1", parent.getId());

        mgr.addTask(parent);
        mgr.addTask(child1);
        mgr.addTask(child2);

        List<Task> subs = mgr.getSubtasks(parent.getId());
        assertEquals(2, subs.size());

        mgr.removeTask(parent);

        // After removal, parent and subtasks should be gone
        assertNull(mgr.getTaskById(parent.getId()));
        assertNull(mgr.getTaskById(child1.getId()));
        assertNull(mgr.getTaskById(child2.getId()));
    }

    @Test
    public void moveTaskToChecklistUpdatesChecklistId() {
        TestHelpers.InMemoryTaskRepository repo = new TestHelpers.InMemoryTaskRepository();
        TaskManager mgr = new TaskManager(repo);

        Checklist a = new Checklist("A");
        Checklist b = new Checklist("B");
        repo.addChecklist(a);
        repo.addChecklist(b);

        Task t = new Task("Task", TaskType.CUSTOM, null, a.getId());
        mgr.addTask(t);

        mgr.moveTaskToChecklist(t, b);
        Task got = mgr.getTaskById(t.getId());
        assertNotNull(got);
        assertEquals(b.getId(), got.getChecklistId());
    }

    @Test
    public void updatePreservesChecklistIdWhenNull() {
        TestHelpers.InMemoryTaskRepository repo = new TestHelpers.InMemoryTaskRepository();
        TaskManager mgr = new TaskManager(repo);

        Task t = new Task("Task2", TaskType.CUSTOM, null, "chk-99");
        mgr.addTask(t);

        // Create a partial task with same id but null checklistId
        Task partial = new Task(t.getId(), "Task2-renamed", TaskType.CUSTOM, null, false, null, null, null);
        partial.setChecklistId(null);
        mgr.updateTask(partial);

        Task got = mgr.getTaskById(t.getId());
        assertNotNull(got);
        assertEquals("Task2-renamed", got.getName());
        // checklistId should be preserved
        assertEquals("chk-99", got.getChecklistId());
    }

    @Test
    public void setTasksSanitizesHeadings() {
        TestHelpers.InMemoryTaskRepository repo = new TestHelpers.InMemoryTaskRepository();
        TaskManager mgr = new TaskManager(repo);

        // valid parent
        Task p = new Task("P", TaskType.CUSTOM, null, "chk-x");
        mgr.addTask(p);

        // heading with no parentId -> should be removed
        Task badHeading = new Task("h1", TaskType.HEADING, null, null);
        badHeading.setParentId(null);

        // valid heading referencing parent
        Task goodHeading = new Task("h2", TaskType.HEADING, null, null);
        goodHeading.setParentId(p.getId());

        List<Task> incoming = new ArrayList<>();
        incoming.add(p);
        incoming.add(badHeading);
        incoming.add(goodHeading);

        mgr.setTasks(incoming);

        // After sanitize, badHeading should be removed
        List<Task> all = mgr.getAllTasks();
        boolean hasBad = all.stream().anyMatch(t -> t.getType() == TaskType.HEADING && t.getParentId() == null);
        assertFalse(hasBad);
        // good heading should remain
        boolean hasGood = all.stream().anyMatch(t -> t.getType() == TaskType.HEADING && p.getId().equals(t.getParentId()));
        assertTrue(hasGood);
    }
}
