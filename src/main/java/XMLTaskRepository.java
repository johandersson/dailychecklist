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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class XMLTaskRepository implements TaskRepository {
    private static String FILE_NAME = ApplicationConfiguration.APPLICATION_DATA_DIR + File.separator + ApplicationConfiguration.DATA_FILE_NAME;
    private static String REMINDER_FILE_NAME = ApplicationConfiguration.APPLICATION_DATA_DIR + File.separator + ApplicationConfiguration.REMINDERS_FILE_NAME;
    private static String CHECKLIST_NAMES_FILE_NAME = ApplicationConfiguration.APPLICATION_DATA_DIR + File.separator + ApplicationConfiguration.CHECKLIST_NAMES_FILE_NAME;

    // Component managers
    private TaskStaxHandler taskXmlHandler;
    private TaskXmlHandler fallbackXmlHandler;
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
    // Single-threaded executor for all writes to avoid concurrent DOM writes and to perform persistence off the EDT
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "xml-persist-worker");
        t.setDaemon(true);
        return t;
    });

    // Coalescer for write debounce: collect frequent updates and flush them as a batch
    private final java.util.concurrent.ConcurrentMap<String, Task> pendingWrites = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ScheduledExecutorService coalesceScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "xml-write-coalescer");
        t.setDaemon(true);
        return t;
    });
    private volatile java.util.concurrent.ScheduledFuture<?> coalesceFuture = null;
    private final long COALESCE_DELAY_MS = 300; // short window to coalesce frequent updates

    // Read/write lock to allow concurrent readers but exclusive writers for cache access
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    // Parent component for error dialogs
    private Component parentComponent;

    // Lightweight retry/backoff: attempts before showing user-visible error
    private static final int PERSIST_MAX_ATTEMPTS = 3;
    private static final long PERSIST_INITIAL_BACKOFF_MS = 200L;

    /**
     * Submit a persistence task to the write executor with simple retry/backoff semantics.
     * On repeated failure the in-memory cache is marked dirty and a user-visible error is shown on the EDT when possible.
     */
    private void submitPersistWithRetry(Runnable persistOp, String context) {
        writeExecutor.submit(() -> {
            int attempts = 0;
            long backoff = PERSIST_INITIAL_BACKOFF_MS;
            while (true) {
                try {
                    persistOp.run();
                    return;
                } catch (Exception e) {
                    attempts++;
                    if (attempts >= PERSIST_MAX_ATTEMPTS) {
                        rwLock.writeLock().lock();
                        try { tasksCacheDirty = true; } finally { rwLock.writeLock().unlock(); }
                        if (parentComponent != null) {
                            final Exception ex = e instanceof Exception ? (Exception)e : new Exception(e);
                            javax.swing.SwingUtilities.invokeLater(() -> ApplicationErrorHandler.showDataSaveError(parentComponent, context, ex));
                        } else {
                            System.err.println("Failed persisting " + context + ": " + e.getMessage());
                        }
                        return;
                    }
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    backoff *= 2L;
                }
            }
        });
    }

    /**
     * Sets the parent component for error dialogs.
     */
    public void setParentComponent(Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    @FunctionalInterface
    private interface PersistOperation {
        void run() throws Exception;
    }

    /**
     * Submits a persistence operation to the single-threaded write executor with
     * a small retry/backoff strategy and lightweight logging. If the operation
     * fails repeatedly the repository cache is marked dirty and a user-visible
     * error dialog is shown (if a parent component is configured).
     */
    private void submitWithRetries(String desc, PersistOperation op) {
        writeExecutor.submit(() -> {
            int attempts = 0;
            long backoff = 500; // ms
            while (true) {
                try {
                    long start = System.nanoTime();
                    op.run();
                    long dur = System.nanoTime() - start;
                    MetricsCollector.record("Persist succeeded (" + desc + ") in " + (dur / 1_000_000.0) + " ms");
                    return;
                } catch (Exception e) {
                    attempts++;
                    MetricsCollector.record("Persist failed (" + desc + ") attempt " + attempts + ": " + e.getMessage());
                    rwLock.writeLock().lock();
                    try { tasksCacheDirty = true; } finally { rwLock.writeLock().unlock(); }
                    if (attempts >= 3) {
                        if (parentComponent != null) {
                            final Exception ex = e;
                            javax.swing.SwingUtilities.invokeLater(() -> ApplicationErrorHandler.showDataSaveError(parentComponent, desc, ex));
                        }
                        return;
                    }
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    backoff *= 2;
                }
            }
        });
    }

    /**
     * Like {@link #submitWithRetries} but quiet: never shows a user-visible error dialog.
     * Intended for coalesced (debounced) background flushes where we prefer silent
     * logging and marking the cache dirty instead of interrupting the user.
     */
    private void submitWithRetriesQuiet(String desc, PersistOperation op) {
        writeExecutor.submit(() -> {
            int attempts = 0;
            long backoff = 500; // ms
            while (true) {
                try {
                    op.run();
                    return;
                } catch (Exception e) {
                    attempts++;
                    java.util.logging.Logger.getLogger(XMLTaskRepository.class.getName())
                        .log(java.util.logging.Level.WARNING, "Quiet persist failed (" + desc + ") attempt " + attempts, e);
                    rwLock.writeLock().lock();
                    try { tasksCacheDirty = true; } finally { rwLock.writeLock().unlock(); }
                    if (attempts >= 3) {
                        // Quiet: do not show dialogs. Just log and give up.
                        java.util.logging.Logger.getLogger(XMLTaskRepository.class.getName())
                            .log(java.util.logging.Level.SEVERE, "Giving up quiet persist (" + desc + ") after " + attempts + " attempts");
                        return;
                    }
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    backoff *= 2;
                }
            }
        });
    }

    private void scheduleCoalescedFlushIfNeeded() {
        if (coalesceFuture != null && !coalesceFuture.isDone()) return;
        coalesceFuture = coalesceScheduler.schedule(this::flushPendingWrites, COALESCE_DELAY_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void flushPendingWrites() {
        try {
            if (pendingWrites.isEmpty()) return;
            List<Task> batch = new ArrayList<>(pendingWrites.values());
            pendingWrites.clear();
            // Use the quiet retry path for coalesced flushes to avoid showing a dialog
            // for transient background write failures.
            submitWithRetriesQuiet("tasks-update-coalesced", () -> persistUpdateTasks(batch));
                } catch (Exception e) {
            MetricsCollector.record("Failed to flush coalesced writes: " + e.getMessage());
        }
    }

    // Persistence helpers: try StAX first, fall back to DOM-based TaskXmlHandler on error.
    private void persistAdd(Task task) throws Exception {
        try {
            taskXmlHandler.addTask(task);
        } catch (Exception e) {
            fallbackXmlHandler.addTask(task);
        }
    }

    private void persistUpdateTasks(List<Task> tasks) throws Exception {
        try {
            taskXmlHandler.updateTasks(tasks);
        } catch (Exception e) {
            fallbackXmlHandler.updateTasks(tasks);
        }
    }

    private void persistRemove(Task task) throws Exception {
        try {
            taskXmlHandler.removeTask(task);
        } catch (Exception e) {
            fallbackXmlHandler.removeTask(task);
        }
    }

    private void persistSetAllTasks(List<Task> tasks) throws Exception {
        try {
            taskXmlHandler.setAllTasks(tasks);
        } catch (Exception e) {
            fallbackXmlHandler.setAllTasks(tasks);
        }
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
        taskXmlHandler = new TaskStaxHandler(FILE_NAME);
        fallbackXmlHandler = new TaskXmlHandler(FILE_NAME);
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
                // Ensure parent data directory exists and create an empty tasks document safely
                ApplicationConfiguration.ensureDataDirectoryExists();
                try {
                    taskXmlHandler.ensureFileExists();
                } catch (Exception e) {
                    // StAX handler failed to ensure file; try DOM fallback
                    fallbackXmlHandler.ensureFileExists();
                }
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
        // Use read-lock for the fast path, escalate to write-lock when cache needs loading
        rwLock.readLock().lock();
        try {
            if (!tasksCacheDirty && cachedTasks != null) {
                return java.util.Collections.unmodifiableList(cachedTasks);
            }
        } finally {
            rwLock.readLock().unlock();
        }

        // Need to (re)load the cache under write lock
        rwLock.writeLock().lock();
        try {
            if (!tasksCacheDirty && cachedTasks != null) {
                return java.util.Collections.unmodifiableList(cachedTasks);
            }
            ensureDataFileExists();
            try {
                try {
                    cachedTasks = taskXmlHandler.parseAllTasks();
                } catch (Exception e) {
                    // Try DOM fallback when StAX parsing fails (migrate if possible)
                    try {
                        cachedTasks = fallbackXmlHandler.parseAllTasks();
                        // Schedule a background migration to StAX format
                        List<Task> migrated = new ArrayList<>(cachedTasks);
                        submitWithRetriesQuiet("migrate-xml-to-stax", () -> taskXmlHandler.setAllTasks(migrated));
                    } catch (Exception ex) {
                        throw ex;
                    }
                }
                // Memory safety check
                if (MemorySafetyManager.checkTaskLimit(cachedTasks.size())) {
                    cachedTasks = cachedTasks.subList(0, Math.min(MemorySafetyManager.MAX_TASKS, cachedTasks.size()));
                }
                rebuildMapsFromCachedTasks();
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
            return java.util.Collections.unmodifiableList(cachedTasks);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Rebuilds the lookup maps from the current cachedTasks list. Caller must hold write lock.
     */
    private void rebuildMapsFromCachedTasks() {
        taskMap = new HashMap<>();
        tasksByType = new HashMap<>();
        tasksByChecklist = new HashMap<>();
        for (Task task : cachedTasks) {
            taskMap.put(task.getId(), task);
            tasksByType.computeIfAbsent(task.getType(), k -> new ArrayList<>()).add(task);
            if (task.getType() == TaskType.CUSTOM && task.getChecklistId() != null) {
                tasksByChecklist.computeIfAbsent(task.getChecklistId(), k -> new ArrayList<>()).add(task);
            }
        }
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
    public synchronized void addTask(Task task) {
        // Validate task before saving
        if (!TaskXmlHandler.validateTask(task)) {
            System.err.println("Invalid task data, skipping save: " + task);
            return;
        }

        // Update in-memory cache first and persist asynchronously
        rwLock.writeLock().lock();
        try {
            if (cachedTasks == null) cachedTasks = new ArrayList<>();
            cachedTasks.add(task);
            rebuildMapsFromCachedTasks();
            tasksCacheDirty = false;
        } finally {
            rwLock.writeLock().unlock();
        }

        // Persist off the write lock to avoid blocking readers (with retries)
        submitWithRetries("task-add", () -> persistAdd(task));
    }

    @Override
    public synchronized void updateTask(Task task) {
        // Validate task before saving
        if (!TaskXmlHandler.validateTask(task)) {
            System.err.println("Invalid task data, skipping save: " + task);
            return;
        }

        System.out.println("[TRACE] XMLTaskRepository.updateTask start id=" + task.getId() + ", thread=" + Thread.currentThread().getName());
        // Update in-memory cache immediately
        rwLock.writeLock().lock();
        try {
            if (cachedTasks == null) getCachedTasks();
            boolean replaced = false;
            for (int i = 0; i < cachedTasks.size(); i++) {
                if (cachedTasks.get(i).getId().equals(task.getId())) {
                    cachedTasks.set(i, task);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) cachedTasks.add(task);
            rebuildMapsFromCachedTasks();
            tasksCacheDirty = false;
        } finally {
            rwLock.writeLock().unlock();
        }

        // Add to coalesced pending writes and schedule a batched flush
        pendingWrites.put(task.getId(), task);
        scheduleCoalescedFlushIfNeeded();
        System.out.println("[TRACE] XMLTaskRepository.updateTask scheduled coalesced persist id=" + task.getId());
    }

    /**
     * Updates a task without showing error dialogs (for UI state management).
     * Returns true if successful, false if failed.
     */
    public synchronized boolean updateTaskQuiet(Task task) {
        // Update in-memory first and persist asynchronously
        try {
            System.out.println("[TRACE] XMLTaskRepository.updateTaskQuiet scheduling id=" + task.getId() + ", thread=" + Thread.currentThread().getName());
            rwLock.writeLock().lock();
            try {
                if (cachedTasks == null) getCachedTasks();
                boolean replaced = false;
                for (int i = 0; i < cachedTasks.size(); i++) {
                    if (cachedTasks.get(i).getId().equals(task.getId())) {
                        cachedTasks.set(i, task);
                        replaced = true;
                        break;
                    }
                }
                if (!replaced) cachedTasks.add(task);
                rebuildMapsFromCachedTasks();
                tasksCacheDirty = false;
            } finally {
                rwLock.writeLock().unlock();
            }

            // Coalesce quiet updates as well (batched flush will persist them)
            pendingWrites.put(task.getId(), task);
            scheduleCoalescedFlushIfNeeded();
            return true;
        } catch (Exception e) {
            System.out.println("[TRACE] XMLTaskRepository.updateTaskQuiet failed scheduling id=" + task.getId() + ", err=" + e.getMessage());
            return false;
        }
    }

    /**
     * Atomically update multiple tasks and write once.
     */
    public synchronized void updateTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return;
        try {
            // Update in-memory cache first under write lock
            rwLock.writeLock().lock();
            try {
                if (cachedTasks == null) getCachedTasks();
                // Replace or add each task
                for (Task t : tasks) {
                    boolean found = false;
                    for (int i = 0; i < cachedTasks.size(); i++) {
                        if (cachedTasks.get(i).getId().equals(t.getId())) {
                            cachedTasks.set(i, t);
                            found = true;
                            break;
                        }
                    }
                    if (!found) cachedTasks.add(t);
                }
                rebuildMapsFromCachedTasks();
                tasksCacheDirty = false;
            } finally {
                rwLock.writeLock().unlock();
            }

            // Persist asynchronously (with retries/backoff)
            submitWithRetries("tasks-update", () -> {
                long start = System.nanoTime();
                persistUpdateTasks(tasks);
                long dur = System.nanoTime() - start;
                MetricsCollector.record("updateTasks wrote " + tasks.size() + " tasks in " + (dur / 1_000_000.0) + " ms");
            });
        } catch (Exception e) {
            if (parentComponent != null) {
                ApplicationErrorHandler.showDataSaveError(parentComponent, "tasks", e);
            }
        }
    }

    /**
     * Quiet variant of updateTasks for background persistence.
     */
    public synchronized boolean updateTasksQuiet(List<Task> tasks) {
        // Update in-memory first and persist asynchronously
        try {
            rwLock.writeLock().lock();
            try {
                if (cachedTasks == null) getCachedTasks();
                for (Task t : tasks) {
                    boolean found = false;
                    for (int i = 0; i < cachedTasks.size(); i++) {
                        if (cachedTasks.get(i).getId().equals(t.getId())) {
                            cachedTasks.set(i, t);
                            found = true;
                            break;
                        }
                    }
                    if (!found) cachedTasks.add(t);
                }
                rebuildMapsFromCachedTasks();
                tasksCacheDirty = false;
            } finally {
                rwLock.writeLock().unlock();
            }

            submitWithRetries("tasks-update-quiet", () -> persistUpdateTasks(tasks));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized void removeTask(Task task) {
        // Remove from in-memory cache then persist asynchronously
        rwLock.writeLock().lock();
        List<Task> snapshot = null;
        try {
            if (cachedTasks == null) getCachedTasks();
            cachedTasks.removeIf(t -> t.getId().equals(task.getId()));
            rebuildMapsFromCachedTasks();
            tasksCacheDirty = false;
            // Capture a snapshot to persist without re-reading the file
            snapshot = new ArrayList<>(cachedTasks);
        } finally {
            rwLock.writeLock().unlock();
        }

        // Persist the full snapshot to avoid parse+partial-write on each removal.
        // This reduces I/O when many deletes happen in succession.
        final List<Task> toPersist = snapshot;
        submitWithRetries("task-remove-" + task.getId(), () -> persistSetAllTasks(toPersist));
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
    public synchronized void setTasks(List<Task> tasks) {
        try {
            // Create a backup before replacing all tasks
            if (backupManager != null) {
                backupManager.createBackup("before-set-all-tasks");
            }
            persistSetAllTasks(tasks);
            // Update in-memory representation immediately
            rwLock.writeLock().lock();
            try {
                cachedTasks = new ArrayList<>(tasks);
                rebuildMapsFromCachedTasks();
                tasksCacheDirty = false;
            } finally {
                rwLock.writeLock().unlock();
            }

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
            Set<Checklist> foundChecklists = collectChecklistsFromTasks(tasks);
            updateChecklistRegistry(foundChecklists);
        } catch (Exception e) {
            System.err.println("Failed to rebuild checklist names registry: " + e.getMessage());
        }
    }

    /**
     * Extracts checklist references from tasks. Handles both UUID-like ids and
     * older backups where the checklist name was stored in the id field.
     */
    private Set<Checklist> collectChecklistsFromTasks(List<Task> tasks) {
        Set<Checklist> found = new HashSet<>();
        for (Task task : tasks) {
            if (task.getType() != TaskType.CUSTOM) continue;
            String checklistId = task.getChecklistId();
            if (checklistId == null) continue;
            String trimmed = checklistId.trim();
            if (trimmed.isEmpty()) continue;

            if (looksLikeUuid(trimmed)) {
                Checklist c = checklistNameManager.getChecklistById(trimmed);
                if (c == null) {
                    c = new Checklist(trimmed, "Untitled Checklist");
                }
                found.add(c);
            } else {
                // Old-style: the field contains the name
                Checklist c = checklistNameManager.getChecklistByName(trimmed);
                if (c == null) {
                    c = new Checklist(trimmed);
                }
                found.add(c);
            }
        }
        return found;
    }

    /**
     * Update the persistent checklist name registry with the discovered checklists.
     * If an entry is missing it will be added; if an existing entry has an empty
     * name, it will be updated.
     */
    private void updateChecklistRegistry(Set<Checklist> foundChecklists) {
        for (Checklist checklist : foundChecklists) {
            Checklist existing = checklistNameManager.getChecklistById(checklist.getId());
            if (existing == null) {
                checklistNameManager.addChecklist(checklist);
            } else if ((existing.getName() == null || existing.getName().trim().isEmpty())
                    && checklist.getName() != null && !checklist.getName().trim().isEmpty()) {
                checklistNameManager.updateChecklistName(existing, checklist.getName());
            }
        }
    }

    private boolean looksLikeUuid(String s) {
        return s.matches("[0-9a-fA-F\\-]{36}");
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
    public Set<Checklist> getChecklists() {
        return checklistNameManager.getChecklists();
    }

    @Override
    public void addChecklist(Checklist checklist) {
        checklistNameManager.addChecklist(checklist);
    }

    @Override
    public void updateChecklistName(Checklist checklist, String newName) {
        checklistNameManager.updateChecklistName(checklist, newName);
    }

    @Override
    public void removeChecklist(Checklist checklist) {
        checklistNameManager.removeChecklist(checklist);
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
        // Shutdown executor
        try {
            writeExecutor.shutdownNow();
        } catch (Exception ignore) {}
        try {
            coalesceScheduler.shutdownNow();
        } catch (Exception ignore) {}
    }

    @Override
    public void start() {
        // Start the automatic backup system
        backupManager.start();
    }
}