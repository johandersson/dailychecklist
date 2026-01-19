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
 */import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private final TaskRepository repository;

    public TaskManager(TaskRepository repository) {
        this.repository = repository;
        this.repository.initialize();
    }

    public List<Task> getDailyTasks() {
        return repository.getDailyTasks();
    }

    public List<Task> getAllTasks() {
        return repository.getAllTasks();
    }

    public void addTask(Task task) {
        repository.addTask(task);
    }

    public void updateTask(Task task) {
        repository.updateTask(task);
    }

    public void removeTask(Task task) {
        repository.removeTask(task);
    }

    public List<Task> getTasks(TaskType type, String checklistName) {
        List<Task> allTasks = repository.getAllTasks();
        List<Task> filtered = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.getType() == type) {
                if (type != TaskType.CUSTOM || checklistName == null || checklistName.equals(task.getChecklistName())) {
                    filtered.add(task);
                }
            }
        }
        return filtered;
    }

    public java.util.Set<String> getCustomChecklistNames() {
        List<Task> allTasks = repository.getAllTasks();
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Task task : allTasks) {
            if (task.getType() == TaskType.CUSTOM && task.getChecklistName() != null) {
                names.add(task.getChecklistName());
            }
        }
        return names;
    }

    public boolean hasUndoneTasks() {
        for (Task task : getAllTasks()) {
            if (!task.isDone()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets reminders that are due within the next specified minutes.
     * More efficient than checking all reminders.
     */
    public List<Reminder> getDueReminders(int minutesAhead, java.util.Set<String> openedChecklists) {
        if (repository instanceof XMLTaskRepository) {
            return ((XMLTaskRepository) repository).getDueReminders(minutesAhead, openedChecklists);
        }
        // Fallback to old method if not XMLTaskRepository
        List<Reminder> allReminders = repository.getReminders();
        List<Reminder> dueReminders = new ArrayList<>();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        for (Reminder r : allReminders) {
            String checklistName = r.getChecklistName();
            if (checklistName != null && openedChecklists != null && openedChecklists.contains(checklistName)) {
                continue;
            }

            java.time.LocalDateTime reminderTime = java.time.LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
            if (!reminderTime.isAfter(now) && reminderTime.isAfter(now.minusMinutes(5))) {
                dueReminders.add(r);
            }
        }
        return dueReminders;
    }

    /**
     * Gets the next upcoming reminder time efficiently.
     */
    public java.time.LocalDateTime getNextReminderTime(java.util.Set<String> openedChecklists) {
        if (repository instanceof XMLTaskRepository) {
            return ((XMLTaskRepository) repository).getNextReminderTime(openedChecklists);
        }
        // Fallback implementation
        List<Reminder> reminders = repository.getReminders();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime nextTime = null;

        for (Reminder r : reminders) {
            String checklistName = r.getChecklistName();
            if (checklistName != null && openedChecklists != null && openedChecklists.contains(checklistName)) {
                continue;
            }

            java.time.LocalDateTime reminderTime = java.time.LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
            if (reminderTime.isAfter(now) && (nextTime == null || reminderTime.isBefore(nextTime))) {
                nextTime = reminderTime;
            }
        }
        return nextTime;
    }

    public void setTasks(List<Task> tasks) {
        repository.setTasks(tasks);
    }

    public List<Reminder> getReminders() {
        return repository.getReminders();
    }

    public void addReminder(Reminder reminder) {
        repository.addReminder(reminder);
    }

    public void removeReminder(Reminder reminder) {
        repository.removeReminder(reminder);
    }

    /**
     * Validates and sanitizes user input for task/checklist names
     * @param input The raw user input
     * @return Sanitized input or null if invalid
     */
    public static String validateAndSanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        String trimmed = input.trim();

        // Check length limits
        if (trimmed.isEmpty() || trimmed.length() > 100) {
            return null;
        }

        // Remove potentially dangerous characters for XML and file systems
        // Allow alphanumeric, spaces, hyphens, underscores, and basic punctuation
        if (!trimmed.matches("[\\w\\s\\-_.()]+")) {
            return null;
        }

        // Additional security: prevent path traversal attempts
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\") ||
            trimmed.contains("<") || trimmed.contains(">") || trimmed.contains("&")) {
            return null;
        }

        return trimmed;
    }

    /**
     * Validates input and shows appropriate error message
     * @param input The raw user input
     * @param fieldName Name of the field for error messages
     * @return Validated and sanitized input, or null if invalid
     */
    public static String validateInputWithError(String input, String fieldName) {
        String validated = validateAndSanitizeInput(input);
        if (validated == null && input != null && !input.trim().isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(null,
                fieldName + " contains invalid characters or is too long.\n" +
                "Use only letters, numbers, spaces, hyphens, underscores, dots, and parentheses.\n" +
                "Maximum length: 100 characters.",
                "Invalid Input", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        return validated;
    }
}
