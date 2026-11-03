import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for invalid HEADING cleanup and menu label correctness.
 */
public class InvalidHeadingCleanupTest {
    
    @Test
    public void testInvalidHeadingsRemovedOnLoad() {
        // Given: tasks including an invalid HEADING with null parentId
        List<Task> tasks = java.util.List.of(
            new Task("Morning Task", TaskType.MORNING, "id1"),
            new Task("Valid Heading", TaskType.HEADING, "id2", null, "id1"),
            new Task("Invalid Heading", TaskType.HEADING, "id3") // Missing parentId
        );
        
        // When: creating a snapshot (which validates task structure)
        TaskMovePlanner.Snapshot snapshot = TaskMovePlanner.snapshot(tasks);
        
        // Then: the invalid HEADING should be cleaned up
        // The snapshot should only contain the valid tasks
        assertEquals(2, snapshot.tasks().size());
        assertTrue(snapshot.tasks().stream()
            .noneMatch(t -> t.type() == TaskType.HEADING && t.parentId() == null));
    }

    @Test
    public void testHeadingWithNullParentIdIsInvalid() {
        // Given: a HEADING with parentId == null
        List<Task> tasks = java.util.List.of(
            new Task("Invalid Heading", TaskType.HEADING, "h1")
        );
        
        // When: planning a move with this snapshot
        TaskMovePlanner.Snapshot snapshot = TaskMovePlanner.snapshot(tasks);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            java.util.List.of("h1"),
            new TaskMovePlanner.Destination(TaskType.MORNING, null),
            null,
            null
        );
        
