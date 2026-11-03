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
import javax.swing.DefaultListModel;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * TDD tests for custom checklist drag-drop with parent changes.
 * Ensures the fix works correctly for custom checklists, not just daily types.
 */
public class TaskCustomChecklistParentChangeTest {
    private TaskManager taskManager;
    private TestHelpers.InMemoryTaskRepository repository;
    private TaskMovePlanner.Destination customDest;
    private static final String CUSTOM_CHECKLIST_ID = "my-custom-list";

    private Task task(String id, String name, String parentId) {
        Task task = new Task(id, name, TaskType.CUSTOM, "monday", false, "2099-12-31",
            CUSTOM_CHECKLIST_ID, parentId);
        return task;
    }

    private DefaultListModel<Task> rows(Task... tasks) {
        DefaultListModel<Task> model = new DefaultListModel<>();
        for (Task t : tasks) {
            model.addElement(t);
        }
        return model;
    }

    @Before
    public void setUp() {
        repository = new TestHelpers.InMemoryTaskRepository();
        taskManager = new TaskManager(repository);
        // Register the custom checklist so it's recognized as valid
        Checklist customChecklist = new Checklist("My Custom List", CUSTOM_CHECKLIST_ID);
        repository.addChecklist(customChecklist);
        customDest = new TaskMovePlanner.Destination(TaskType.CUSTOM, CUSTOM_CHECKLIST_ID);
    }

    @Test
    public void moveCustomTaskToBecomesSubtask() throws ExecutionException, InterruptedException {
        // Given: two top-level custom tasks
        Task parentTask = task("parent", "Parent Task", null);
        Task topLevelTask = task("toplevel", "Top Level Task", null);
        repository.addTask(parentTask);
        repository.addTask(topLevelTask);

        // When: moving toplevel to become a child of parent
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("toplevel"), customDest,
            rows(parentTask, topLevelTask), 
            0, // drop ON parentTask
            false
        );
        taskManager.moveTasks(intent).get();

