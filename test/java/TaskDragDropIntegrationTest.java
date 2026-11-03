/*
 * Daily Checklist
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * End-to-end integration tests for drag-and-drop operations.
 * Tests the complete flow from UI intent through repository commit.
 */
public class TaskDragDropIntegrationTest {
    private TaskManager taskManager;
    private TestHelpers.InMemoryTaskRepository repository;

    private Task task(String id, TaskType type, String parent) {
        Task task = new Task(id, id, type, "monday", false, "2099-12-31",
            type == TaskType.CUSTOM ? "custom-id" : null, parent);
        task.setNote("note: " + id);
        return task;
    }

    @Before
    public void setUp() {
        repository = new TestHelpers.InMemoryTaskRepository();
        taskManager = new TaskManager(repository);
    }

    @Test
    public void dragTopLevelTaskUnderAnotherTask() throws ExecutionException, InterruptedException {
        // Setup: Two top-level MORNING tasks
        Task parent = task("parent", TaskType.MORNING, null);
        Task child = task("child", TaskType.MORNING, null);
        
        repository.addTask(parent);
        repository.addTask(child);
        
        // Act: Drag 'child' to become a subtask of 'parent'
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("child"), dest, null, "parent"
        );
        List<String> result = taskManager.moveTasks(intent).get();
        
