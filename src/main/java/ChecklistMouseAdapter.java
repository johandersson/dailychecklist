import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import javax.swing.JList;
import javax.swing.SwingUtilities;

public class ChecklistMouseAdapter extends MouseAdapter {
    private JList<Task> list;
    private TaskManager taskManager;

    public ChecklistMouseAdapter(JList<Task> list, TaskManager taskManager) {
        this.list = list;
        this.taskManager = taskManager;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        if (index >= 0) {
            if (SwingUtilities.isRightMouseButton(e)) {
                // For testing, perhaps do nothing or mock
                // showContextMenu(e, list, index);
            } else {
                Task task = list.getModel().getElementAt(index);
                boolean originalDoneState = task.isDone();
                
                // Toggle the done state
                task.setDone(!task.isDone());
                if (task.isDone()) {
                    task.setDoneDate(new Date(System.currentTimeMillis()));
                } else {
                    task.setDoneDate(null);
                }
                
                // Try to save the change
                if (!taskManager.updateTaskQuiet(task)) {
                    // Revert the task state if saving failed
                    task.setDone(originalDoneState);
                    if (originalDoneState) {
                        task.setDoneDate(new Date(System.currentTimeMillis()));
                    } else {
                        task.setDoneDate(null);
                    }
                    
                    // Show error dialog
                    javax.swing.JOptionPane.showMessageDialog(
                        list,
                        "Failed to save task changes. The task state has been reverted.\n\nPlease check your file permissions and disk space.",
                        "Save Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                    );
                }
                
                list.repaint(list.getCellBounds(index, index));
            }
        }
    }
}