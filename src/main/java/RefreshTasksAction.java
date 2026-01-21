import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

@SuppressWarnings("serial")
public class RefreshTasksAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    private transient Runnable updateTasks;

    public RefreshTasksAction(Runnable updateTasks) {
        super("REFRESH_TASKS");
        this.updateTasks = updateTasks;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateTasks.run();
    }
}
