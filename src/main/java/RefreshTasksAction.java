import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class RefreshTasksAction extends AbstractAction {
    private Runnable updateTasks;

    public RefreshTasksAction(Runnable updateTasks) {
        super("REFRESH_TASKS");
        this.updateTasks = updateTasks;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateTasks.run();
    }
}
