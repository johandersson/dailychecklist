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
 * TDD tests for the exact user-reported issue:
 * - Top-level task → Subtask (works)
 * - Subtask → Top-level (fails) - this is the issue
 * - This should be a round-trip that works both ways
 */
public class TaskRoundTripParentChangeTest {
    private TaskManager taskManager;
    private TestHelpers.InMemoryTaskRepository repository;
    private TaskMovePlanner.Destination morningDest;

    private Task task(String id, TaskType type, String parent) {
        return new Task(id, id, type, "monday", false, "2099-12-31",
            type == TaskType.CUSTOM ? "custom-id" : null, parent);
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
        morningDest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
    }

    /**
     * Test the exact scenario the user reported:
     * 1. Create task "A" (top-level)
     * 2. Create task "B" (top-level)
     * 3. Drag B under A (B becomes subtask of A) → WORKS
     * 4. Drag B back to top-level (B becomes top-level again) → FAILS?
     */
    @Test
    public void roundTripTaskFromTopLevelToSubtaskAndBack() throws ExecutionException, InterruptedException {
        // Setup: Two top-level tasks
        Task taskA = task("taskA", TaskType.MORNING, null);
        Task taskB = task("taskB", TaskType.MORNING, null);
        repository.addTask(taskA);
        repository.addTask(taskB);

        // Step 1: Verify initial state
        Task initialB = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("taskB"))
            .findFirst()
            .orElse(null);
        assertNull("taskB should initially be top-level", initialB.getParentId());

        // Step 2: Move B to become subtask of A using buildIntent + moveTasks
        TaskMovePlanner.Intent intentToSubtask = TaskDropController.buildIntent(
            taskManager, List.of("taskB"), morningDest, 
            rows(taskA, taskB), 
            0, // drop at index 0 (on taskA)
            false // not insert - drop ON taskA
        );
        assertEquals("Intent should have A as parent", "taskA", intentToSubtask.parentId());
        
        taskManager.moveTasks(intentToSubtask).get();

        Task subtaskB = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("taskB"))
            .findFirst()
            .orElse(null);
        assertEquals("taskB should now have taskA as parent", "taskA", subtaskB.getParentId());

        // Step 3: Now try to move B back to top-level
        // Simulate UI view showing A and B together
        Task parentA = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("taskA"))
            .findFirst()
            .orElse(null);
        
        // When displaying taskA's children, we'd see B
        // To move B to top-level, we drop it at index 0 with insert=true (before all)
        TaskMovePlanner.Intent intentToTopLevel = TaskDropController.buildIntent(
            taskManager, List.of("taskB"), morningDest,
            rows(parentA), // Only A is visible at top level now
            0, // insert before A
            true
        );
        assertEquals("Intent should have null parent (top-level)", null, intentToTopLevel.parentId());
        
        taskManager.moveTasks(intentToTopLevel).get();

        // Step 4: Verify B is back to top-level
        Task topLevelB = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("taskB"))
            .findFirst()
            .orElse(null);
        assertNull("taskB should be back to top-level", topLevelB.getParentId());
    }

    /**
     * Test moving subtask to different parent (another failing scenario)
     */
    @Test
    public void moveSubtaskToDifferentParentViaUI() throws ExecutionException, InterruptedException {
        // Setup: A (top-level), B (subtask of A), C (top-level)
        Task taskA = task("taskA", TaskType.MORNING, null);
        Task taskB = task("taskB", TaskType.MORNING, "taskA");
        Task taskC = task("taskC", TaskType.MORNING, null);
        repository.addTask(taskA);
        repository.addTask(taskB);
        repository.addTask(taskC);

        // Act: Drag B from under A to under C
        // Simulate dropping B at C's position
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("taskB"), morningDest,
            rows(taskA, taskC), // Top-level view
            1, // drop at taskC position
            false // not insert
        );
        
        // The intent should indicate B should be under C
        assertEquals("B should be under C", "taskC", intent.parentId());
        
        taskManager.moveTasks(intent).get();

        // Assert
        Task movedB = repository.getAllTasks().stream()
            .filter(t -> t.getId().equals("taskB"))
            .findFirst()
            .orElse(null);
        assertEquals("taskB should now have taskC as parent", "taskC", movedB.getParentId());
    }

    /**
     * Test the specific UI intent building for moving to top-level
     */
    @Test
    public void buildIntentCorrectlyIdentifiesTopLevelDrop() {
        // Setup: A (top-level with child B)
        Task taskA = task("taskA", TaskType.MORNING, null);
        Task taskB = task("taskB", TaskType.MORNING, "taskA");
        repository.addTask(taskA);
        repository.addTask(taskB);

        // When: Building intent for dropping B at the top level (before A)
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("taskB"), morningDest,
            rows(taskA), // Only top-level shown
            0, // index 0 (before A)
            true // insert mode
        );

        // Then: Intent should indicate B is moving to top-level (parent = null)
        assertNull("Parent should be null for top-level drop", intent.parentId());
        assertEquals("Should be dropping before A", "taskA", intent.beforeTaskId());
    }

    /**
     * Test that moving task under another task works correctly
     */
    @Test
    public void buildIntentCorrectlyIdentifiesSubtaskDrop() {
        // Setup: A and B are top-level
        Task taskA = task("taskA", TaskType.MORNING, null);
        Task taskB = task("taskB", TaskType.MORNING, null);
        repository.addTask(taskA);
        repository.addTask(taskB);

        // When: Building intent for dropping B under A (making it a child)
        // Drop at index 0 in "not insert" mode means drop ON taskA -> nest under A
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("taskB"), morningDest,
            rows(taskA, taskB), // Both top-level
            0, // drop at index 0 (on taskA)
            false // not insert, so should nest
        );

        // Then: Intent should indicate B becomes child of A
        assertEquals("Parent should be A", "taskA", intent.parentId());
    }
}
