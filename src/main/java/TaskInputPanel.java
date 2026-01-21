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
 */import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class TaskInputPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JTextField taskField;
    private JComboBox<String> timeComboBox;

    public TaskInputPanel(String lastTask, String lastAmountOfMinutesForTask) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JLabel taskLabelPopup = new JLabel("Enter your focus task:");
        taskField = new JTextField(lastTask, 20);

        JLabel timeLabelPopup = new JLabel("Select timer countdown:");
        String[] timeOptions = {"20 minutes", "15 minutes", "10 minutes", "5 minutes"};
        timeComboBox = new JComboBox<>(timeOptions);
        if (!lastAmountOfMinutesForTask.isEmpty()) {
            timeComboBox.setSelectedItem(lastAmountOfMinutesForTask);
        }

        JPanel inputPanel = new JPanel();
        inputPanel.setBackground(Color.WHITE);
        inputPanel.add(taskLabelPopup);
        inputPanel.add(taskField);
        inputPanel.add(timeLabelPopup);
        inputPanel.add(timeComboBox);

        add(inputPanel);
    }

    public String getTask() {
        return taskField.getText();
    }

    public String getSelectedTime() {
        return (String) timeComboBox.getSelectedItem();
    }

    public int getTimeInSeconds() {
        switch (getSelectedTime()) {
            case "15 minutes":
                return 15 * 60;
            case "10 minutes":
                return 10 * 60;
            case "5 minutes":
                return 5 * 60;
            case "20 minutes":
            default:
                return 20 * 60;
        }
    }
}

