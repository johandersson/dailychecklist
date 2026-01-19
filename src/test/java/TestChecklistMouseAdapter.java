import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListModel;
import javax.swing.JList;

public class TestChecklistMouseAdapter {
    @Test
    void testMouseClickedLeft() {
        DefaultListModel<Task> model = new DefaultListModel<>();
        Task task = new Task("Test", TaskType.MORNING, null);
        model.addElement(task);
        JList<Task> list = new JList<>(model);
        TaskManager taskManager = new TaskManager(new XMLTaskRepository());
        ChecklistMouseAdapter adapter = new ChecklistMouseAdapter(list, taskManager);

        // Simulate left click on index 0
        MouseEvent event = new MouseEvent(list, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 10, 10, 1, false);
        adapter.mouseClicked(event);

        // Check that task is toggled to done
        assertTrue(task.isDone());
        assertNotNull(task.getDoneDate());
    }

    @Test
    void testMouseClickedRight() {
        DefaultListModel<Task> model = new DefaultListModel<>();
        Task task = new Task("Test", TaskType.MORNING, null);
        model.addElement(task);
        JList<Task> list = new JList<>(model);
        TaskManager taskManager = new TaskManager(new XMLTaskRepository());
        ChecklistMouseAdapter adapter = new ChecklistMouseAdapter(list, taskManager);

        // Simulate right click
        MouseEvent event = new MouseEvent(list, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), MouseEvent.BUTTON3_DOWN_MASK, 10, 10, 1, false, MouseEvent.BUTTON3);
        adapter.mouseClicked(event);

        // Should not change task
        assertFalse(task.isDone());
    }
}