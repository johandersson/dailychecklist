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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class CustomChecklistsOverviewPanel extends JPanel {
    private JList<String> checklistList;
    private DefaultListModel<String> listModel;
    private TaskManager taskManager;
    private Runnable updateTasks;

    public CustomChecklistsOverviewPanel(TaskManager taskManager, Runnable updateTasks) {
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        initialize();
    }

    private void initialize() {
        listModel = new DefaultListModel<>();
        checklistList = new JList<>(listModel);
        checklistList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = checklistList.getSelectedValue();
                    if (selected != null) {
                        openChecklistWindow(selected);
                    }
                }
            }
        });

        JButton addButton = new JButton("+");
        addButton.addActionListener(e -> addNewChecklist());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(addButton, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(checklistList), BorderLayout.CENTER);
    }

    private void addNewChecklist() {
        String name = JOptionPane.showInputDialog(this, "Enter checklist name:");
        if (name != null && !name.trim().isEmpty()) {
            String trimmed = name.trim();
            Set<String> existing = taskManager.getCustomChecklistNames();
            if (existing.contains(trimmed)) {
                JOptionPane.showMessageDialog(this, "Checklist name already exists.");
                return;
            }
            openChecklistWindow(trimmed);
        }
    }

    private void openChecklistWindow(String checklistName) {
        ChecklistWindow window = new ChecklistWindow(taskManager, updateTasks, checklistName);
        window.setVisible(true);
    }

    public void updateTasks() {
        listModel.clear();
        Set<String> names = taskManager.getCustomChecklistNames();
        for (String name : names) {
            listModel.addElement(name);
        }
    }
}