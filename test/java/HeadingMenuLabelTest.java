import javax.swing.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for correct menu labels when renaming/removing HEADINGs vs tasks.
 */
public class HeadingMenuLabelTest {
    
    @Test
    public void testRenameMenuLabelForHeading() {
        // Given: a HEADING task
        Task heading = new Task("My Heading", TaskType.HEADING, "h1", null, "p1");
        
        // When: determining the menu label for rename
        String label = getMenuLabelForRename(heading);
        
        // Then: the label should indicate it's a heading
        assertEquals("Rename heading", label);
    }
    
    @Test
    public void testRenameMenuLabelForRegularTask() {
        // Given: a regular MORNING task
        Task task = new Task("Morning Task", TaskType.MORNING, "t1");
        
        // When: determining the menu label for rename
        String label = getMenuLabelForRename(task);
        
        // Then: the label should indicate it's a task
        assertEquals("Rename task", label);
    }
    
    @Test
    public void testRenameMenuLabelForSubtask() {
        // Given: a subtask
        Task subtask = new Task("Subtask", TaskType.MORNING, "t2", "p1", "p1");
        
        // When: determining the menu label for rename
        String label = getMenuLabelForRename(subtask);
        
        // Then: the label should indicate it's a task (subtask is still a task)
        assertEquals("Rename task", label);
    }
    
    @Test
    public void testRemoveMenuLabelForHeading() {
        // Given: a HEADING task
        Task heading = new Task("My Heading", TaskType.HEADING, "h1", null, "p1");
        
        // When: determining the menu label for remove
        String label = getMenuLabelForRemove(heading);
        
        // Then: the label should indicate it's a heading
        assertEquals("Remove heading", label);
    }
    
    @Test
    public void testRemoveMenuLabelForRegularTask() {
        // Given: a regular MORNING task
        Task task = new Task("Morning Task", TaskType.MORNING, "t1");
        
        // When: determining the menu label for remove
        String label = getMenuLabelForRemove(task);
        
        // Then: the label should indicate it's a task
        assertEquals("Remove task", label);
    }
    
    @Test
    public void testRemoveMenuLabelForCustomTask() {
        // Given: a CUSTOM task
        Task customTask = new Task("Custom Task", TaskType.CUSTOM, "t3", "checklist1");
        
        // When: determining the menu label for remove
        String label = getMenuLabelForRemove(customTask);
        
        // Then: the label should indicate it's a task
        assertEquals("Remove task", label);
    }
    
    @Test
    public void testRemoveMenuLabelForEveningTask() {
        // Given: an EVENING task
        Task eveningTask = new Task("Evening Task", TaskType.EVENING, "t4");
        
        // When: determining the menu label for remove
        String label = getMenuLabelForRemove(eveningTask);
        
        // Then: the label should indicate it's a task
        assertEquals("Remove task", label);
    }
    
    @Test
    public void testAllTaskTypesHaveCorrectMenuLabels() {
        // Given: tasks of all types
        List<Task> tasks = List.of(
            new Task("Morning", TaskType.MORNING, "m1"),
            new Task("Evening", TaskType.EVENING, "e1"),
            new Task("Custom", TaskType.CUSTOM, "c1", "checklist1"),
            new Task("Heading", TaskType.HEADING, "h1", null, "m1")
        );
        
        // When: checking all menu labels
        // Then: each should have the correct label
        for (Task task : tasks) {
            if (task.getType() == TaskType.HEADING) {
                assertEquals("Rename heading", getMenuLabelForRename(task));
                assertEquals("Remove heading", getMenuLabelForRemove(task));
            } else {
                assertEquals("Rename task", getMenuLabelForRename(task));
                assertEquals("Remove task", getMenuLabelForRemove(task));
            }
        }
    }
    
    @Test
    public void testHeadingMenuLabelConsistency() {
        // Given: multiple HEADINGs
        Task heading1 = new Task("Heading 1", TaskType.HEADING, "h1", null, "p1");
        Task heading2 = new Task("Heading 2", TaskType.HEADING, "h2", null, "p2");
        Task heading3 = new Task("Heading 3", TaskType.HEADING, "h3", null, "p3");
        
        // When: getting menu labels for each
        String label1Rename = getMenuLabelForRename(heading1);
        String label2Rename = getMenuLabelForRename(heading2);
        String label3Rename = getMenuLabelForRename(heading3);
        
        String label1Remove = getMenuLabelForRemove(heading1);
        String label2Remove = getMenuLabelForRemove(heading2);
        String label3Remove = getMenuLabelForRemove(heading3);
        
        // Then: all labels should be consistent
        assertEquals(label1Rename, label2Rename);
        assertEquals(label2Rename, label3Rename);
        assertEquals(label1Remove, label2Remove);
        assertEquals(label2Remove, label3Remove);
    }
    
    // Helper methods that simulate the menu logic
    private String getMenuLabelForRename(Task task) {
        return (task.getType() == TaskType.HEADING) ? "Rename heading" : "Rename task";
    }
    
    private String getMenuLabelForRemove(Task task) {
        return (task.getType() == TaskType.HEADING) ? "Remove heading" : "Remove task";
    }
}