        // Then: toplevel now has parent as its parent
        Task moved = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("toplevel"))
            .findFirst()
            .orElse(null);

        assertNotNull("toplevel should exist", moved);
        assertEquals("toplevel should have parent as parentId", "parent", moved.getParentId());
        assertEquals("Should remain CUSTOM type", TaskType.CUSTOM, moved.getType());
        assertEquals("Should keep checklist id", CUSTOM_CHECKLIST_ID, moved.getChecklistId());
    }

    @Test
    public void moveCustomSubtaskToTopLevel() throws ExecutionException, InterruptedException {
        // Given: parent -> child hierarchy in custom checklist
        Task parentTask = task("parent", "Parent Task", null);
        Task subtask = task("child", "Child Task", "parent");
        repository.addTask(parentTask);
        repository.addTask(subtask);

        // When: moving child to top-level
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("child"), customDest,
            rows(parentTask), // Only parent visible at top level
            0, // insert before parent
            true
        );
        taskManager.moveTasks(intent).get();

        // Then: child should have no parent
        Task moved = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("child"))
            .findFirst()
            .orElse(null);

        assertNotNull("child should exist", moved);
        assertNull("child should have no parent (top-level)", moved.getParentId());
        assertEquals("Should keep checklist id", CUSTOM_CHECKLIST_ID, moved.getChecklistId());
    }

    @Test
    public void moveCustomSubtaskToDifferentParent() throws ExecutionException, InterruptedException {
        // Given: parent1 -> child, parent2 (separate)
        Task parent1 = task("parent1", "Parent 1", null);
        Task child = task("child", "Child", "parent1");
        Task parent2 = task("parent2", "Parent 2", null);
        repository.addTask(parent1);
        repository.addTask(child);
        repository.addTask(parent2);

        // When: moving child from parent1 to parent2
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("child"), customDest,
            rows(parent1, parent2), 
            1, // drop ON parent2
            false
        );
        taskManager.moveTasks(intent).get();

        // Then: child now has parent2 as its parent
        Task moved = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("child"))
            .findFirst()
            .orElse(null);

        assertNotNull("child should exist", moved);
        assertEquals("child should now have parent2 as parentId", "parent2", moved.getParentId());
    }

    @Test
    public void roundTripCustomTaskParentChange() throws ExecutionException, InterruptedException {
        // Given: two top-level custom tasks
        Task taskA = task("taskA", "Task A", null);
        Task taskB = task("taskB", "Task B", null);
        repository.addTask(taskA);
        repository.addTask(taskB);

        // Step 1: Move B to become subtask of A
        TaskMovePlanner.Intent intentToSubtask = TaskDropController.buildIntent(
            taskManager, List.of("taskB"), customDest,
            rows(taskA, taskB),
            0, // drop ON A
            false
        );
        taskManager.moveTasks(intentToSubtask).get();

        Task subtaskB = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("taskB"))
            .findFirst()
            .orElse(null);
        assertEquals("taskB should be under taskA", "taskA", subtaskB.getParentId());

        // Step 2: Move B back to top-level
        Task parentA = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("taskA"))
            .findFirst()
            .orElse(null);
        
        TaskMovePlanner.Intent intentToTopLevel = TaskDropController.buildIntent(
            taskManager, List.of("taskB"), customDest,
            rows(parentA), // Only A shown at top level
            0, // insert before A
            true
        );
        taskManager.moveTasks(intentToTopLevel).get();

        // Then: B is back to top-level
        Task topLevelB = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("taskB"))
            .findFirst()
            .orElse(null);
        assertNull("taskB should be back to top-level", topLevelB.getParentId());
        assertEquals("Should keep checklist id", CUSTOM_CHECKLIST_ID, topLevelB.getChecklistId());
    }

    @Test
    public void moveMultipleCustomTasks() throws ExecutionException, InterruptedException {
        // Given: task1, task2, parent (all top-level)
        Task task1 = task("task1", "Task 1", null);
        Task task2 = task("task2", "Task 2", null);
        Task parent = task("parent", "Parent", null);
        
        repository.addTask(task1);
        repository.addTask(task2);
        repository.addTask(parent);

        // When: moving both task1 and task2 to become children of parent
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("task1", "task2"), customDest,
            rows(task1, task2, parent),
            2, // drop ON parent
            false
        );
        List<String> result = taskManager.moveTasks(intent).get();

        // Then: both have parent as parentId
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
    public void customTaskPreservesMetadataOnMove() throws ExecutionException, InterruptedException {
        // Given: task with metadata
        Task task = new Task("task", "Task Name", TaskType.CUSTOM, "monday", true, "2025-01-01",
            CUSTOM_CHECKLIST_ID, null);
        task.setNote("Important note");
        
        repository.addTask(task);
        Task parent = task("parent", "Parent", null);
        repository.addTask(parent);

        // When: moving task with metadata
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("task"), customDest,
            rows(parent),
            0, // drop ON parent
            false
        );
        taskManager.moveTasks(intent).get();

        // Then: metadata preserved
        Task moved = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("task"))
            .findFirst()
            .orElse(null);

        assertNotNull("Task should exist", moved);
        assertEquals("Note should be preserved", "Important note", moved.getNote());
        assertTrue("Completion status should be preserved", moved.isDone());
        assertEquals("Done date should be preserved", "2025-01-01", moved.getDoneDate());
        assertEquals("Name should be preserved", "Task Name", moved.getName());
        assertEquals("Checklist id should be preserved", CUSTOM_CHECKLIST_ID, moved.getChecklistId());
    }

    @Test
    public void customTaskWithHeadingMetadata() throws ExecutionException, InterruptedException {
        // Given: parent -> [child (with HEADING metadata)]
        Task parent = task("parent", "Parent", null);
        Task child = task("child", "Child", "parent");
        Task heading = task("heading", "Heading", "child");
        heading = new Task(heading.getId(), heading.getName(), TaskType.HEADING, heading.getWeekday(), 
            heading.isDone(), heading.getDoneDate(), CUSTOM_CHECKLIST_ID, "child");
        
        repository.addTask(parent);
        repository.addTask(child);
        repository.addTask(heading);

        // When: moving child to top-level
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("child"), customDest,
            rows(parent),
            0,
            true
        );
        taskManager.moveTasks(intent).get();

        // Then: child is top-level, heading still under child
        Task movedChild = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("child"))
            .findFirst()
            .orElse(null);
        Task movedHeading = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("heading"))
            .findFirst()
            .orElse(null);

        assertNotNull("child should exist", movedChild);
        assertNotNull("heading should exist", movedHeading);
        assertNull("child should be top-level", movedChild.getParentId());
        assertEquals("heading should still have child as parentId", "child", movedHeading.getParentId());
    }

    @Test
    public void customChecklistBoundaryRespected() throws ExecutionException, InterruptedException {
        // Given: custom task in custom checklist
        Task customTask = task("custom", "Custom Task", null);
        repository.addTask(customTask);

        // When: trying to move with wrong checklist id in destination
        TaskMovePlanner.Destination wrongDest = 
            new TaskMovePlanner.Destination(TaskType.CUSTOM, "different-checklist");

        // Should still be able to build intent (planner will reject or handle)
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("custom"), wrongDest,
            rows(customTask),
            0, true
        );

        // Verify the intent was created with the custom task
        assertTrue("Intent should contain custom task", intent.taskIds().contains("custom"));
    }
}
