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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class CustomAddTaskPanel extends BaseAddTaskPanel {
    private final String checklistName;

    public CustomAddTaskPanel(TaskManager taskManager, Runnable updateTasks, String checklistName) {
        super(taskManager, updateTasks);
        this.checklistName = checklistName;
    }

    @Override
    protected void initializeSpecific() {
        // No additional initialization needed for custom
    }

    @Override
    protected ActionListener createAddActionListener() {
        return e -> {
            String[] tasks = taskField.getText().split("\\n");
            for (String taskName : tasks) {
                if (!taskName.trim().isEmpty()) {
                    Task newTask = new Task(taskName.trim(), TaskType.CUSTOM, null, checklistName);
                    taskManager.addTask(newTask);
                } else {
                    JOptionPane.showMessageDialog(this, "Task name cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            String message = String.format("Added %d custom tasks to %s successfully.", tasks.length, checklistName);
            JOptionPane.showMessageDialog(this, message, "Tasks Added", JOptionPane.INFORMATION_MESSAGE);
            taskField.setText("");
            updateTasks.run();
        };
    }

    @Override
    protected int getButtonRow() {
        return 1;
    }
}