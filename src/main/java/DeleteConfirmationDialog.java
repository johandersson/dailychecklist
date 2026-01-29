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

import java.awt.Component;
import java.util.List;

public final class DeleteConfirmationDialog {
    private DeleteConfirmationDialog() {}

    public static boolean showConfirm(Component parent, TaskManager taskManager, List<Task> tasksToDelete) {
        List<Task> extraSubs = collectExtraSubtasks(taskManager, tasksToDelete);

        javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(parent), "Confirm Deletion", true);
        dialog.setLayout(new java.awt.BorderLayout());
        dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);

        javax.swing.JLabel label = buildSummaryLabel(extraSubs);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 4, 6));
        dialog.add(label, java.awt.BorderLayout.NORTH);

        javax.swing.JComponent listComponent = buildTaskListComponent(tasksToDelete, taskManager);
        dialog.add(listComponent, java.awt.BorderLayout.CENTER);

        final boolean[] confirmed = {false};
        javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.FlowLayout());
        javax.swing.JButton yesButton = new javax.swing.JButton("Yes, Delete");
        javax.swing.JButton noButton = new javax.swing.JButton("No, Cancel");
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        dialog.add(buttonPanel, java.awt.BorderLayout.SOUTH);

        yesButton.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });
        noButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return confirmed[0];
    }

    private static java.util.List<Task> collectExtraSubtasks(TaskManager taskManager, java.util.List<Task> tasksToDelete) {
        java.util.Set<String> selectedIds = new java.util.HashSet<>();
        for (Task t : tasksToDelete) selectedIds.add(t.getId());
        java.util.List<Task> extraSubs = new java.util.ArrayList<>();
        for (Task t : tasksToDelete) {
            java.util.List<Task> subs = taskManager.getSubtasks(t.getId());
            if (subs != null) {
                for (Task s : subs) {
                    if (!selectedIds.contains(s.getId())) extraSubs.add(s);
                }
            }
        }
        return extraSubs;
    }

    private static javax.swing.JLabel buildSummaryLabel(java.util.List<Task> extraSubs) {
        javax.swing.JLabel label;
        if (extraSubs.isEmpty()) {
            label = new javax.swing.JLabel("Are you sure you want to delete the following tasks?");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("The selected tasks have ").append(extraSubs.size()).append(" additional subtask(s) that will also be deleted:\n");
            int shown = 0;
            for (Task s : extraSubs) {
                if (shown++ >= 10) break;
                sb.append(" - ").append(s.getName()).append("\n");
            }
            if (extraSubs.size() > 10) sb.append("... and ").append(extraSubs.size() - 10).append(" more\n");
            sb.append("\nAre you sure you want to delete the following tasks?");
            label = new javax.swing.JLabel("<html>" + sb.toString().replace("\n", "<br>") + "</html>");
        }
        return label;
    }

    private static javax.swing.JComponent buildTaskListComponent(java.util.List<Task> tasksToDelete, TaskManager taskManager) {
        javax.swing.JList<Task> taskList = new javax.swing.JList<>(tasksToDelete.toArray(Task[]::new));
        taskList.setCellRenderer(new CheckboxListCellRenderer(taskManager));
        taskList.setEnabled(false);
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(taskList);
        scrollPane.setPreferredSize(new java.awt.Dimension(300, 150));
        return scrollPane;
    }
}
