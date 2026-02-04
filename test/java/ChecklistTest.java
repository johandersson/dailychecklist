import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Test class for Checklist entity
 */
public class ChecklistTest {
    
    private Checklist checklist;
    
    @Before
    public void setUp() {
        checklist = new Checklist("Test Checklist", "test-id");
    }
    
    @Test
    public void testChecklistCreation() {
        assertNotNull("Checklist should not be null", checklist);
        assertEquals("Checklist name should match", "Test Checklist", checklist.getName());
        assertEquals("Checklist ID should match", "test-id", checklist.getId());
    }
    
    @Test
    public void testChecklistNameModification() {
        String newName = "Modified Checklist";
        checklist.setName(newName);
        assertEquals("Checklist name should be updated", newName, checklist.getName());
    }
    
    @Test
    public void testChecklistIdImmutability() {
        String originalId = checklist.getId();
        assertEquals("Checklist ID should remain unchanged", originalId, checklist.getId());
    }
    
    @Test
    public void testTaskAssociation() {
        List<Task> tasks = new ArrayList<>();
        Task task1 = new Task("Task 1", TaskType.CUSTOM, null, checklist.getId(), null);
        Task task2 = new Task("Task 2", TaskType.CUSTOM, null, checklist.getId(), null);
        
        tasks.add(task1);
        tasks.add(task2);
        
        for (Task task : tasks) {
            assertEquals("Task should reference correct checklist", checklist.getId(), task.getChecklistId());
        }
    }
    
    @Test
    public void testMultipleChecklists() {
        Checklist checklist1 = new Checklist("Checklist 1", "id-1");
        Checklist checklist2 = new Checklist("Checklist 2", "id-2");
        Checklist checklist3 = new Checklist("Checklist 3", "id-3");
        
        assertNotEquals("Checklist IDs should be unique", checklist1.getId(), checklist2.getId());
        assertNotEquals("Checklist IDs should be unique", checklist1.getId(), checklist3.getId());
        assertNotEquals("Checklist IDs should be unique", checklist2.getId(), checklist3.getId());
    }
    
    @Test
    public void testEmptyChecklistName() {
        Checklist emptyChecklist = new Checklist("", "empty-id");
        assertEquals("Empty name should be allowed", "", emptyChecklist.getName());
    }
    
    @Test
    public void testChecklistEquality() {
        Checklist checklist1 = new Checklist("Same Name", "id-1");
        Checklist checklist2 = new Checklist("Same Name", "id-1");
        
        assertEquals("Checklists with same ID should be equal", checklist1.getId(), checklist2.getId());
    }
}
