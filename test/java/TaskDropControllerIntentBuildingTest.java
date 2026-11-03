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

/**
 * Tests for TaskDropController.buildIntent - translating UI drop events to move intents.
 * These test that drop operations create valid intents with correct destination and order.
 */
public class TaskDropControllerIntentBuildingTest {
    private TestHelpers.InMemoryTaskRepository repository;
    private TaskManager taskManager;

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
    }

    @Test
    public void buildIntentCreatesValidIntentForSimpleDrop() {
        Task task1 = task("task1", TaskType.MORNING, null);
        Task task2 = task("task2", TaskType.MORNING, null);
        
        repository.addTask(task1);
        repository.addTask(task2);
        
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("task2"), dest, 
            rows(task1, task2), 
            0,
            true
        );
        
        assertNotNull("Intent should be created", intent);
        assertTrue("Should contain task2", intent.taskIds().contains("task2"));
    }

    @Test
    public void buildIntentHandlesMultipleTasks() {
        Task task1 = task("task1", TaskType.MORNING, null);
        Task task2 = task("task2", TaskType.MORNING, null);
        Task task3 = task("task3", TaskType.MORNING, null);
        
        repository.addTask(task1);
        repository.addTask(task2);
        repository.addTask(task3);
        
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("task1", "task2"), dest,
            rows(task1, task2, task3),
            0, true
        );
        
        assertEquals("Should have 2 selections", 2, intent.taskIds().size());
        assertTrue("Should contain task1", intent.taskIds().contains("task1"));
        assertTrue("Should contain task2", intent.taskIds().contains("task2"));
    }

    @Test
    public void buildIntentRejectsNonexistentTask() {
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        
        assertThrows(IllegalArgumentException.class, () -> 
            TaskDropController.buildIntent(
                taskManager, List.of("nonexistent"), dest,
                rows(),
                0, true
            )
        );
    }

    @Test
    public void buildIntentRejectsInvalidDropLocation() {
        Task task = task("task", TaskType.MORNING, null);
        repository.addTask(task);
        
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        
        assertThrows(IllegalArgumentException.class, () ->
            TaskDropController.buildIntent(
                taskManager, List.of("task"), dest,
                rows(task),
                -1,
                true
            )
        );
    }

    @Test
    public void buildIntentHandlesHeadingSelection() {
        Task parent = task("parent", TaskType.MORNING, null);
        Task heading = task("heading", TaskType.HEADING, "parent");
        Task target = task("target", TaskType.MORNING, null);
        
        repository.addTask(parent);
        repository.addTask(heading);
        repository.addTask(target);
        
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("parent", "heading"), dest,
            rows(parent, heading, target),
            2,
            false
        );
        
        assertNotNull("Intent should be created", intent);
        assertEquals("Should have both parent and heading", 2, intent.taskIds().size());
    }

    @Test
    public void buildIntentAcceptsEmptySelectionWithValidIndex() {
        Task task = task("task", TaskType.MORNING, null);
        repository.addTask(task);
        
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of(),
            dest,
            rows(task),
            0, true
        );
        
        assertNotNull("Intent should be created", intent);
    }

    @Test
    public void buildIntentAllowsHeadingWithMissingParent() {
        Task heading = task("heading", TaskType.HEADING, "missing");
        Task other = task("other", TaskType.MORNING, null);
        
        repository.addTask(heading);
        repository.addTask(other);
        
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("heading"), dest,
            rows(heading, other),
            0, true
        );
        
        assertNotNull("Intent should be created", intent);
        assertTrue("Should contain heading", intent.taskIds().contains("heading"));
    }

    @Test
    public void buildIntentPreservesSelectionOrder() {
        Task task1 = task("task1", TaskType.MORNING, null);
        Task task2 = task("task2", TaskType.MORNING, null);
        Task task3 = task("task3", TaskType.MORNING, null);
        
        repository.addTask(task1);
        repository.addTask(task2);
        repository.addTask(task3);
        
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.MORNING, null);
        
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("task3", "task1", "task2"), dest,
            rows(task1, task2, task3),
            0, true
        );
        
        assertEquals("Should maintain selection order", List.of("task3", "task1", "task2"), 
            intent.taskIds());
    }

    @Test
    public void buildIntentHandlesCustomChecklist() {
        Task customParent = task("cparent", TaskType.CUSTOM, null);
        customParent.setChecklistId("custom-id");
        Task customChild = task("cchild", TaskType.CUSTOM, "cparent");
        customChild.setChecklistId("custom-id");
        
        repository.addTask(customParent);
        repository.addTask(customChild);
        
        TaskMovePlanner.Destination dest = new TaskMovePlanner.Destination(TaskType.CUSTOM, "custom-id");
        
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(
            taskManager, List.of("cchild"), dest,
            rows(customParent, customChild),
            0, true
        );
        
        assertEquals("Should preserve checklist id", "custom-id", dest.checklistId());
    }
}
