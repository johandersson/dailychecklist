import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class KeyBindingRefreshAction extends AbstractAction {
    private Runnable updateTasks;

    public KeyBindingRefreshAction(Runnable updateTasks) {
        this.updateTasks = updateTasks;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateTasks.run();
    }
}