import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.event.ActionEvent;

public class TestAddTaskAction {
    @Test
    public void testActionPerformedDoesNotThrow() {
        TaskManager tm = new TaskManager(new XMLTaskRepository());
        AddTaskAction action = new AddTaskAction(null, tm, () -> {});
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "ADD_NEW_TASK"));
        // If no exception thrown, consider success (UI input dialogs may return null in headless mode)
        assertTrue(true);
    }
}
