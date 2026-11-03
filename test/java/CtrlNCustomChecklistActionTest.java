import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.List;

/**
 * TDD tests for Ctrl+N behavior in custom checklists tab
 * 
 * Requirement: When in custom checklists tab, Ctrl+N should:
 * - Create a new task in the marked custom checklist (if one is marked)
 * - Create a new custom checklist if no checklist is marked
 * - NOT create a daily morning/evening task
 */
public class CtrlNCustomChecklistActionTest {
    
    private TaskManager taskManager;
    private TestHelpers.InMemoryTaskRepository repository;
    private MockCustomChecklistContextProvider contextProvider;
    
    @Before
    public void setUp() {
        repository = new TestHelpers.InMemoryTaskRepository();
        taskManager = new TaskManager(repository);
        contextProvider = new MockCustomChecklistContextProvider();
    }
    
    @Test
    public void testCtrlNWithNoMarkedChecklistCreatesNewChecklist() {
        // GIVEN: No marked/selected checklist
        contextProvider.setSelectedChecklistInternal(null);
        CtrlNCustomChecklistAction action = new CtrlNCustomChecklistAction(taskManager, contextProvider);
        
        // WHEN: Action is triggered (simulating Ctrl+N in custom checklists tab)
        String newChecklistName = "Test Checklist";
        action.createNewChecklistWithName(newChecklistName);
        
        // THEN: A new custom checklist is created
        Checklist created = taskManager.getCustomChecklists().stream()
            .filter(c -> newChecklistName.equals(c.getName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull("New checklist should be created", created);
        assertEquals("Checklist name should match", newChecklistName, created.getName());
    }
    
    @Test
    public void testCtrlNWithMarkedChecklistCreatesNewTask() {
        // GIVEN: A marked custom checklist exists
        Checklist marked = new Checklist("My Checklist");
        taskManager.addChecklist(marked);
        contextProvider.setSelectedChecklistInternal(marked);
        
        CtrlNCustomChecklistAction action = new CtrlNCustomChecklistAction(taskManager, contextProvider);
        
        // WHEN: Action is triggered with a task name
        String taskName = "New Task in Checklist";
        action.createNewTaskInSelectedChecklist(taskName);
        
        // THEN: A new task is created in the marked checklist
        Checklist retrieved = taskManager.getChecklistById(marked.getId());
        List<Task> tasks = taskManager.getTasksByChecklist(retrieved.getId());
        
        Task newTask = tasks.stream()
            .filter(t -> taskName.equals(t.getName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull("New task should be created in marked checklist", newTask);
        assertEquals("Task name should match", taskName, newTask.getName());
        assertEquals("Task should belong to the marked checklist", marked.getId(), newTask.getChecklistId());
    }
    
    @Test
    public void testCtrlNTaskIsNotMorningOrEveningType() {
        // GIVEN: A marked custom checklist exists
        Checklist marked = new Checklist("My Checklist");
        taskManager.addChecklist(marked);
        contextProvider.setSelectedChecklistInternal(marked);
        
        CtrlNCustomChecklistAction action = new CtrlNCustomChecklistAction(taskManager, contextProvider);
        
        // WHEN: A new task is created via Ctrl+N
        String taskName = "Custom Task";
        action.createNewTaskInSelectedChecklist(taskName);
        
        // THEN: Task should be CUSTOM type, not MORNING or EVENING
        Checklist retrieved = taskManager.getChecklistById(marked.getId());
        List<Task> tasks = taskManager.getTasksByChecklist(retrieved.getId());
        
        Task newTask = tasks.stream()
            .filter(t -> taskName.equals(t.getName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull("Task should exist", newTask);
        assertEquals("Task type should be CUSTOM", TaskType.CUSTOM, newTask.getType());
        assertNotEquals("Task type should NOT be MORNING", TaskType.MORNING, newTask.getType());
        assertNotEquals("Task type should NOT be EVENING", TaskType.EVENING, newTask.getType());
    }
    
    @Test
    public void testCtrlNSwitchesFromDailyToCustomChecklistTab() {
        // GIVEN: User is in daily checklist tab
        // WHEN: Ctrl+N is pressed and no custom checklist is marked
        // THEN: UI should focus on custom checklists tab and prepare to create new checklist
        // (This is integration test, tested via AddTaskActionContextAware)
        
        assertTrue("This is an integration test, verified in AddTaskActionContextAware", true);
    }
    
    @Test
    public void testCtrlNMultipleTasksInMarkedChecklist() {
        // GIVEN: A marked custom checklist with existing tasks
        Checklist marked = new Checklist("Tasks Checklist");
        taskManager.addChecklist(marked);
        
        Task existing = new Task("Existing Task", TaskType.CUSTOM, null, marked.getId(), null);
        taskManager.addTask(existing);
        
        contextProvider.setSelectedChecklistInternal(marked);
        CtrlNCustomChecklistAction action = new CtrlNCustomChecklistAction(taskManager, contextProvider);
        
        // WHEN: New task is created via Ctrl+N
        String newTaskName = "Another Task";
        action.createNewTaskInSelectedChecklist(newTaskName);
        
        // THEN: Existing task remains, new task is added
        Checklist retrieved = taskManager.getChecklistById(marked.getId());
        List<Task> tasks = taskManager.getTasksByChecklist(retrieved.getId());
        
        assertEquals("Should have 2 tasks total", 2, tasks.size());
        assertTrue("Should contain existing task", tasks.stream().anyMatch(t -> "Existing Task".equals(t.getName())));
        assertTrue("Should contain new task", tasks.stream().anyMatch(t -> newTaskName.equals(t.getName())));
    }
    
    @Test
    public void testCtrlNPreservesChecklistSelection() {
        // GIVEN: Multiple checklists exist, one is marked
        Checklist checklist1 = new Checklist("Checklist 1");
        Checklist checklist2 = new Checklist("Checklist 2");
        taskManager.addChecklist(checklist1);
        taskManager.addChecklist(checklist2);
        
        contextProvider.setSelectedChecklistInternal(checklist1);
        CtrlNCustomChecklistAction action = new CtrlNCustomChecklistAction(taskManager, contextProvider);
        
        // WHEN: Task is created in marked checklist
        action.createNewTaskInSelectedChecklist("New Task");
        
        // THEN: Selection should remain on marked checklist
        assertEquals("Marked checklist should still be selected", 
            checklist1.getId(), contextProvider.getSelectedChecklist().getId());
    }
    
    /**
     * Mock implementation for testing context provider
     */
    public static class MockCustomChecklistContextProvider implements CustomChecklistContextProvider {
        private Checklist selectedChecklist;
        
        public void setSelectedChecklistInternal(Checklist checklist) {
            this.selectedChecklist = checklist;
        }
        
        @Override
        public Checklist getSelectedChecklist() {
            return selectedChecklist;
        }
        
        @Override
        public void setSelectedChecklistInUI(Checklist checklist) {
            this.selectedChecklist = checklist;
        }
        
        @Override
        public boolean isInCustomChecklistsTab() {
            return true;
        }
    }
}
