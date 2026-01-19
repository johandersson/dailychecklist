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
import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ChecklistWindow extends JFrame {
    private CustomChecklistPanel checklistPanel;
    private TaskManager taskManager;
    private String checklistName;
    private Runnable updateTasks;

    public ChecklistWindow(TaskManager taskManager, Runnable updateTasks, String checklistName) {
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        this.checklistName = checklistName;
        initialize();
    }

    private void initialize() {
        setTitle("Checklist: " + checklistName);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 600);
        setLocationRelativeTo(null);

        checklistPanel = new CustomChecklistPanel(taskManager, new TaskUpdater(), checklistName);

        JButton addTaskButton = new JButton("Add Task");
        addTaskButton.addActionListener(e -> openAddTaskWindow());

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(addTaskButton);

        setLayout(new BorderLayout());
        add(checklistPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        checklistPanel.updateTasks();
    }

    private void openAddTaskWindow() {
        AddTaskPanel addPanel = new AddTaskPanel(taskManager, () -> {
            checklistPanel.updateTasks();
            updateTasks.run();
        }, checklistName);
        JFrame addFrame = new JFrame("Add Task to " + checklistName);
        addFrame.add(addPanel);
        addFrame.pack();
        addFrame.setLocationRelativeTo(this);
        addFrame.setVisible(true);
    }
}