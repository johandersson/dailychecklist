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
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class XMLTaskRepository implements TaskRepository {
    private static String FILE_NAME = System.getProperty("user.home") + File.separator + "dailychecklist-tasks.xml";
    private static String REMINDER_FILE_NAME = System.getProperty("user.home") + File.separator + "dailychecklist-reminders.properties";
    private static String CHECKLIST_NAMES_FILE_NAME = System.getProperty("user.home") + File.separator + "dailychecklist-checklist-names.properties";

    // Component managers
    private TaskXmlHandler taskXmlHandler;
    private ReminderManager reminderManager;
    private ChecklistNameManager checklistNameManager;

    // Backup system
    private BackupManager backupManager;

    // Parent component for error dialogs
    private Component parentComponent;

    /**
     * Creates a new XMLTaskRepository with no parent component.
     * Error dialogs will not be shown.
     */
    public XMLTaskRepository() {
        this(null);
    }

    /**
     * Creates a new XMLTaskRepository with a parent component for error dialogs.
     *
     * @param parentComponent Parent component for error dialogs, or null to disable dialogs
     */
    public XMLTaskRepository(Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    @Override
    public void initialize() {
        // Initialize component managers
        taskXmlHandler = new TaskXmlHandler(FILE_NAME);
        reminderManager = new ReminderManager(REMINDER_FILE_NAME, FILE_NAME);
        checklistNameManager = new ChecklistNameManager(CHECKLIST_NAMES_FILE_NAME);

        // Initialize backup system (but don't start threads yet)
        String[] dataFiles = {FILE_NAME, REMINDER_FILE_NAME, CHECKLIST_NAMES_FILE_NAME, ApplicationConfiguration.SETTINGS_FILE_PATH};
        backupManager = new BackupManager(ApplicationConfiguration.BACKUP_DIRECTORY, ApplicationConfiguration.MAX_BACKUP_FILES, ApplicationConfiguration.BACKUP_INTERVAL_MILLIS, dataFiles, parentComponent);
        backupManager.initialize();
    }

    /**
     * Ensures the data file exists, creating it if necessary.
     * This is called lazily when first accessing data.
     */
    private void ensureDataFileExists() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            try {
                // Create an empty tasks document
                taskXmlHandler.setAllTasks(new ArrayList<>());
            } catch (Exception e) {
                // Show user-friendly error dialog and re-throw as runtime exception
                if (parentComponent != null) {
                    ApplicationErrorHandler.showDataSaveError(parentComponent, "data file", e);
                }
                throw new RuntimeException("Failed to create data file: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public List<Task> getDailyTasks() {
        ensureDataFileExists();

        List<Task> tasks = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(System.currentTimeMillis()));

        try {
            List<Task> allTasks = taskXmlHandler.parseAllTasks();

            // Memory safety check
            if (MemorySafetyManager.checkTaskLimit(allTasks.size())) {
                allTasks = allTasks.subList(0, Math.min(MemorySafetyManager.MAX_TASKS, allTasks.size()));
            }

            for (Task task : allTasks) {
                try {
                    taskXmlHandler.checkAndResetPastDoneDate(task, today);
                } catch (ParseException e) {
                    // Log the error but continue processing other tasks
                    System.err.println("Failed to parse date for task " + task.getId() + ": " + e.getMessage());
                }
                tasks.add(task);
            }
        } catch (Exception e) {
            if (parentComponent != null) {
                ApplicationErrorHandler.showDataLoadError(parentComponent, "daily tasks", e);
            }
        }

        return tasks;
    }

    @Override
    public List<Task> getAllTasks() {
        ensureDataFileExists();

        List<Task> tasks = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(System.currentTimeMillis()));

        try {
            tasks = taskXmlHandler.parseAllTasks();

            // Memory safety check
            if (MemorySafetyManager.checkTaskLimit(tasks.size())) {
                tasks = tasks.subList(0, Math.min(MemorySafetyManager.MAX_TASKS, tasks.size()));
            }

            // Check and reset past done dates
            for (Task task : tasks) {
                try {
                    taskXmlHandler.checkAndResetPastDoneDate(task, today);
                } catch (ParseException e) {
                    // Log the error but continue processing other tasks
                    System.err.println("Failed to parse date for task " + task.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            if (parentComponent != null) {
                ApplicationErrorHandler.showDataLoadError(parentComponent, "all tasks", e);
            }
        }

        return new ArrayList<>(tasks);
    }

    @Override
    public void addTask(Task task) {
        try {
            taskXmlHandler.addTask(task);
            backupManager.createBackup("save");
        } catch (Exception e) {
            if (parentComponent != null) {
                ApplicationErrorHandler.showDataSaveError(parentComponent, "task", e);
            }
        }
    }

    @Override
    public void updateTask(Task task) {
        try {
            taskXmlHandler.updateTask(task);
            backupManager.createBackup("save");
        } catch (Exception e) {
            if (parentComponent != null) {
                ApplicationErrorHandler.showDataSaveError(parentComponent, "task", e);
            }
        }
    }

    @Override
    public void removeTask(Task task) {
        try {
            taskXmlHandler.removeTask(task);
            backupManager.createBackup("save");
        } catch (Exception e) {
            if (parentComponent != null) {
                ApplicationErrorHandler.showDataSaveError(parentComponent, "task", e);
            }
        }
    }

    @Override
    public boolean hasUndoneTasks() {
        try {
            List<Task> tasks = taskXmlHandler.parseAllTasks();
            for (Task task : tasks) {
                if (!task.isDone()) {
                    return true;
                }
            }
        } catch (Exception e) {
            if (parentComponent != null) {
                ApplicationErrorHandler.showDataLoadError(parentComponent, "task status", e);
            }
            return false; // Default to no undone tasks on error
        }
        return false;
    }

    @Override
    public void setTasks(List<Task> tasks) {
        // Create backup before saving
        backupManager.createBackup("save");

        try {
            taskXmlHandler.setAllTasks(tasks);
        } catch (Exception e) {
            if (parentComponent != null) {
                ApplicationErrorHandler.showDataSaveError(parentComponent, "tasks", e);
            }
        }
    }

    @Override
    public List<Reminder> getReminders() {
        return reminderManager.getReminders();
    }

    @Override
    public void addReminder(Reminder reminder) {
        // Create backup before modifying reminders
        backupManager.createBackup("add-reminder");
        reminderManager.addReminder(reminder);
    }

    @Override
    public void removeReminder(Reminder reminder) {
        // Create backup before modifying reminders
        backupManager.createBackup("remove-reminder");
        reminderManager.removeReminder(reminder);
    }

    @Override
    public List<Reminder> getDueReminders(int minutesAhead, java.util.Set<String> openedChecklists) {
        return reminderManager.getDueReminders(minutesAhead, openedChecklists);
    }

    @Override
    public LocalDateTime getNextReminderTime(java.util.Set<String> openedChecklists) {
        return reminderManager.getNextReminderTime(openedChecklists);
    }

    @Override
    public Set<String> getChecklistNames() {
        return checklistNameManager.getChecklistNames();
    }

    @Override
    public void addChecklistName(String name) {
        // Create backup before modifying checklist names
        backupManager.createBackup("add-checklist");
        checklistNameManager.addChecklistName(name);
    }

    @Override
    public void removeChecklistName(String name) {
        // Create backup before modifying checklist names
        backupManager.createBackup("remove-checklist");
        checklistNameManager.removeChecklistName(name);
    }

    // Public method to manually trigger backup
    public void createManualBackup() {
        backupManager.createManualBackup();
    }

    // Method to shutdown backup system
    public void shutdownBackupSystem() {
        if (backupManager != null) {
            backupManager.shutdown();
        }
    }

    @Override
    public void shutdown() {
        shutdownBackupSystem();
    }
}