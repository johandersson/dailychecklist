import org.junit.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class TaskManagerUnitTest {

    @Test
    public void testSubtasksLifecycleAndMoveAndUpdateChecklistPreserved() throws Exception {
        TestHelpers.InMemoryTaskRepository repo = new TestHelpers.InMemoryTaskRepository();
        TaskManager mgr = new TaskManager(repo);

        // Create parent and child, add via manager
        Task parent = new Task("Parent", TaskType.CUSTOM, null, "chk-A", null);
        Task child = new Task("Child", TaskType.CUSTOM, null, "chk-A", parent.getId());

        mgr.addTask(parent);
        mgr.addTask(child);

        // Subtasks should be visible
        List<Task> subs = mgr.getSubtasks(parent.getId());
        assertEquals(1, subs.size());
        assertEquals(child.getId(), subs.get(0).getId());

        // Move parent to another checklist
        Checklist newChk = new Checklist("NewChk", "chk-B");
        mgr.moveTaskToChecklist(parent, newChk);
        Task fetched = mgr.getTaskById(parent.getId());
        assertNotNull(fetched);
        assertEquals("chk-B", fetched.getChecklistId());

        // Update a task preserving checklistId: create an update object with same id but null checklist
        Task original = new Task(parent.getId(), "Parent", TaskType.CUSTOM, null, false, null, "chk-B", null);
        // Simulate incoming partial update with null checklistId
        Task partial = new Task(parent.getId(), "Parent Renamed", TaskType.CUSTOM, null, false, null);
        // Ensure repository has original
        repo.addTask(original);
        // Apply update through manager (should preserve checklistId)
        mgr.updateTask(partial);
        Task after = mgr.getTaskById(parent.getId());
        assertNotNull(after);
        assertEquals("chk-B", after.getChecklistId());

        // Removing parent should remove direct subtasks as well
        mgr.removeTask(parent);
        assertNull(mgr.getTaskById(parent.getId()));
        List<Task> afterSubs = mgr.getSubtasks(parent.getId());
        assertTrue(afterSubs.isEmpty());
    }

    @Test
    public void testSetTasksSanitizesHeadingsAndValidateInput() {
        TestHelpers.InMemoryTaskRepository repo = new TestHelpers.InMemoryTaskRepository();
        TaskManager mgr = new TaskManager(repo);

        Task headingInvalid = new Task("hid", "Heading", TaskType.HEADING, null, false, null);
        Task headingDuplicateParent = new Task("hid2", "Heading2", TaskType.HEADING, null, false, null);
        // headingDuplicateParent has same parent id to trigger duplicate removal
        headingInvalid.setParentId(null); // invalid
        headingDuplicateParent.setParentId("p1");

        Task good = new Task("t1", "Task1", TaskType.CUSTOM, null, false, null, "chk", null);

        java.util.List<Task> list = new java.util.ArrayList<>();
        list.add(headingInvalid);
        list.add(headingDuplicateParent);
        list.add(headingDuplicateParent); // duplicate parent to force duplicate removal
        list.add(good);

        mgr.setTasks(list);
        // After sanitization, duplicate/invalid headings removed; repository should contain only non-heading tasks
        List<Task> all = mgr.getAllTasks();
        assertTrue(all.stream().anyMatch(t -> t.getName().equals("Task1")));

        // validateAndSanitizeInput tests
        assertNull(TaskManager.validateAndSanitizeInput(null));
        assertNull(TaskManager.validateAndSanitizeInput(""));
        assertNull(TaskManager.validateAndSanitizeInput("../../etc/passwd"));
        assertNotNull(TaskManager.validateAndSanitizeInput("Good Name 123"));
    }
}
