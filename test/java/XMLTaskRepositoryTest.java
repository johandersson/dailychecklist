import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.List;

/**
 * Test class for XMLTaskRepository - the data persistence layer
 * Note: These tests use the in-memory repository to avoid file I/O complexity
 */
public class XMLTaskRepositoryTest {
    
    private XMLTaskRepository repository;
    
    @Before
    public void setUp() {
        repository = new XMLTaskRepository();
    }
    
    @Test
    public void testAddAndRetrieveTask() {
        Task task = new Task("Repository Test Task", TaskType.MORNING, "MONDAY", "test-checklist", null);
        repository.addTask(task);
        
        Task retrieved = repository.getTaskById(task.getId());
        assertNotNull("Task should be retrievable", retrieved);
        assertEquals("Task name should match", task.getName(), retrieved.getName());
        assertEquals("Task ID should match", task.getId(), retrieved.getId());
    }
    
    @Test
    public void testUpdateTask() {
        Task task = new Task("Original", TaskType.MORNING, "MONDAY", "test-checklist", null);
        repository.addTask(task);
        
        task.setName("Modified");
        task.setDone(true);
        repository.updateTask(task);
        
        Task retrieved = repository.getTaskById(task.getId());
        assertEquals("Name should be updated", "Modified", retrieved.getName());
        assertTrue("Completed status should be updated", retrieved.isDone());
    }
    
    @Test
    public void testRemoveTask() {
        Task task = new Task("To Remove", TaskType.MORNING, "MONDAY", "test-checklist", null);
        repository.addTask(task);
        
        assertNotNull("Task should exist", repository.getTaskById(task.getId()));
        repository.removeTask(task);
        assertNull("Task should be removed", repository.getTaskById(task.getId()));
    }
    
    @Test
    public void testGetAllTasks() {
        Task task1 = new Task("Task 1", TaskType.MORNING, "MONDAY", "test-checklist", null);
        Task task2 = new Task("Task 2", TaskType.EVENING, "TUESDAY", "test-checklist", null);
        
        repository.addTask(task1);
        repository.addTask(task2);
        
        List<Task> allTasks = repository.getAllTasks();
        assertTrue("Should have at least 2 tasks", allTasks.size() >= 2);
    }
    
    @Test
    public void testIncrementalMapUpdate() {
        // Test the O(1) addTaskToMaps optimization
        Task task1 = new Task("Task 1", TaskType.MORNING, "MONDAY", "test-checklist", null);
        Task task2 = new Task("Task 2", TaskType.MORNING, "MONDAY", "test-checklist", null);
        Task task3 = new Task("Task 3", TaskType.EVENING, "TUESDAY", "test-checklist", null);
        
        repository.addTask(task1);
        repository.addTask(task2);
        repository.addTask(task3);
        
        // Verify all tasks are accessible
        assertNotNull("Task 1 should be in repository", repository.getTaskById(task1.getId()));
        assertNotNull("Task 2 should be in repository", repository.getTaskById(task2.getId()));
        assertNotNull("Task 3 should be in repository", repository.getTaskById(task3.getId()));
    }
    
    @Test
    public void testSubtaskRelationships() {
        Task parent = new Task("Parent", TaskType.CUSTOM, null, "test-checklist", null);
        repository.addTask(parent);
        
        Task subtask1 = new Task("Subtask 1", TaskType.CUSTOM, null, "test-checklist", parent.getId());
        Task subtask2 = new Task("Subtask 2", TaskType.CUSTOM, null, "test-checklist", parent.getId());
        
        repository.addTask(subtask1);
        repository.addTask(subtask2);
        
        // Verify subtasks are associated with parent
        Task retrievedParent = repository.getTaskById(parent.getId());
        Task retrievedSub1 = repository.getTaskById(subtask1.getId());
        Task retrievedSub2 = repository.getTaskById(subtask2.getId());
        
        assertNotNull("Parent should exist", retrievedParent);
        assertNotNull("Subtask 1 should exist", retrievedSub1);
        assertNotNull("Subtask 2 should exist", retrievedSub2);
        assertEquals("Subtask 1 should reference parent", parent.getId(), retrievedSub1.getParentId());
        assertEquals("Subtask 2 should reference parent", parent.getId(), retrievedSub2.getParentId());
    }
    
    @Test
    public void testMassiveSubtaskAddition() {
        // Performance test for the O(1) optimization
        Task parent = new Task("Parent with many children", TaskType.CUSTOM, null, "perf-test", null);
        repository.addTask(parent);
        
        long startTime = System.currentTimeMillis();
        
        // Add 500 subtasks
        for (int i = 0; i < 500; i++) {
            Task subtask = new Task("Subtask " + i, TaskType.CUSTOM, null, "perf-test", parent.getId());
            repository.addTask(subtask);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // With O(1) optimization, this should be very fast (under 500ms)
        assertTrue("Adding 500 subtasks should be fast with O(1) optimization, took: " + duration + "ms", duration < 500);
        
        // Verify all can be retrieved
        assertNotNull("Parent should be retrievable", repository.getTaskById(parent.getId()));
    }
}
