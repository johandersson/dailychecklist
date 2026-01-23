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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

@SuppressWarnings("serial")
public abstract class BaseAddTaskPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    protected transient final TaskManager taskManager;
    protected transient final Runnable updateTasks;
    protected JTextArea taskField;
    protected JButton addButton;

    @SuppressWarnings("this-escape")
    public BaseAddTaskPanel(TaskManager taskManager, Runnable updateTasks) {
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        initializeCommon();
        initializeSpecific();
    }

    private void initializeCommon() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        taskField = new JTextArea(25, 40);
        taskField.setLineWrap(true);
        taskField.setWrapStyleWord(true);
        JScrollPane taskScrollPane = new JScrollPane(taskField);
        taskScrollPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        taskScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        taskScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        taskScrollPane.setPreferredSize(new Dimension(300, 200));
        taskScrollPane.setMinimumSize(new Dimension(300, 200));
        taskField.setFont(new Font("Yu Gothic UI", Font.PLAIN, 16));
        taskField.setForeground(Color.BLACK);
        taskField.setBackground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(taskScrollPane, gbc);

        addButton = new JButton("Add tasks");
        addButton.addActionListener(createAddActionListener());
        gbc.gridx = 0;
        gbc.gridy = getButtonRow();
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(addButton, gbc);
    }

    protected abstract void initializeSpecific();

    protected abstract ActionListener createAddActionListener();

    protected abstract int getButtonRow();
}