        // Assert: child now has parent as its parent
        Task movedChild = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("child"))
            .findFirst()
            .orElse(null);
        
        assertNotNull("Child task should exist after move", movedChild);
        assertEquals("child should have parent as its parentId", "parent", movedChild.getParentId());
        assertEquals("child should still be MORNING type", TaskType.MORNING, movedChild.getType());
        assertTrue("result should contain child", result.contains("child"));
    }

    @Test
    public void dragChildTaskToTopLevel() throws ExecutionException, InterruptedException {
        // Setup: parent -> child hierarchy
        Task parent = task("parent", TaskType.MORNING, null);
        Task child = task("child", TaskType.MORNING, "parent");
        
        repository.addTask(parent);
        repository.addTask(child);
        
        // Act: Drag 'child' to top-level (remove parent)
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("child"), dest, null, null
        );
        taskManager.moveTasks(intent).get();
        
        // Assert: child has no parent
        Task movedChild = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("child"))
            .findFirst()
            .orElse(null);
        
        assertNotNull("Child task should exist after move", movedChild);
        assertNull("child should have no parent (top-level)", movedChild.getParentId());
    }

    @Test
    public void dragChildTaskToDifferentParent() throws ExecutionException, InterruptedException {
        // Setup: parent1 -> child, parent2 (siblings)
        Task parent1 = task("parent1", TaskType.MORNING, null);
        Task child = task("child", TaskType.MORNING, "parent1");
        Task parent2 = task("parent2", TaskType.MORNING, null);
        
        repository.addTask(parent1);
        repository.addTask(child);
        repository.addTask(parent2);
        
        // Act: Drag 'child' from parent1 to parent2
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("child"), dest, null, "parent2"
        );
        taskManager.moveTasks(intent).get();
        
        // Assert: child now has parent2 as parent
        Task movedChild = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("child"))
            .findFirst()
            .orElse(null);
        
        assertNotNull("Child task should exist after move", movedChild);
        assertEquals("child should now have parent2 as parentId", "parent2", movedChild.getParentId());
    }

    @Test
    public void dragTaskWithHeadingMetadata() throws ExecutionException, InterruptedException {
        // Setup: parent with HEADING metadata
        Task parent = task("parent", TaskType.MORNING, null);
        Task heading = task("heading", TaskType.HEADING, "parent");
        Task target = task("target", TaskType.MORNING, null);
        
        repository.addTask(parent);
        repository.addTask(heading);
        repository.addTask(target);
        
        // Act: Drag parent to become child of target (HEADING should follow)
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("parent"), dest, null, "target"
        );
        taskManager.moveTasks(intent).get();
        
        // Assert: Both parent and heading moved correctly
        Task movedParent = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("parent"))
            .findFirst()
            .orElse(null);
        Task movedHeading = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("heading"))
            .findFirst()
            .orElse(null);
        
        assertNotNull("Parent should exist", movedParent);
        assertNotNull("Heading should exist", movedHeading);
        assertEquals("parent should have target as parentId", "target", movedParent.getParentId());
        assertEquals("heading should still have parent as parentId", "parent", movedHeading.getParentId());
    }

    @Test
    public void moveTaskPreservesMetadata() throws ExecutionException, InterruptedException {
        // Setup: Task with note and completion status
        Task task = new Task("task", "Task Name", TaskType.MORNING, "monday", true, "2025-01-01",
            null, null);
        task.setNote("Important note");
        
        repository.addTask(task);
        
        Task target = task("target", TaskType.MORNING, null);
        repository.addTask(target);
        
        // Act: Move task with metadata
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("task"), dest, null, "target"
        );
        taskManager.moveTasks(intent).get();
        
        // Assert: Metadata preserved
        Task movedTask = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("task"))
            .findFirst()
            .orElse(null);
        
        assertNotNull("Task should exist", movedTask);
        assertEquals("Note should be preserved", "Important note", movedTask.getNote());
        assertTrue("Completion status should be preserved", movedTask.isDone());
        assertEquals("Done date should be preserved", "2025-01-01", movedTask.getDoneDate());
        assertEquals("Name should be preserved", "Task Name", movedTask.getName());
    }

    @Test
    public void moveMultipleTasksAsChildren() throws ExecutionException, InterruptedException {
        // Setup: Three independent top-level tasks
        Task task1 = task("task1", TaskType.MORNING, null);
        Task task2 = task("task2", TaskType.MORNING, null);
        Task parent = task("parent", TaskType.MORNING, null);
        
        repository.addTask(task1);
        repository.addTask(task2);
        repository.addTask(parent);
        
        // Act: Move both task1 and task2 to become children of parent
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("task1", "task2"), dest, null, "parent"
        );
        List<String> result = taskManager.moveTasks(intent).get();
        
        // Assert: Both have parent as parentId
        Task movedTask1 = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("task1"))
            .findFirst()
            .orElse(null);
        Task movedTask2 = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("task2"))
            .findFirst()
            .orElse(null);
        
        assertNotNull("task1 should exist", movedTask1);
        assertNotNull("task2 should exist", movedTask2);
        assertEquals("task1 should have parent as parentId", "parent", movedTask1.getParentId());
        assertEquals("task2 should have parent as parentId", "parent", movedTask2.getParentId());
        assertTrue("Result should contain task1", result.contains("task1"));
        assertTrue("Result should contain task2", result.contains("task2"));
    }

    @Test
    public void moveTaskBetweenDailyTypes() throws ExecutionException, InterruptedException {
        // Setup: MORNING task
        Task morningTask = task("m1", TaskType.MORNING, null);
        repository.addTask(morningTask);
        
        // Act: Move to EVENING
        TaskMovePlanner.Destination eveningDest = new TaskMovePlanner.Destination(TaskType.EVENING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("m1"), eveningDest, null, null
        );
        taskManager.moveTasks(intent).get();
        
        // Assert: Type changed to EVENING
        Task movedTask = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("m1"))
            .findFirst()
            .orElse(null);
        
        assertNotNull("Task should exist", movedTask);
        assertEquals("Task should be EVENING type now", TaskType.EVENING, movedTask.getType());
    }

    @Test
    public void moveClearsSubtaskCache() throws ExecutionException, InterruptedException {
        // Setup: parent -> child
        Task parent = task("parent", TaskType.MORNING, null);
        Task child = task("child", TaskType.MORNING, "parent");
        
        repository.addTask(parent);
        repository.addTask(child);
        
        // Prime cache
        assertNotNull("Subtasks should be available", taskManager.getSubtasks("parent"));
        
        // Act: Move child to different parent
        Task newParent = task("newparent", TaskType.MORNING, null);
        repository.addTask(newParent);
        
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("child"), dest, null, "newparent"
        );
        taskManager.moveTasks(intent).get();
        
        // Assert: Cache reflects new structure
        List<Task> oldParentSubs = taskManager.getSubtasks("parent");
        List<Task> newParentSubs = taskManager.getSubtasks("newparent");
        
        assertTrue("Old parent should have no subtasks", oldParentSubs.isEmpty());
        assertTrue("New parent should have child", newParentSubs.stream()
            .anyMatch(t -> t.getId().equals("child")));
    }

    @Test
    public void moveWithMultipleTasksTogether() throws ExecutionException, InterruptedException {
        // Setup: Multiple top-level tasks
        Task task1 = task("task1", TaskType.MORNING, null);
        Task task2 = task("task2", TaskType.MORNING, null);
        Task target = task("target", TaskType.MORNING, null);
        
        repository.addTask(task1);
        repository.addTask(task2);
        repository.addTask(target);
        
        // Act: Move both tasks to become children of target
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("task1", "task2"), dest, null, "target"
        );
        List<String> result = taskManager.moveTasks(intent).get();
        
        // Assert: Both tasks moved successfully
        assertEquals("Both tasks should be moved", 2, result.size());
        Task moved1 = repository.getAllTasks().stream().filter(t -> t.getId().equals("task1")).findFirst().orElse(null);
        Task moved2 = repository.getAllTasks().stream().filter(t -> t.getId().equals("task2")).findFirst().orElse(null);
        assertNotNull("task1 should exist", moved1);
        assertNotNull("task2 should exist", moved2);
        assertEquals("task1 should have new parent", "target", moved1.getParentId());
        assertEquals("task2 should have new parent", "target", moved2.getParentId());
    }

    @Test
    public void moveRespectsBoundaryConditions() throws ExecutionException, InterruptedException {
        // Setup: Single top-level task with a child
        Task task = task("task", TaskType.MORNING, null);
        Task subtask = task("subtask", TaskType.MORNING, "task");
        
        repository.addTask(task);
        repository.addTask(subtask);
        
        // Act: Try to move task under its own grandchild would be invalid
        // Instead, just verify that you can't nest more than 2 levels
        // So trying to move task to be child of subtask should fail
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("task"), dest, null, "subtask"
        );
        
        // Assert: Should fail (can't create grandchild scenario)
        try {
            taskManager.moveTasks(intent).get();
            fail("Should reject invalid nesting");
        } catch (ExecutionException e) {
            assertTrue("Should contain error message",
                e.getCause().getMessage() != null &&
                (e.getCause().getMessage().contains("GRANDCHILD") ||
                 e.getCause().getMessage().contains("Grandchild")));
        }
    }

    @Test
    public void moveReturnsCorrectSelectedIds() throws ExecutionException, InterruptedException {
        // Setup: Two top-level tasks
        Task parent = task("parent", TaskType.MORNING, null);
        Task other = task("other", TaskType.MORNING, null);
        
        repository.addTask(parent);
        repository.addTask(other);
        
        // Act: Move parent to become a child of other
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("parent"), dest, null, "other"
        );
        List<String> result = taskManager.moveTasks(intent).get();
        
        // Assert: Result contains only the explicitly selected task
        assertTrue("Result should contain parent", result.contains("parent"));
        assertEquals("Result should have exactly 1 element", 1, result.size());
    }
}
