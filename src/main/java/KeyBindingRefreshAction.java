import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

@SuppressWarnings("serial")
public class KeyBindingRefreshAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    private transient Runnable updateTasks;

    public KeyBindingRefreshAction(Runnable updateTasks) {
        this.updateTasks = updateTasks;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateTasks.run();
    }
}