        // Then: the plan should reject with INVALID_SNAPSHOT
        TaskMovePlanner.Result result = TaskMovePlanner.plan(snapshot, intent);
        assertFalse(result.accepted());
        assertEquals(TaskMovePlanner.Rejection.INVALID_SNAPSHOT, result.rejection());
    }

    @Test
    public void testValidHeadingWithParentIdAccepted() {
        // Given: a HEADING with valid parentId
        Task parent = new Task("Parent Task", TaskType.MORNING, "p1");
        Task heading = new Task("Valid Heading", TaskType.HEADING, "h1", null, "p1");
        Task child = new Task("Child Task", TaskType.MORNING, "c1", "p1", "p1");
        
        List<Task> tasks = java.util.List.of(parent, heading, child);
        
        // When: creating a snapshot
        TaskMovePlanner.Snapshot snapshot = TaskMovePlanner.snapshot(tasks);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            java.util.List.of("c1"),
            new TaskMovePlanner.Destination(TaskType.MORNING, null),
            null,
            null
        );
        
        // Then: snapshot should be valid and contain all tasks, and plan should accept
        assertEquals(3, snapshot.tasks().size());
        assertTrue(snapshot.tasks().stream()
            .filter(t -> t.type() == TaskType.HEADING)
            .allMatch(t -> t.parentId() != null));
        
        TaskMovePlanner.Result result = TaskMovePlanner.plan(snapshot, intent);
        assertTrue("Should accept valid structure", result.accepted());
    }

    @Test
    public void testHeadingAndParentChildStructure() {
        // Given: proper parent-child-heading structure
        Task parent = new Task("Parent", TaskType.MORNING, "p");
        Task heading = new Task("My Heading", TaskType.HEADING, "h", null, "p");
        Task child = new Task("Child", TaskType.MORNING, "c", "p", "p");
        
        List<Task> tasks = java.util.List.of(parent, heading, child);
        
        // When: creating a snapshot and planning a move
        TaskMovePlanner.Snapshot snapshot = TaskMovePlanner.snapshot(tasks);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            java.util.List.of("c"),
            new TaskMovePlanner.Destination(TaskType.MORNING, null),
            null,
            null
        );
        
        // Then: the move should be accepted
        TaskMovePlanner.Result result = TaskMovePlanner.plan(snapshot, intent);
        assertTrue("Move should be accepted for valid structure", result.accepted());
    }

    @Test
    public void testMultipleInvalidHeadingsAreRemoved() {
        // Given: multiple invalid HEADINGs (with null parentId)
        List<Task> tasks = java.util.List.of(
            new Task("Task1", TaskType.MORNING, "t1"),
            new Task("Bad Heading 1", TaskType.HEADING, "h1"), // Invalid
            new Task("Task2", TaskType.MORNING, "t2"),
            new Task("Bad Heading 2", TaskType.HEADING, "h2")  // Invalid
        );
        
        // When: creating a snapshot
        TaskMovePlanner.Snapshot snapshot = TaskMovePlanner.snapshot(tasks);
        
        // Then: only valid tasks remain
        assertEquals(2, snapshot.tasks().size());
        assertTrue(snapshot.tasks().stream()
            .allMatch(t -> t.type() != TaskType.HEADING));
    }

    @Test
    public void testParentChildHeadingMove() {
        // Given: parent -> [heading, child] structure
        Task parent = new Task("Parent", TaskType.EVENING, "p");
        Task heading = new Task("Workout", TaskType.HEADING, "h", null, "p");
        Task child1 = new Task("Running", TaskType.EVENING, "c1", "p", "p");
        Task child2 = new Task("Stretching", TaskType.EVENING, "c2", "p", "p");
        
        List<Task> tasks = java.util.List.of(parent, heading, child1, child2);
        
        // When: planning a move for child1 to a different parent
        TaskMovePlanner.Snapshot snapshot = TaskMovePlanner.snapshot(tasks);
        Task parent2 = new Task("Parent2", TaskType.EVENING, "p2");
        List<Task> allTasks = new java.util.ArrayList<>(tasks);
        allTasks.add(parent2);
        
        snapshot = TaskMovePlanner.snapshot(allTasks);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            java.util.List.of("c1"),
            new TaskMovePlanner.Destination(TaskType.EVENING, null),
            null,
            "p2"
        );
        
        // Then: the move should be accepted
        TaskMovePlanner.Result result = TaskMovePlanner.plan(snapshot, intent);
        assertTrue("Moving child to different parent should work", result.accepted());
    }

    @Test
    public void testSnapshotEqualityWithAndWithoutInvalidHeadings() {
        // Given: two snapshots - one with invalid heading, one without
        List<Task> validTasks = java.util.List.of(
            new Task("Task", TaskType.MORNING, "t1")
        );
        
        List<Task> withInvalidHeading = java.util.List.of(
            new Task("Task", TaskType.MORNING, "t1"),
            new Task("Bad Heading", TaskType.HEADING, "h1") // Invalid
        );
        
        // When: creating snapshots (invalid heading will be cleaned)
        TaskMovePlanner.Snapshot valid = TaskMovePlanner.snapshot(validTasks);
        TaskMovePlanner.Snapshot invalid = TaskMovePlanner.snapshot(withInvalidHeading);
        
        // Then: they should be equal after cleanup
        assertEquals(valid, invalid);
    }

    @Test
    public void testNestedHeadingsAreInvalid() {
        // Given: a HEADING as child of another HEADING
        Task parentTask = new Task("Parent", TaskType.MORNING, "task1");
        Task parentHeading = new Task("Parent Heading", TaskType.HEADING, "h1", null, "task1");
        Task childHeading = new Task("Child Heading", TaskType.HEADING, "h2", null, "h1");
        
        List<Task> tasks = java.util.List.of(parentTask, parentHeading, childHeading);
        
        // When: creating a snapshot
        TaskMovePlanner.Snapshot snapshot = TaskMovePlanner.snapshot(tasks);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            java.util.List.of("h2"),
            new TaskMovePlanner.Destination(TaskType.MORNING, null),
            null,
            null
        );
        
        // Then: the plan should reject because HEADING parent of HEADING is invalid
        TaskMovePlanner.Result result = TaskMovePlanner.plan(snapshot, intent);
        assertFalse("Nested HEADINGs should be rejected", result.accepted());
    }
}
