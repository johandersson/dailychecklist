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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalDateTime;
import java.util.stream.IntStream;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * A dialog for adding or editing reminders with full date/time selection and preset buttons.
 */
public class ReminderEditDialog extends JDialog {
    private final TaskManager taskManager;
    private final String checklistName;
    private final Reminder existingReminder;
    private final Runnable onSave;

    public ReminderEditDialog(TaskManager taskManager, String checklistName, Reminder existingReminder, Runnable onSave) {
        super();
        this.taskManager = taskManager;
        this.checklistName = checklistName;
        this.existingReminder = existingReminder;
        this.onSave = onSave;

        setTitle(existingReminder == null ? "Add Reminder for " + checklistName : "Edit Reminder for " + checklistName);
        setModal(true);
        setLayout(new BorderLayout());
        setResizable(false);

        initializeUI();
        pack();
        setLocationRelativeTo(null);
    }

    private void initializeUI() {
        LocalDateTime now = LocalDateTime.now();

        // Header
        String currentTimeString = String.format("%02d:%02d on %d/%d/%d",
            now.getHour(), now.getMinute(), now.getMonthValue(), now.getDayOfMonth(), now.getYear());
        String headerText;
        if (existingReminder != null) {
            String existingTimeString = String.format("%02d:%02d on %d/%d/%d",
                existingReminder.getHour(), existingReminder.getMinute(),
                existingReminder.getMonth(), existingReminder.getDay(), existingReminder.getYear());
            headerText = "<html>Edit reminder for: <b>" + checklistName + "</b><br><small>Current time: " + currentTimeString + "<br>Existing reminder: " + existingTimeString + "</small></html>";
        } else {
            headerText = "<html>Set a reminder for: <b>" + checklistName + "</b><br><small>Current time is pre-selected: " + currentTimeString + "</small></html>";
        }
        JLabel headerLabel = new JLabel(headerText, JLabel.CENTER);
        headerLabel.setFont(headerLabel.getFont().deriveFont(14.0f));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Main content panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Date section
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setFont(dateLabel.getFont().deriveFont(Font.BOLD));
        contentPanel.add(dateLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        contentPanel.add(new JLabel("Year:"), gbc);
        gbc.gridx = 1;
        JComboBox<Integer> yearBox = new JComboBox<>(IntStream.rangeClosed(now.getYear(), now.getYear() + 5).boxed().toArray(Integer[]::new));
        yearBox.setSelectedItem(existingReminder != null ? existingReminder.getYear() : now.getYear());
        contentPanel.add(yearBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        contentPanel.add(new JLabel("Month:"), gbc);
        gbc.gridx = 1;
        JComboBox<Integer> monthBox = new JComboBox<>(IntStream.rangeClosed(1, 12).boxed().toArray(Integer[]::new));
        monthBox.setSelectedItem(existingReminder != null ? existingReminder.getMonth() : now.getMonthValue());
        contentPanel.add(monthBox, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        contentPanel.add(new JLabel("Day:"), gbc);
        gbc.gridx = 1;
        JComboBox<Integer> dayBox = new JComboBox<>(IntStream.rangeClosed(1, 31).boxed().toArray(Integer[]::new));
        dayBox.setSelectedItem(existingReminder != null ? existingReminder.getDay() : now.getDayOfMonth());
        contentPanel.add(dayBox, gbc);

        // Time section
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JLabel timeLabel = new JLabel("Time:");
        timeLabel.setFont(timeLabel.getFont().deriveFont(Font.BOLD));
        contentPanel.add(timeLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 5;
        contentPanel.add(new JLabel("Hour (0-23):"), gbc);
        gbc.gridx = 1;
        JComboBox<Integer> hourBox = new JComboBox<>(IntStream.rangeClosed(0, 23).boxed().toArray(Integer[]::new));
        hourBox.setSelectedItem(existingReminder != null ? existingReminder.getHour() : now.getHour());
        contentPanel.add(hourBox, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        contentPanel.add(new JLabel("Minute:"), gbc);
        gbc.gridx = 1;
        JComboBox<Integer> minuteBox = new JComboBox<>(IntStream.rangeClosed(0, 59).boxed().toArray(Integer[]::new));
        minuteBox.setSelectedItem(existingReminder != null ? existingReminder.getMinute() : (now.getMinute() / 5) * 5);
        contentPanel.add(minuteBox, gbc);

        // Quick preset buttons
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        presetPanel.setBorder(BorderFactory.createTitledBorder("Quick Set"));

        JButton in15MinButton = new JButton("In 15 min");
        JButton in1HourButton = new JButton("In 1 hour");
        JButton tomorrowButton = new JButton("Tomorrow");
        JButton nextWeekButton = new JButton("Next week");

        in15MinButton.addActionListener(e -> setTimeFromNow(hourBox, minuteBox, yearBox, monthBox, dayBox, 15));
        in1HourButton.addActionListener(e -> setTimeFromNow(hourBox, minuteBox, yearBox, monthBox, dayBox, 60));
        tomorrowButton.addActionListener(e -> setTimeTomorrow(hourBox, minuteBox, yearBox, monthBox, dayBox, now.getHour(), now.getMinute()));
        nextWeekButton.addActionListener(e -> setTimeNextWeek(hourBox, minuteBox, yearBox, monthBox, dayBox, now.getHour(), now.getMinute()));

        presetPanel.add(in15MinButton);
        presetPanel.add(in1HourButton);
        presetPanel.add(tomorrowButton);
        presetPanel.add(nextWeekButton);

        contentPanel.add(presetPanel, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton(existingReminder == null ? "Add Reminder" : "Save Reminder");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            try {
                int year = (Integer) yearBox.getSelectedItem();
                int month = (Integer) monthBox.getSelectedItem();
                int day = (Integer) dayBox.getSelectedItem();
                int hour = (Integer) hourBox.getSelectedItem();
                int minute = (Integer) minuteBox.getSelectedItem();

                // Validate date
                java.time.LocalDateTime.of(year, month, day, hour, minute);

                if (existingReminder == null) {
                    // Adding new reminder - check for existing
                    java.util.List<Reminder> existingReminders = taskManager.getReminders().stream()
                        .filter(r -> r.getChecklistName().equals(checklistName))
                        .toList();

                    if (!existingReminders.isEmpty()) {
                        int choice = JOptionPane.showConfirmDialog(this,
                            "A reminder already exists for this checklist. Replace it?",
                            "Replace Reminder", JOptionPane.YES_NO_OPTION);
                        if (choice != JOptionPane.YES_OPTION) {
                            return; // Don't add the new reminder
                        }
                        // Remove existing reminders for this checklist
                        for (Reminder existing : existingReminders) {
                            taskManager.removeReminder(existing);
                        }
                    }

                    Reminder newReminder = new Reminder(checklistName, year, month, day, hour, minute);
                    taskManager.addReminder(newReminder);

                    // Show informative success message
                    String timeString = String.format("%02d:%02d", hour, minute);
                    String dateString = String.format("%d/%d/%d", month, day, year);
                    String message = String.format("Reminder set successfully!\n\nChecklist: %s\nDate: %s\nTime: %s\n\nYou'll be reminded at the specified time.",
                        checklistName, dateString, timeString);
                    JOptionPane.showMessageDialog(this, message, "Reminder Set", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // Editing existing reminder
                    taskManager.removeReminder(existingReminder);
                    Reminder newReminder = new Reminder(checklistName, year, month, day, hour, minute);
                    taskManager.addReminder(newReminder);

                    // Show informative success message
                    String timeString = String.format("%02d:%02d", hour, minute);
                    String dateString = String.format("%d/%d/%d", month, day, year);
                    String message = String.format("Reminder updated successfully!\n\nChecklist: %s\nDate: %s\nTime: %s\n\nYou'll be reminded at the specified time.",
                        checklistName, dateString, timeString);
                    JOptionPane.showMessageDialog(this, message, "Reminder Updated", JOptionPane.INFORMATION_MESSAGE);
                }

                dispose();
                if (onSave != null) {
                    onSave.run();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid date/time. Please check your input.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(headerLabel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setTimeFromNow(JComboBox<Integer> hourBox, JComboBox<Integer> minuteBox,
                               JComboBox<Integer> yearBox, JComboBox<Integer> monthBox, JComboBox<Integer> dayBox,
                               int minutesFromNow) {
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(minutesFromNow);
        yearBox.setSelectedItem(futureTime.getYear());
        monthBox.setSelectedItem(futureTime.getMonthValue());
        dayBox.setSelectedItem(futureTime.getDayOfMonth());
        hourBox.setSelectedItem(futureTime.getHour());
        minuteBox.setSelectedItem(futureTime.getMinute());
    }

    private void setTimeTomorrow(JComboBox<Integer> hourBox, JComboBox<Integer> minuteBox,
                                JComboBox<Integer> yearBox, JComboBox<Integer> monthBox, JComboBox<Integer> dayBox,
                                int hour, int minute) {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        yearBox.setSelectedItem(tomorrow.getYear());
        monthBox.setSelectedItem(tomorrow.getMonthValue());
        dayBox.setSelectedItem(tomorrow.getDayOfMonth());
        hourBox.setSelectedItem(hour);
        minuteBox.setSelectedItem(minute);
    }

    private void setTimeNextWeek(JComboBox<Integer> hourBox, JComboBox<Integer> minuteBox,
                                JComboBox<Integer> yearBox, JComboBox<Integer> monthBox, JComboBox<Integer> dayBox,
                                int hour, int minute) {
        LocalDateTime nextWeek = LocalDateTime.now().plusWeeks(1);
        yearBox.setSelectedItem(nextWeek.getYear());
        monthBox.setSelectedItem(nextWeek.getMonthValue());
        dayBox.setSelectedItem(nextWeek.getDayOfMonth());
        hourBox.setSelectedItem(hour);
        minuteBox.setSelectedItem(minute);
    }
}