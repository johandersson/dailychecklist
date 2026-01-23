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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XMLTaskRepository implements TaskRepository {
    private static String FILE_NAME = ApplicationConfiguration.APPLICATION_DATA_DIR + File.separator + ApplicationConfiguration.DATA_FILE_NAME;
    private static String REMINDER_FILE_NAME = ApplicationConfiguration.APPLICATION_DATA_DIR + File.separator + ApplicationConfiguration.REMINDERS_FILE_NAME;
    private static String CHECKLIST_NAMES_FILE_NAME = ApplicationConfiguration.APPLICATION_DATA_DIR + File.separator + ApplicationConfiguration.CHECKLIST_NAMES_FILE_NAME;

    // Component managers
    private TaskXmlHandler taskXmlHandler;
    private ReminderManager reminderManager;
    private ChecklistNameManager checklistNameManager;

    // Backup system
    private BackupManager backupManager;

    // Task caching
    private List<Task> cachedTasks = null;
    private Map<String, Task> taskMap = null;
    private Map<TaskType, List<Task>> tasksByType = null;
    private Map<String, List<Task>> tasksByChecklist = null;
    private boolean tasksCacheDirty = true;

    // Parent component for error dialogs
    private Component parentComponent;

    /**
     * Sets the parent component for error dialogs.
     */
    public void setParentComponent(Component parentComponent) {
        this.parentComponent = parentComponent;
    }

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

        // Register any orphaned checklist names from existing tasks
        registerOrphanedChecklistNames();
    }

    /**
     * Scans all tasks and ensures their checklist names are registered.
     * This prevents tasks from becoming invisible if their checklist names weren't properly saved.
     * Uses efficient streaming XML parsing to minimize memory usage and startup time.
     */
    private void registerOrphanedChecklistNames() {
        try {
            Set<String> existingChecklistNames = checklistNameManager.getChecklistNames();
            Set<String> orphanedNames = findOrphanedChecklistNames(existingChecklistNames);

            // Only add names that aren't already registered
            for (String checklistName : orphanedNames) {
                checklistNameManager.addChecklistName(checklistName);
            }
        } catch (Exception e) {
            // Log error but don't fail initialization
            System.err.println("Failed to register orphaned checklist names: " + e.getMessage());
        }
    }

    /**
     * Efficiently scans the XML file to find checklist names that exist in tasks but not in the registry.
     * Uses streaming XML parsing to avoid loading all tasks into memory.
     */
    private Set<String> findOrphanedChecklistNames(Set<String> existingChecklistNames) throws Exception {
        Set<String> orphanedNames = new HashSet<>();
        File file = new File(FILE_NAME);

        if (!file.exists()) {
            return orphanedNames; // No file means no orphaned names
        }

        javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();
        javax.xml.parsers.SAXParser saxParser = factory.newSAXParser();

        // Custom SAX handler that only extracts checklist names from CUSTOM tasks
        org.xml.sax.helpers.DefaultHandler handler = new org.xml.sax.helpers.DefaultHandler() {
            private boolean inCustomTask = false;
            private boolean inChecklistName = false;
            private StringBuilder checklistNameBuffer = new StringBuilder();

            @Override
            public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
                if ("task".equals(qName)) {
                    // Check if this is a CUSTOM task
                    String type = attributes.getValue("type");
                    if ("CUSTOM".equals(type)) {
                        inCustomTask = true;
                    }
                } else if (inCustomTask && "checklistName".equals(qName)) {
                    inChecklistName = true;
                    checklistNameBuffer.setLength(0); // Clear buffer
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                if (inChecklistName) {
                    checklistNameBuffer.append(ch, start, length);
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                if ("checklistName".equals(qName) && inChecklistName) {
                    String checklistName = checklistNameBuffer.toString().trim();
                    if (!checklistName.isEmpty() && !existingChecklistNames.contains(checklistName)) {
                        orphanedNames.add(checklistName);
                    }
                    inChecklistName = false;
                } else if ("task".equals(qName)) {
                    inCustomTask = false;
                }
            }
        };

        saxParser.parse(file, handler);
        return orphanedNames;
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
        List<Task> tasks = getCachedTasks();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(System.currentTimeMillis()));

        List<Task> dailyTasks = new ArrayList<>();
        for (Task task : tasks) {
            try {
                taskXmlHandler.checkAndResetPastDoneDate(task, today);
            } catch (ParseException e) {
                // Log the error but continue processing other tasks
                System.err.println("Failed to parse date for task " + task.getId() + ": " + e.getMessage());
            }
            dailyTasks.add(task);
        }

        return dailyTasks;
    }

    /**
     * Gets all tasks from cache, loading from XML if cache is dirty.
     * Applies memory safety checks.
     */
    private List<Task> getCachedTasks() {
        if (tasksCacheDirty) {
            ensureDataFileExists();
            try {
                cachedTasks = taskXmlHandler.parseAllTasks();
                // Memory safety check
                if (MemorySafetyManager.checkTaskLimit(cachedTasks.size())) {
                    cachedTasks = cachedTasks.subList(0, Math.min(MemorySafetyManager.MAX_TASKS, cachedTasks.size()));
                }
                // Populate task map for fast lookups
                taskMap = new HashMap<>();
                tasksByType = new HashMap<>();
                tasksByChecklist = new HashMap<>();
                for (Task task : cachedTasks) {
                    taskMap.put(task.getId(), task);
                    // Group by type
                    tasksByType.computeIfAbsent(task.getType(), k -> new ArrayList<>()).add(task);
                    // Group by checklist (for custom tasks)
                    if (task.getType() == TaskType.CUSTOM && task.getChecklistName() != null) {
                        tasksByChecklist.computeIfAbsent(task.getChecklistName(), k -> new ArrayList<>()).add(task);
                    }
                }
                tasksCacheDirty = false;
            } catch (Exception e) {
                if (parentComponent != null) {
                    ApplicationErrorHandler.showDataLoadError(parentComponent, "cached tasks", e);
                }
                cachedTasks = new ArrayList<>();
                taskMap = new HashMap<>();
                tasksByType = new HashMap<>();
                tasksByChecklist = new HashMap<>();
            }
        }
        return new ArrayList<>(cachedTasks);
    }

    @Override
    public List<Task> getAllTasks() {
        List<Task> tasks = getCachedTasks();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(System.currentTimeMillis()));

        // Check and reset past done dates
        for (Task task : tasks) {
            try {
                taskXmlHandler.checkAndResetPastDoneDate(task, today);
            } catch (ParseException e) {
                // Log the error but continue processing other tasks
                System.err.println("Failed to parse date for task " + task.getId() + ": " + e.getMessage());
            }
        }

        return tasks;
    }

    /**
     * Gets a task by its ID using the fast lookup map.
     * Returns null if not found.
     */
    public Task getTaskById(String id) {
        getCachedTasks(); // Ensure cache is loaded
        return taskMap.get(id);
    }

    /**
     * Gets tasks by type using the pre-grouped map.
     */
    public List<Task> getTasksByType(TaskType type) {
        getCachedTasks(); // Ensure cache is loaded
        return new ArrayList<>(tasksByType.getOrDefault(type, new ArrayList<>()));
    }

    /**
     * Gets tasks by checklist name using the pre-grouped map.
     */
    public List<Task> getTasksByChecklist(String checklistName) {
        getCachedTasks(); // Ensure cache is loaded
        return new ArrayList<>(tasksByChecklist.getOrDefault(checklistName, new ArrayList<>()));
    }

    @Override
    public void addTask(Task task) {
        try {
            taskXmlHandler.addTask(task);
            tasksCacheDirty = true; // Mark cache as dirty
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
            tasksCacheDirty = true; // Mark cache as dirty
        } catch (Exception e) {
            if (parentComponent != null) {
                ApplicationErrorHandler.showDataSaveError(parentComponent, "task", e);
            }
        }
    }

    /**
     * Updates a task without showing error dialogs (for UI state management).
     * Returns true if successful, false if failed.
     */
    public boolean updateTaskQuiet(Task task) {
        try {
            taskXmlHandler.updateTask(task);
            tasksCacheDirty = true; // Mark cache as dirty
            return true;
        } catch (Exception e) {
            // Don't show error dialog, just return failure
            return false;
        }
    }

    @Override
    public void removeTask(Task task) {
        try {
            taskXmlHandler.removeTask(task);
            tasksCacheDirty = true; // Mark cache as dirty
        } catch (Exception e) {
            if (parentComponent != null) {
                ApplicationErrorHandler.showDataSaveError(parentComponent, "task", e);
            }
        }
    }

    @Override
    public boolean hasUndoneTasks() {
        List<Task> tasks = getCachedTasks();
        for (Task task : tasks) {
            if (!task.isDone()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setTasks(List<Task> tasks) {
        try {
            // Create a backup before replacing all tasks
            if (backupManager != null) {
                backupManager.createBackup("before-set-all-tasks");
            }
            taskXmlHandler.setAllTasks(tasks);
            tasksCacheDirty = true; // Mark cache as dirty

            // After setting tasks, rebuild the checklist names registry from the new tasks
            rebuildChecklistNamesRegistry(tasks);
        } catch (Exception e) {
            if (parentComponent != null) {
                ApplicationErrorHandler.showDataSaveError(parentComponent, "tasks", e);
            }
        }
    }

    /**
     * Rebuilds the checklist names registry from the provided tasks.
     * This ensures checklist names are properly registered after operations like backup restore.
     */
    private void rebuildChecklistNamesRegistry(List<Task> tasks) {
        try {
            Set<String> checklistNames = new HashSet<>();
            for (Task task : tasks) {
                if (task.getType() == TaskType.CUSTOM && task.getChecklistName() != null && !task.getChecklistName().trim().isEmpty()) {
                    checklistNames.add(task.getChecklistName().trim());
                }
            }

            // Clear existing registry and add all found names
            Set<String> existingNames = checklistNameManager.getChecklistNames();
            for (String existingName : existingNames) {
                if (!checklistNames.contains(existingName)) {
                    checklistNameManager.removeChecklistName(existingName);
                }
            }

            for (String checklistName : checklistNames) {
                if (!existingNames.contains(checklistName)) {
                    checklistNameManager.addChecklistName(checklistName);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to rebuild checklist names registry: " + e.getMessage());
        }
    }

    @Override
    public List<Reminder> getReminders() {
        return reminderManager.getReminders();
    }

    @Override
    public void addReminder(Reminder reminder) {
        reminderManager.addReminder(reminder);
    }

    @Override
    public void removeReminder(Reminder reminder) {
        reminderManager.removeReminder(reminder);
    }

    @Override
    public List<Reminder> getDueReminders(int minutesAhead, Set<String> openedChecklists) {
        return reminderManager.getDueReminders(minutesAhead, openedChecklists);
    }

    @Override
    public LocalDateTime getNextReminderTime(Set<String> openedChecklists) {
        return reminderManager.getNextReminderTime(openedChecklists);
    }

    @Override
    public Set<String> getChecklistNames() {
        return checklistNameManager.getChecklistNames();
    }

    @Override
    public void addChecklistName(String name) {
        checklistNameManager.addChecklistName(name);
    }

    @Override
    public void removeChecklistName(String name) {
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
        // Create final backup before shutting down
        backupManager.createBackup("shutdown");
        shutdownBackupSystem();
        
        // Clear cache to free memory
        cachedTasks = null;
        tasksCacheDirty = true;
    }

    @Override
    public void start() {
        // Start the automatic backup system
        backupManager.start();
    }
}