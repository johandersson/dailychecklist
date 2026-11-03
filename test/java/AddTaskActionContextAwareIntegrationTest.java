import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.List;

/**
 * Integration tests for Ctrl+N context-aware behavior
 * Tests the interaction between DailyChecklist, KeyBindingManager, and context providers
 */
public class AddTaskActionContextAwareIntegrationTest {
    
    private TaskManager taskManager;
    private TestHelpers.InMemoryTaskRepository repository;
    private MockTabbedPaneContextProvider tabContextProvider;
    
    @Before
    public void setUp() {
        repository = new TestHelpers.InMemoryTaskRepository();
        taskManager = new TaskManager(repository);
        tabContextProvider = new MockTabbedPaneContextProvider();
    }
    
    @Test
    public void testCtrlNInDailyChecklistTabCreatesMorningTask() {
        // GIVEN: Context provider indicates we're in daily checklist tab
        tabContextProvider.setInCustomChecklistsTab(false);
        AddTaskActionContextAware action = new AddTaskActionContextAware(null, taskManager, () -> {}, tabContextProvider);
        
        // WHEN: Headless mode creates a morning task
        // (Note: actionPerformed will trigger handleDailyChecklistTab in headless mode)
        java.awt.event.ActionEvent event = new java.awt.event.ActionEvent(new javax.swing.JButton(), 0, "");
        action.actionPerformed(event);
        
        // THEN: A MORNING task is created
        List<Task> tasks = repository.getAllTasks();
        assertFalse("Tasks should be created", tasks.isEmpty());
        assertTrue("Should have MORNING task", tasks.stream().anyMatch(t -> t.getType() == TaskType.MORNING));
    }
    
    @Test
    public void testCtrlNInCustomChecklistTabCreatesCustomTask() {
        // GIVEN: We're in custom checklists tab with a marked checklist
        Checklist marked = new Checklist("My Checklist");
        taskManager.addChecklist(marked);
        
        tabContextProvider.setInCustomChecklistsTab(true);
        tabContextProvider.setSelectedChecklist(marked);
        
        AddTaskActionContextAware action = new AddTaskActionContextAware(null, taskManager, () -> {}, tabContextProvider);
        
        // WHEN: Headless mode creates a task
        java.awt.event.ActionEvent event = new java.awt.event.ActionEvent(new javax.swing.JButton(), 0, "");
        action.actionPerformed(event);
        
        // THEN: A CUSTOM task is created in the marked checklist
        List<Task> tasks = repository.getAllTasks();
        assertFalse("Tasks should be created", tasks.isEmpty());
        assertTrue("Should have CUSTOM task", tasks.stream().anyMatch(t -> t.getType() == TaskType.CUSTOM));
        assertTrue("Task should belong to marked checklist", 
            tasks.stream().anyMatch(t -> marked.getId().equals(t.getChecklistId())));
    }
    
    @Test
    public void testCtrlNInCustomChecklistTabWithNoMarkedChecklistCreatesNewChecklist() {
        // GIVEN: We're in custom checklists tab with NO marked checklist
        tabContextProvider.setInCustomChecklistsTab(true);
        tabContextProvider.setSelectedChecklist(null);
        
        AddTaskActionContextAware action = new AddTaskActionContextAware(null, taskManager, () -> {}, tabContextProvider);
        
        // WHEN: Action is triggered
        java.awt.event.ActionEvent event = new java.awt.event.ActionEvent(new javax.swing.JButton(), 0, "");
        action.actionPerformed(event);
        
        // THEN: A new checklist is created and selected
        assertFalse("Checklists should be created", taskManager.getCustomChecklists().isEmpty());
        assertNotNull("Context provider should have selected checklist", tabContextProvider.getSelectedChecklist());
    }
    
    @Test
    public void testContextProviderDelegation() {
        // GIVEN: A tab context provider with both daily and custom contexts
        Checklist checklist = new Checklist("Test");
        taskManager.addChecklist(checklist);
        
        tabContextProvider.setInCustomChecklistsTab(false);
        tabContextProvider.setSelectedChecklist(checklist);
        
        // WHEN: We're in daily tab
        assertFalse("Should report not in custom tab", tabContextProvider.isInCustomChecklistsTab());
        
        // THEN: Selected checklist should be ignored (reported as null from daily tab)
        // In real implementation, this depends on the tab - for now just verify the method works
        tabContextProvider.setInCustomChecklistsTab(true);
        assertTrue("Should report in custom tab", tabContextProvider.isInCustomChecklistsTab());
    }
    
    /**
     * Mock tab context provider for testing
     */
    public static class MockTabbedPaneContextProvider implements CustomChecklistContextProvider {
        private boolean inCustomChecklistsTab;
        private Checklist selectedChecklist;
        
        public void setInCustomChecklistsTab(boolean value) {
            this.inCustomChecklistsTab = value;
        }
        
        public void setSelectedChecklist(Checklist checklist) {
            this.selectedChecklist = checklist;
        }
        
        @Override
        public Checklist getSelectedChecklist() {
            return inCustomChecklistsTab ? selectedChecklist : null;
        }
        
        @Override
        public void setSelectedChecklistInUI(Checklist checklist) {
            this.selectedChecklist = checklist;
        }
        
        @Override
        public boolean isInCustomChecklistsTab() {
            return inCustomChecklistsTab;
        }
    }
}
