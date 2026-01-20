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
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;

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

    @Override
    public void initialize() {
        // Initialize component managers
        taskXmlHandler = new TaskXmlHandler(FILE_NAME);
        reminderManager = new ReminderManager(REMINDER_FILE_NAME, FILE_NAME);
        checklistNameManager = new ChecklistNameManager(CHECKLIST_NAMES_FILE_NAME);

        // Create the XML file if it doesn't exist
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            try {
                // Create an empty tasks document
                taskXmlHandler.setAllTasks(new ArrayList<>());
            } catch (Exception e) {
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(null, "Failed to initialize task file: " + e.getMessage(), "Initialization Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        // Initialize backup system
        String backupDir = System.getProperty("user.home") + File.separator + "dailychecklist-backups";
        String settingsFile = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "settings.ini";
        String[] dataFiles = {FILE_NAME, REMINDER_FILE_NAME, CHECKLIST_NAMES_FILE_NAME, settingsFile};
        backupManager = new BackupManager(backupDir, 10, 30 * 60 * 1000, dataFiles);
        backupManager.initialize();
    }

    @Override
    public List<Task> getDailyTasks() {
        List<Task> tasks = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(System.currentTimeMillis()));

        try {
            List<Task> allTasks = taskXmlHandler.parseAllTasks();

            // Memory safety check
            if (MemorySafetyManager.checkTaskLimit(allTasks.size())) {
                allTasks = allTasks.subList(0, Math.min(MemorySafetyManager.MAX_TASKS, allTasks.size()));
            }

            for (Task task : allTasks) {
                taskXmlHandler.checkAndResetPastDoneDate(task, today);
                tasks.add(task);
            }
        } catch (Exception e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to load daily tasks: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        return tasks;
    }

    @Override
    public List<Task> getAllTasks() {
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
                taskXmlHandler.checkAndResetPastDoneDate(task, today);
            }
        } catch (Exception e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to load all tasks: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
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
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to add task: " + e.getMessage(), "Add Task Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void updateTask(Task task) {
        try {
            taskXmlHandler.updateTask(task);
            backupManager.createBackup("save");
        } catch (Exception e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to update task: " + e.getMessage(), "Update Task Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void removeTask(Task task) {
        try {
            taskXmlHandler.removeTask(task);
            backupManager.createBackup("save");
        } catch (Exception e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to remove task: " + e.getMessage(), "Remove Task Error", JOptionPane.ERROR_MESSAGE);
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
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to check for undone tasks: " + e.getMessage(), "Check Error", JOptionPane.ERROR_MESSAGE);
            }
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
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to set tasks: " + e.getMessage(), "Set Tasks Error", JOptionPane.ERROR_MESSAGE);
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