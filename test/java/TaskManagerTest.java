import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.List;

/**
 * Test class for TaskManager
 * Note: These are lightweight tests that verify the core API without file I/O
 */
public class TaskManagerTest {
    
    private TaskManager taskManager;
    
    @Before
    public void setUp() {
        // Use the default constructor which creates an in-memory repository
        XMLTaskRepository repository = new XMLTaskRepository();
        taskManager = new TaskManager(repository);
    }
    
    @Test
    public void testAddTask() {
        Task task = new Task("New Task", TaskType.MORNING, "MONDAY", "test-checklist", null);
        taskManager.addTask(task);
        
        Task retrieved = taskManager.getTaskById(task.getId());
        assertNotNull("Task should be retrievable after adding", retrieved);
        assertEquals("Task name should match", task.getName(), retrieved.getName());
        assertEquals("Task ID should match", task.getId(), retrieved.getId());
    }
    
    @Test
    public void testUpdateTask() {
        Task task = new Task("Original Name", TaskType.MORNING, "MONDAY", "test-checklist", null);
        taskManager.addTask(task);
        
        String newName = "Updated Name";
        task.setName(newName);
        taskManager.updateTask(task);
        
        Task retrieved = taskManager.getTaskById(task.getId());
        assertEquals("Task name should be updated", newName, retrieved.getName());
    }
    
    @Test
    public void testRemoveTask() {
        Task task = new Task("Task to Remove", TaskType.MORNING, "MONDAY", "test-checklist", null);
        taskManager.addTask(task);
        
        assertNotNull("Task should exist before removal", taskManager.getTaskById(task.getId()));
        
        taskManager.removeTask(task);
        
        assertNull("Task should not exist after removal", taskManager.getTaskById(task.getId()));
    }
    
    @Test
    public void testGetSubtasks() {
        Task parent = new Task("Parent Task", TaskType.MORNING, "MONDAY", "test-checklist", null);
        taskManager.addTask(parent);
        
        Task subtask1 = new Task("Subtask 1", TaskType.MORNING, "MONDAY", "test-checklist", parent.getId());
        Task subtask2 = new Task("Subtask 2", TaskType.MORNING, "MONDAY", "test-checklist", parent.getId());
        
        taskManager.addTask(subtask1);
        taskManager.addTask(subtask2);
        
        List<Task> subtasks = taskManager.getSubtasks(parent.getId());
        assertEquals("Should have 2 subtasks", 2, subtasks.size());
        assertTrue("Should contain subtask1", subtasks.stream().anyMatch(t -> t.getId().equals(subtask1.getId())));
        assertTrue("Should contain subtask2", subtasks.stream().anyMatch(t -> t.getId().equals(subtask2.getId())));
    }
    
    @Test
    public void testRemoveTaskWithSubtasks() {
        Task parent = new Task("Parent", TaskType.CUSTOM, null, "test-checklist", null);
        taskManager.addTask(parent);
        
        Task subtask = new Task("Subtask", TaskType.CUSTOM, null, "test-checklist", parent.getId());
        taskManager.addTask(subtask);
        
        taskManager.removeTask(parent);
        
        assertNull("Parent should be removed", taskManager.getTaskById(parent.getId()));
        assertNull("Subtask should also be removed", taskManager.getTaskById(subtask.getId()));
    }
    
    @Test
    public void testMultipleSubtasksPerformance() {
        // Test the O(1) addTaskToMaps optimization with multiple subtasks
        Task parent = new Task("Parent", TaskType.CUSTOM, null, "test-checklist", null);
        taskManager.addTask(parent);
        
        long startTime = System.currentTimeMillis();
        
        // Add 100 subtasks
        for (int i = 0; i < 100; i++) {
            Task subtask = new Task("Subtask " + i, TaskType.CUSTOM, null, "test-checklist", parent.getId());
            taskManager.addTask(subtask);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify all subtasks are retrievable
        List<Task> subtasks = taskManager.getSubtasks(parent.getId());
        assertEquals("Should have 100 subtasks", 100, subtasks.size());
        
        // Should complete in reasonable time (under 1 second for 100 tasks)
        assertTrue("Adding 100 subtasks should be fast (< 1000ms), took: " + duration + "ms", duration < 1000);
    }
}
