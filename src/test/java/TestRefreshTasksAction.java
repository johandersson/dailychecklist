import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.event.ActionEvent;

public class TestRefreshTasksAction {
    @Test
    public void testActionPerformedRunsUpdateTasks() {
        final boolean[] called = {false};
        Runnable updateTasks = () -> called[0] = true;
        RefreshTasksAction action = new RefreshTasksAction(updateTasks);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "REFRESH_TASKS"));
        assertTrue(called[0], "updateTasks should be called when actionPerformed is invoked");
    }
}
