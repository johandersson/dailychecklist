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
                task.setDone(!task.isDone());
                if (task.isDone()) {
                    task.setDoneDate(new Date(System.currentTimeMillis()));
                } else {
                    task.setDoneDate(null);
                }
                taskManager.updateTask(task);
                list.repaint(list.getCellBounds(index, index));
            }
        }
    }
}