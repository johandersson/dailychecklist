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
import java.awt.Insets;
import java.time.LocalDateTime;
import java.util.Objects;
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
@SuppressWarnings({"serial","this-escape"})
public class ReminderEditDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private transient final TaskManager taskManager;
    private final String checklistName;
    private final Reminder existingReminder;
    private transient final Runnable onSave;
    private final String taskIdParam; // optional; if set, operate on task-level reminders

    

    // UI Components
    private JComboBox<Integer> yearBox, monthBox, dayBox, hourBox, minuteBox;
    private JButton saveButton;

    public ReminderEditDialog(TaskManager taskManager, String checklistName, Reminder existingReminder, Runnable onSave) {
        this(taskManager, checklistName, existingReminder, onSave, null);
    }

    public ReminderEditDialog(TaskManager taskManager, String checklistName, Reminder existingReminder, Runnable onSave, String taskIdParam) {
        super();
        this.taskManager = taskManager;
        this.checklistName = checklistName;
        this.existingReminder = existingReminder;
        this.onSave = onSave;
        this.taskIdParam = taskIdParam;

        initializeDialog();
        initializeUI();
        pack();
        setLocationRelativeTo(null);
    }

    private void initializeDialog() {
        setTitle(existingReminder == null ? "Add Reminder for " + checklistName : "Edit Reminder for " + checklistName);
        setModal(true);
        setLayout(new BorderLayout());
        setResizable(false);
    }

    private void initializeUI() {
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        LocalDateTime now = LocalDateTime.now();
        String currentTimeString = String.format("%02d:%02d on %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        String headerText;
        if (existingReminder != null) {
            String existingTimeString = String.format("%02d:%02d on %04d-%02d-%02d",
                existingReminder.getHour(), existingReminder.getMinute(),
                existingReminder.getYear(), existingReminder.getMonth(), existingReminder.getDay());
            headerText = String.format("<html>Edit reminder for: <b>%s</b><br><small>Current time: %s<br>Existing reminder: %s</small></html>",
                checklistName, currentTimeString, existingTimeString);
        } else {
            headerText = String.format("<html>Set a reminder for: <b>%s</b><br><small>Current time is pre-selected: %s</small></html>",
                checklistName, currentTimeString);
        }

        JLabel headerLabel = new JLabel(headerText, JLabel.CENTER);
        headerLabel.setFont(FontManager.getTaskListFont());
        headerLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(headerLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Date section
        addDateSection(panel, gbc);

        // Time section
        addTimeSection(panel, gbc);

        // Preset buttons
        addPresetSection(panel, gbc);

        return panel;
    }

    private void addDateSection(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setFont(dateLabel.getFont().deriveFont(Font.BOLD));
        panel.add(dateLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;

        // Year
        panel.add(new JLabel("Year:"), gbc);
        gbc.gridx = 1;
        yearBox = createYearComboBox();
        panel.add(yearBox, gbc);

        // Month
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Month:"), gbc);
        gbc.gridx = 1;
        monthBox = createMonthComboBox();
        panel.add(monthBox, gbc);

        // Day
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Day:"), gbc);
        gbc.gridx = 1;
        dayBox = createDayComboBox();
        panel.add(dayBox, gbc);
    }

    private void addTimeSection(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JLabel timeLabel = new JLabel("Time:");
        timeLabel.setFont(timeLabel.getFont().deriveFont(Font.BOLD));
        panel.add(timeLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 5;

        // Hour
        panel.add(new JLabel("Hour (0-23):"), gbc);
        gbc.gridx = 1;
        hourBox = createHourComboBox();
        panel.add(hourBox, gbc);

        // Minute
        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(new JLabel("Minute:"), gbc);
        gbc.gridx = 1;
        minuteBox = createMinuteComboBox();
        panel.add(minuteBox, gbc);
    }

    private void addPresetSection(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        presetPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JButton in15MinButton = createPresetButton("In 15 min", () -> setTimeFromNow(15), true);
        JButton in1HourButton = createPresetButton("In 1 hour", () -> setTimeFromNow(60), true);
        JButton tomorrowButton = createPresetButton("Tomorrow", this::setTimeTomorrow, true);
        JButton nextWeekButton = createPresetButton("Next week", this::setTimeNextWeek, true);

        presetPanel.add(in15MinButton);
        presetPanel.add(in1HourButton);
        presetPanel.add(tomorrowButton);
        presetPanel.add(nextWeekButton);

        panel.add(presetPanel, gbc);
    }

    private JButton createPresetButton(String text, Runnable action, boolean autoSave) {
        JButton button = new JButton(text);
        button.addActionListener(e -> {
            action.run();
            if (autoSave) {
                // Auto-save when using quick set
                saveReminder();
            }
        });
        return button;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        saveButton = new JButton(existingReminder == null ? "Add Reminder" : "Change Reminder");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> saveReminder());
        cancelButton.addActionListener(e -> dispose());

        panel.add(saveButton);
        panel.add(cancelButton);
        return panel;
    }

    private JComboBox<Integer> createYearComboBox() {
        LocalDateTime now = LocalDateTime.now();
        JComboBox<Integer> box = new JComboBox<>(IntStream.rangeClosed(now.getYear(), now.getYear() + 5).boxed().toArray(Integer[]::new));
        box.setSelectedItem(existingReminder != null ? existingReminder.getYear() : now.getYear());
        return box;
    }

    private JComboBox<Integer> createMonthComboBox() {
        LocalDateTime now = LocalDateTime.now();
        JComboBox<Integer> box = new JComboBox<>(IntStream.rangeClosed(1, 12).boxed().toArray(Integer[]::new));
        box.setSelectedItem(existingReminder != null ? existingReminder.getMonth() : now.getMonthValue());
        return box;
    }

    private JComboBox<Integer> createDayComboBox() {
        LocalDateTime now = LocalDateTime.now();
        JComboBox<Integer> box = new JComboBox<>(IntStream.rangeClosed(1, 31).boxed().toArray(Integer[]::new));
        box.setSelectedItem(existingReminder != null ? existingReminder.getDay() : now.getDayOfMonth());
        return box;
    }

    private JComboBox<Integer> createHourComboBox() {
        LocalDateTime now = LocalDateTime.now();
        JComboBox<Integer> box = new JComboBox<>(IntStream.rangeClosed(0, 23).boxed().toArray(Integer[]::new));
        box.setSelectedItem(existingReminder != null ? existingReminder.getHour() : now.getHour());
        return box;
    }

    private JComboBox<Integer> createMinuteComboBox() {
        LocalDateTime now = LocalDateTime.now();
        JComboBox<Integer> box = new JComboBox<>(IntStream.rangeClosed(0, 59).boxed().toArray(Integer[]::new));
        box.setSelectedItem(existingReminder != null ? existingReminder.getMinute() : now.getMinute());
        return box;
    }

    private void setTimeFromNow(int minutesFromNow) {
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(minutesFromNow);
        yearBox.setSelectedItem(futureTime.getYear());
        monthBox.setSelectedItem(futureTime.getMonthValue());
        dayBox.setSelectedItem(futureTime.getDayOfMonth());
        hourBox.setSelectedItem(futureTime.getHour());
        minuteBox.setSelectedItem(futureTime.getMinute());
    }

    private void setTimeTomorrow() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);
        yearBox.setSelectedItem(tomorrow.getYear());
        monthBox.setSelectedItem(tomorrow.getMonthValue());
        dayBox.setSelectedItem(tomorrow.getDayOfMonth());
        hourBox.setSelectedItem(now.getHour());
        minuteBox.setSelectedItem(now.getMinute());
    }

    private void setTimeNextWeek() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextWeek = now.plusWeeks(1);
        yearBox.setSelectedItem(nextWeek.getYear());
        monthBox.setSelectedItem(nextWeek.getMonthValue());
        dayBox.setSelectedItem(nextWeek.getDayOfMonth());
        hourBox.setSelectedItem(now.getHour());
        minuteBox.setSelectedItem(now.getMinute());
    }

    private void saveReminder() {
        try {
            int year = (Integer) yearBox.getSelectedItem();
            int month = (Integer) monthBox.getSelectedItem();
            int day = (Integer) dayBox.getSelectedItem();
            int hour = (Integer) hourBox.getSelectedItem();
            int minute = (Integer) minuteBox.getSelectedItem();

            // Validate date
            java.time.LocalDateTime.of(year, month, day, hour, minute);

            if (existingReminder == null) {
                handleNewReminder(year, month, day, hour, minute);
            } else {
                handleEditReminder(year, month, day, hour, minute);
            }
            // Close the dialog first so windowing focus events settle, then run onSave
            // Close the dialog and run the onSave callback immediately afterwards.
            try {
                dispose();
            } catch (Exception ignore) {}
            if (onSave != null) {
                try {
                    onSave.run();
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(ReminderEditDialog.class.getName()).log(java.util.logging.Level.SEVERE, "onSave callback failed", ex);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date/time. Please check your input.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleNewReminder(int year, int month, int day, int hour, int minute) {
        // Determine which existing reminders to consider based on taskIdParam
        java.util.List<Reminder> existingReminders = taskManager.getReminders().stream()
            .filter(r -> r.getChecklistName().equals(checklistName))
            .filter(r -> taskIdParam == null ? r.getTaskId() == null : Objects.equals(r.getTaskId(), taskIdParam))
            .toList();

        if (!existingReminders.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                taskIdParam == null ? "A reminder already exists for this checklist. Replace it?" : "A reminder already exists for this task. Replace it?",
                "Replace Reminder", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return; // Don't add the new reminder
            }
            // Remove matched existing reminders
            for (Reminder existing : existingReminders) {
                taskManager.removeReminder(existing);
            }
        }

        Reminder newReminder = new Reminder(checklistName, year, month, day, hour, minute, taskIdParam);
        taskManager.addReminder(newReminder);
        // Reminder added; panels will show the update directly
    }

    private void handleEditReminder(int year, int month, int day, int hour, int minute) {
        taskManager.removeReminder(existingReminder);
        Reminder newReminder = new Reminder(checklistName, year, month, day, hour, minute, taskIdParam != null ? taskIdParam : existingReminder.getTaskId());
        taskManager.addReminder(newReminder);
        // Reminder changed; panels will show the update directly
    }
    
}