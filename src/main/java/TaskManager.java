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
 */import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    /**
     * Updates a task without showing error dialogs.
     * Returns true if successful, false if failed.
     */
    public boolean updateTaskQuiet(Task task) {
        if (repository instanceof XMLTaskRepository) {
            return ((XMLTaskRepository) repository).updateTaskQuiet(task);
        }
        // Fallback for other repository types
        try {
            repository.updateTask(task);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void removeTask(Task task) {
        repository.removeTask(task);
    }

    public List<Task> getTasks(TaskType type, String checklistName) {
        List<Task> allTasks = repository.getAllTasks();
        List<Task> filtered = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.getType() == type) {
            if (type != TaskType.CUSTOM || checklistName == null || Objects.equals(checklistName, task.getChecklistName())) {
                    filtered.add(task);
                }
            }
        }
        return filtered;
    }

    public java.util.Set<String> getCustomChecklistNames() {
        return repository.getChecklistNames();
    }

    public boolean hasUndoneTasks() {
        for (Task task : getAllTasks()) {
            if (!task.isDone()) {
                return true;
            }
        }
        return false;
    }

    public List<Reminder> getDueReminders(int minutesAhead, java.util.Set<String> openedChecklists) {
        return repository.getDueReminders(minutesAhead, openedChecklists);
    }

    public java.time.LocalDateTime getNextReminderTime(java.util.Set<String> openedChecklists) {
        return repository.getNextReminderTime(openedChecklists);
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

    public void addChecklistName(String name) {
        repository.addChecklistName(name);
    }

    public void removeChecklistName(String name) {
        repository.removeChecklistName(name);
    }

    /**
     * Manually creates a backup of all data.
     */
    public void createManualBackup() {
        if (repository instanceof XMLTaskRepository) {
            ((XMLTaskRepository) repository).createManualBackup();
        }
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

        // Allow a broader range of characters including international characters
        // Allow letters (including accented), numbers, spaces, and common symbols
        if (!trimmed.matches("[\\p{L}\\p{N}\\s\\-_.()]+")) {
            return null;
        }

        // Additional security: prevent path traversal attempts and XML injection
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\") ||
            trimmed.contains("<") || trimmed.contains("&") || 
            trimmed.contains("\"") || trimmed.contains("'")) {
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
        if (input == null || input.trim().isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(null,
                fieldName + " cannot be empty.",
                "Invalid Input", javax.swing.JOptionPane.ERROR_MESSAGE);
            return null;
        }
        String validated = validateAndSanitizeInput(input);
        if (validated == null) {
            javax.swing.JOptionPane.showMessageDialog(null,
                fieldName + " contains invalid characters or is too long.\n" +
                "Use only letters (including international characters), numbers, spaces, hyphens, underscores, dots, parentheses, and angle brackets.\n" +
                "Maximum length: 100 characters.",
                "Invalid Input", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        return validated;
    }
}
