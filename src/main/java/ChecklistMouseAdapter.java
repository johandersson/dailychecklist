/*
 * Daily Checklist
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
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