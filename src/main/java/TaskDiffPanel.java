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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * Panel that displays differences between current and backup task lists.
 */
public class TaskDiffPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public TaskDiffPanel(List<Task> currentTasks, List<Task> backupTasks) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Calculate differences
        TaskListDiff diff = new TaskListDiff(currentTasks, backupTasks);

        // Summary
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(new JLabel("Summary of changes:"), gbc);

        gbc.gridy = 1;
        JTextArea summaryArea = new JTextArea(diff.getSummary());
        summaryArea.setEditable(false);
        summaryArea.setBackground(getBackground());
        summaryArea.setWrapStyleWord(true);
        summaryArea.setLineWrap(true);
        add(summaryArea, gbc);

        // Details
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        add(new JLabel("Detailed changes:"), gbc);

        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JTextArea detailsArea = new JTextArea(diff.getDetailedChanges());
        detailsArea.setEditable(false);
        detailsArea.setBackground(getBackground());
        detailsArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        add(new javax.swing.JScrollPane(detailsArea), gbc);
    }
}