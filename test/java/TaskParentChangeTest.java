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
 * TDD tests for drag-drop scenarios involving parent changes:
 * - Moving a top-level task to become a subtask (WORKS)
 * - Moving a subtask back to top-level (FAILING)
 * - Moving a subtask to a different parent (FAILING)
 */
public class TaskParentChangeTest {
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

    /**
     * Scenario 1: Move top-level task to become a subtask (WORKING)
     * This should already work from the user report
     */
    @Test
    public void moveTopLevelTaskToBecomesSubtask() throws ExecutionException, InterruptedException {
        // Given: two top-level MORNING tasks
        Task parentTask = task("parent", TaskType.MORNING, null);
        Task topLevelTask = task("toplevel", TaskType.MORNING, null);
        repository.addTask(parentTask);
        repository.addTask(topLevelTask);

        // When: moving toplevel to become a child of parent
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("toplevel"), dest, null, "parent"
        );
        taskManager.moveTasks(intent).get();

        // Then: toplevel now has parent as its parent
        Task moved = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("toplevel"))
            .findFirst()
            .orElse(null);

        assertNotNull("toplevel should exist", moved);
        assertEquals("toplevel should have parent as parentId", "parent", moved.getParentId());
    }

    /**
     * Scenario 2: Move subtask back to top-level (FAILING)
     * A subtask should be able to become a top-level task
     */
    @Test
    public void moveSubtaskToTopLevel() throws ExecutionException, InterruptedException {
        // Given: parent -> child hierarchy
        Task parentTask = task("parent", TaskType.MORNING, null);
        Task subtask = task("child", TaskType.MORNING, "parent");
        repository.addTask(parentTask);
        repository.addTask(subtask);

        // When: moving child to top-level (parent = null)
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("child"), dest, null, null
        );
        taskManager.moveTasks(intent).get();

        // Then: child should have no parent
        Task moved = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("child"))
            .findFirst()
            .orElse(null);

        assertNotNull("child should exist", moved);
        assertNull("child should have no parent (top-level)", moved.getParentId());
    }

    /**
     * Scenario 3: Move subtask to a different parent (FAILING)
     * A subtask should be able to change its parent
     */
    @Test
    public void moveSubtaskToDifferentParent() throws ExecutionException, InterruptedException {
        // Given: parent1 -> child, parent2 (separate)
        Task parent1 = task("parent1", TaskType.MORNING, null);
        Task child = task("child", TaskType.MORNING, "parent1");
        Task parent2 = task("parent2", TaskType.MORNING, null);
        repository.addTask(parent1);
        repository.addTask(child);
        repository.addTask(parent2);

        // When: moving child from parent1 to parent2
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("child"), dest, null, "parent2"
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

    /**
     * Scenario 4: Complex scenario with multiple children
     * Moving parent -> child -> sibling structure when one child changes parent
     */
    @Test
    public void moveChildFromGroupWithMultipleChildren() throws ExecutionException, InterruptedException {
        // Given: parent1 -> [child1, child2], parent2
        Task parent1 = task("parent1", TaskType.MORNING, null);
        Task child1 = task("child1", TaskType.MORNING, "parent1");
        Task child2 = task("child2", TaskType.MORNING, "parent1");
        Task parent2 = task("parent2", TaskType.MORNING, null);
        
        repository.addTask(parent1);
        repository.addTask(child1);
        repository.addTask(child2);
        repository.addTask(parent2);

        // When: moving child2 to parent2
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("child2"), dest, null, "parent2"
        );
        taskManager.moveTasks(intent).get();

        // Then: child2 is under parent2, child1 still under parent1
        Task movedChild2 = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("child2"))
            .findFirst()
            .orElse(null);
        Task movedChild1 = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("child1"))
            .findFirst()
            .orElse(null);

        assertNotNull("child2 should exist", movedChild2);
        assertNotNull("child1 should exist", movedChild1);
        assertEquals("child2 should have parent2 as parentId", "parent2", movedChild2.getParentId());
        assertEquals("child1 should still have parent1 as parentId", "parent1", movedChild1.getParentId());
    }

    /**
     * Scenario 5: Move subtask with heading metadata to different parent
     * Headings should follow their parent when parent changes parent
     */
    @Test
    public void moveSubtaskWithHeadingToDifferentParent() throws ExecutionException, InterruptedException {
        // Given: parent1 -> [child (with HEADING metadata)], parent2
        Task parent1 = task("parent1", TaskType.MORNING, null);
        Task child = task("child", TaskType.MORNING, "parent1");
        Task heading = task("heading", TaskType.HEADING, "child");
        Task parent2 = task("parent2", TaskType.MORNING, null);
        
        repository.addTask(parent1);
        repository.addTask(child);
        repository.addTask(heading);
        repository.addTask(parent2);

        // When: moving child to parent2
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("child"), dest, null, "parent2"
        );
        taskManager.moveTasks(intent).get();

        // Then: child is under parent2, heading still under child
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
        assertEquals("child should have parent2 as parentId", "parent2", movedChild.getParentId());
        assertEquals("heading should still have child as parentId", "child", movedHeading.getParentId());
    }

    /**
     * Scenario 6: Move subtask to top-level when it has heading metadata
     */
    @Test
    public void moveSubtaskWithHeadingToTopLevel() throws ExecutionException, InterruptedException {
        // Given: parent -> [child (with HEADING metadata)]
        Task parent = task("parent", TaskType.MORNING, null);
        Task child = task("child", TaskType.MORNING, "parent");
        Task heading = task("heading", TaskType.HEADING, "child");
        
        repository.addTask(parent);
        repository.addTask(child);
        repository.addTask(heading);

        // When: moving child to top-level
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(
            List.of("child"), dest, null, null
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
}
