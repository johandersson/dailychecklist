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

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

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
        setSize(400, 700); // Increased height for split
        setLocationRelativeTo(null);

        checklistPanel = new CustomChecklistPanel(taskManager, new TaskUpdater(), checklistName);
        JScrollPane checklistScroll = new JScrollPane(checklistPanel);

        CustomAddTaskPanel addPanel = new CustomAddTaskPanel(taskManager, () -> {
            checklistPanel.updateTasks();
            updateTasks.run();
        }, checklistName);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, checklistScroll, addPanel);
        splitPane.setDividerLocation(400); // Adjust as needed
        splitPane.setResizeWeight(0.7); // More space for checklist

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);

        checklistPanel.updateTasks();
    }
}