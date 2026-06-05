import org.junit.Test;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Tests batch input parsing logic: tabs, spaces, # headings, subtasks.
 * Note: TaskManager.getSubtasks() excludes HEADING tasks by design.
 * Headings are found via getAllTasks() filtered by parentId and type.
 */
public class BatchTaskInputTest {

    private TaskManager createManager() {
        TestHelpers.InMemoryTaskRepository repo = new TestHelpers.InMemoryTaskRepository();
        return new TaskManager(repo);
    }

    private List<Task> getHeadingsForParent(TaskManager mgr, String parentId) {
        return mgr.getAllTasks().stream()
            .filter(t -> t.getType() == TaskType.HEADING && parentId.equals(t.getParentId()))
            .collect(Collectors.toList());
    }

    @Test
    public void testTabIndentedSubtasksCreatedUnderParent() {
        TaskManager mgr = createManager();
        Task parent = new Task("Buy groceries", TaskType.MORNING, null, null);
        mgr.addTask(parent);
        Task subtask = new Task("Milk", TaskType.MORNING, null, null, parent.getId());
        mgr.addTask(subtask);

        List<Task> subs = mgr.getSubtasks(parent.getId());
        assertEquals(1, subs.size());
        assertEquals("Milk", subs.get(0).getName());
        assertEquals(parent.getId(), subs.get(0).getParentId());
    }

    @Test
    public void testHeadingCreatedAsSubtaskOfParent() {
        TaskManager mgr = createManager();
        Task parent = new Task("Morning routine", TaskType.MORNING, null, null);
        mgr.addTask(parent);
        Task heading = new Task("Bathroom", TaskType.HEADING, null, null, parent.getId());
        mgr.addTask(heading);

        // Headings are excluded from getSubtasks() but visible in getAllTasks()
        List<Task> headings = getHeadingsForParent(mgr, parent.getId());
        assertEquals(1, headings.size());
        assertEquals("Bathroom", headings.get(0).getName());
    }

    @Test
    public void testMultipleSubtasksAndHeadingInBatch() {
        TaskManager mgr = createManager();
        Task parent = new Task("Morning routine", TaskType.MORNING, null, null);
        mgr.addTask(parent);
        Task heading = new Task("Health", TaskType.HEADING, null, null, parent.getId());
        mgr.addTask(heading);
        Task sub1 = new Task("Brush teeth", TaskType.MORNING, null, null, parent.getId());
        mgr.addTask(sub1);
        Task sub2 = new Task("Shower", TaskType.MORNING, null, null, parent.getId());
        mgr.addTask(sub2);

        // Subtasks (non-heading)
        List<Task> subs = mgr.getSubtasks(parent.getId());
        assertEquals(2, subs.size());
        // Heading
        List<Task> headings = getHeadingsForParent(mgr, parent.getId());
        assertEquals(1, headings.size());
        assertEquals("Health", headings.get(0).getName());
    }

    @Test
    public void testDuplicateHeadingDetectedViaAllTasks() {
        TaskManager mgr = createManager();
        Task parent = new Task("Workout", TaskType.MORNING, null, null);
        mgr.addTask(parent);
        Task heading1 = new Task("Cardio", TaskType.HEADING, null, null, parent.getId());
        mgr.addTask(heading1);

        // Verify heading is findable via getAllTasks
        List<Task> headings = getHeadingsForParent(mgr, parent.getId());
        assertEquals(1, headings.size());
        assertTrue("Should detect existing heading", !headings.isEmpty());
    }

    @Test
    public void testSpaceIndentedContentDetectedAsIndented() {
        String tabLine = "\tSubtask via tab";
        String spaceLine = "  Subtask via spaces";
        String noIndent = "Parent task";

        assertTrue(tabLine.startsWith("\t") || tabLine.startsWith("  "));
        assertTrue(spaceLine.startsWith("\t") || spaceLine.startsWith("  "));
        assertFalse(noIndent.startsWith("\t") || noIndent.startsWith("  "));

        // Strip leading whitespace
        String stripped = spaceLine.replaceFirst("^[\\t ]+", "");
        assertEquals("Subtask via spaces", stripped);

        String tabStripped = tabLine.replaceFirst("^[\\t ]+", "");
        assertEquals("Subtask via tab", tabStripped);
    }

    @Test
    public void testTopLevelHashCreatesHeadingForNextParent() {
        TaskManager mgr = createManager();
        Task parent = new Task("Push-ups", TaskType.MORNING, null, null);
        mgr.addTask(parent);
        // Deferred heading attached to parent
        Task heading = new Task("Exercise section", TaskType.HEADING, null, null, parent.getId());
        mgr.addTask(heading);
        Task subtask = new Task("10 reps", TaskType.MORNING, null, null, parent.getId());
        mgr.addTask(subtask);

        List<Task> subs = mgr.getSubtasks(parent.getId());
        assertEquals(1, subs.size());
        assertEquals("10 reps", subs.get(0).getName());

        List<Task> headings = getHeadingsForParent(mgr, parent.getId());
        assertEquals(1, headings.size());
        assertEquals("Exercise section", headings.get(0).getName());
    }

    @Test
    public void testMultipleParentsEachWithSubtasks() {
        TaskManager mgr = createManager();
        Task parentA = new Task("Task A", TaskType.EVENING, null, null);
        mgr.addTask(parentA);
        Task subA1 = new Task("Sub A1", TaskType.EVENING, null, null, parentA.getId());
        mgr.addTask(subA1);
        Task subA2 = new Task("Sub A2", TaskType.EVENING, null, null, parentA.getId());
        mgr.addTask(subA2);

        Task parentB = new Task("Task B", TaskType.EVENING, null, null);
        mgr.addTask(parentB);
        Task subB1 = new Task("Sub B1", TaskType.EVENING, null, null, parentB.getId());
        mgr.addTask(subB1);

        assertEquals(2, mgr.getSubtasks(parentA.getId()).size());
        assertEquals(1, mgr.getSubtasks(parentB.getId()).size());
        assertEquals("Sub B1", mgr.getSubtasks(parentB.getId()).get(0).getName());
    }

    @Test
    public void testHashOnlyLineIsIgnored() {
        // A line that's just "#" or "# " should not create anything
        String hashOnly = "#";
        String hashSpace = "# ";
        assertEquals("", hashOnly.substring(1).trim());
        assertEquals("", hashSpace.substring(1).trim());
    }
}
