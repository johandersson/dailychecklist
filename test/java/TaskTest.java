import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Test class for Task entity
 */
public class TaskTest {
    
    private Task task;
    private Task parentTask;
    
    @Before
    public void setUp() {
        parentTask = new Task("Parent Task", TaskType.MORNING, "MONDAY", "checklist-1", null);
        task = new Task("Test Task", TaskType.MORNING, "MONDAY", "checklist-1", null);
    }
    
    @Test
    public void testTaskCreation() {
        assertNotNull("Task should not be null", task);
        assertEquals("Task name should match", "Test Task", task.getName());
        assertEquals("Task type should match", TaskType.MORNING, task.getType());
        assertEquals("Task weekday should match", "MONDAY", task.getWeekday());
        assertNotNull("Task ID should be generated", task.getId());
    }
    
    @Test
    public void testSubtaskCreation() {
        Task subtask = new Task("Subtask", TaskType.MORNING, "MONDAY", "checklist-1", parentTask.getId());
        assertNotNull("Subtask should not be null", subtask);
        assertEquals("Subtask parent ID should match", parentTask.getId(), subtask.getParentId());
        assertNotNull("Subtask ID should be generated", subtask.getId());
        assertNotEquals("Subtask ID should differ from parent ID", parentTask.getId(), subtask.getId());
    }
    
    @Test
    public void testTaskNameModification() {
        String originalName = task.getName();
        String newName = "Modified Task Name";
        task.setName(newName);
        assertEquals("Task name should be updated", newName, task.getName());
        assertNotEquals("Task name should differ from original", originalName, task.getName());
    }
    
    @Test
    public void testTaskCompletionToggle() {
        assertFalse("Task should start uncompleted", task.isDone());
        task.setDone(true);
        assertTrue("Task should be completed after toggle", task.isDone());
        task.setDone(false);
        assertFalse("Task should be uncompleted after second toggle", task.isDone());
    }
    
    @Test
    public void testTaskTypeValidation() {
        Task morningTask = new Task("Morning", TaskType.MORNING, "TUESDAY", "c1", null);
        Task eveningTask = new Task("Evening", TaskType.EVENING, "WEDNESDAY", "c2", null);
        Task customTask = new Task("Custom", TaskType.CUSTOM, null, "c3", null);
        
        assertEquals(TaskType.MORNING, morningTask.getType());
        assertEquals(TaskType.EVENING, eveningTask.getType());
        assertEquals(TaskType.CUSTOM, customTask.getType());
    }
    
    @Test
    public void testWeekdayAssignment() {
        String[] weekdays = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        for (String day : weekdays) {
            Task dayTask = new Task("Task", TaskType.MORNING, day, "checklist", null);
            assertEquals("Weekday should match", day, dayTask.getWeekday());
        }
    }
    
    @Test
    public void testTaskIdUniqueness() {
        Task task1 = new Task("Task 1", TaskType.MORNING, "MONDAY", "c1", null);
        Task task2 = new Task("Task 2", TaskType.MORNING, "MONDAY", "c1", null);
        Task task3 = new Task("Task 3", TaskType.EVENING, "FRIDAY", "c2", null);
        
        assertNotEquals("Task IDs should be unique", task1.getId(), task2.getId());
        assertNotEquals("Task IDs should be unique", task1.getId(), task3.getId());
        assertNotEquals("Task IDs should be unique", task2.getId(), task3.getId());
    }
    
    @Test
    public void testSubtaskList() {
        parentTask.getSubtasks().add(new Task("Subtask 1", TaskType.MORNING, "MONDAY", "c1", parentTask.getId()));
        parentTask.getSubtasks().add(new Task("Subtask 2", TaskType.MORNING, "MONDAY", "c1", parentTask.getId()));
        
        assertTrue("Parent should have subtasks", parentTask.hasSubtasks());
        assertEquals("Parent should have 2 subtasks", 2, parentTask.getSubtasks().size());
    }
}
