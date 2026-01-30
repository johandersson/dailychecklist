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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskManager {
    private final TaskRepository repository;
    private final List<TaskChangeListener> listeners = new CopyOnWriteArrayList<>();
    // Cache of subtasks grouped by parentId; rebuilt when tasks change to avoid repeated sorting
    private volatile java.util.Map<String, java.util.List<Task>> cachedSubtasksByParent = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean subtasksCacheValid = false;

    public TaskManager(TaskRepository repository) {
        this.repository = repository;
        this.repository.initialize();
    }

    /** Lightweight listener for model changes. */
    public interface TaskChangeListener { void onChange(); }

    public void addTaskChangeListener(TaskChangeListener l) { if (l != null) listeners.add(l); }
    public void removeTaskChangeListener(TaskChangeListener l) { if (l != null) listeners.remove(l); }
    private void notifyListeners() { for (TaskChangeListener l : listeners) { try { l.onChange(); } catch (Exception ignore) {} } }

    /**
     * Public helper to notify registered task-change listeners.
     * Use this when model changes were persisted off the EDT and the UI needs refreshing.
     */
    public void notifyTaskChangeListeners() { notifyListeners(); }

    public List<Task> getDailyTasks() {
        return repository.getDailyTasks();
    }

    public List<Task> getAllTasks() {
        return repository.getAllTasks();
    }

    public Task getTaskById(String id) {
        if (repository instanceof XMLTaskRepository xmlRepo) {
            return xmlRepo.getTaskById(id);
        }
        // Fallback: linear search
        for (Task task : getAllTasks()) {
            if (task.getId().equals(id)) {
                return task;
            }
        }
        return null;
    }

    public void addTask(Task task) {
        // Validation: only one heading per parent
        if (task != null && task.getType() == TaskType.HEADING) {
            String pid = task.getParentId();
            if (pid == null || pid.trim().isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(null,
                    "Heading must refer to a parent task.",
                    "Invalid Heading", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (Task t : getAllTasks()) {
                if (t.getType() == TaskType.HEADING && pid.equals(t.getParentId())) {
                    javax.swing.JOptionPane.showMessageDialog(null,
                        "A heading already exists for the selected parent.",
                        "Duplicate Heading", javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
        repository.addTask(task);
        invalidateSubtasksCache();
        notifyListeners();
    }

    public void updateTask(Task task) {
        repository.updateTask(task);
        invalidateSubtasksCache();
        notifyListeners();
    }

    /**
     * Persist a single task immediately (non-coalesced) so the change is
     * visible to other panels without waiting for the coalescer.
     */
    public void updateTaskImmediate(Task task) {
        if (repository instanceof XMLTaskRepository xmlRepo) {
            java.util.List<Task> single = new java.util.ArrayList<>();
            single.add(task);
            xmlRepo.updateTasks(single);
        } else {
            repository.updateTask(task);
        }
        invalidateSubtasksCache();
        notifyListeners();
    }

    /**
     * Updates a task without showing error dialogs.
     * Returns true if successful, false if failed.
     */
    public boolean updateTaskQuiet(Task task) {
        if (repository instanceof XMLTaskRepository xmlRepo) {
            return xmlRepo.updateTaskQuiet(task);
        }
        // Fallback for other repository types
        try {
            repository.updateTask(task);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Updates multiple tasks without showing error dialogs.
     * Returns true if successful.
     */
    public boolean updateTasksQuiet(java.util.List<Task> tasks) {
        if (repository instanceof XMLTaskRepository xmlRepo) {
            return xmlRepo.updateTasksQuiet(tasks);
        }
        try {
            for (Task t : tasks) repository.updateTask(t);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Atomically updates multiple tasks and notifies listeners.
     */
    public void updateTasks(java.util.List<Task> tasks) {
        if (repository instanceof XMLTaskRepository xmlRepo) {
            xmlRepo.updateTasks(tasks);
        } else {
            for (Task t : tasks) repository.updateTask(t);
        }
        invalidateSubtasksCache();
        notifyListeners();
    }

    public void removeTask(Task task) {
        repository.removeTask(task);
        invalidateSubtasksCache();
        notifyListeners();
    }

    public List<Task> getTasks(TaskType type, Checklist checklist) {
        List<Task> allTasks = repository.getAllTasks();
        List<Task> filtered = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.getType() == type) {
                // For CUSTOM tasks, also check checklist id matches
                if (type != TaskType.CUSTOM || checklist == null || Objects.equals(checklist.getId(), task.getChecklistId())) {
                    filtered.add(task);
                }
            }
        }
        return filtered;
    }

    /**
     * Returns direct subtasks (one level) for the given parent id.
     * Uses cached version for better performance.
     */
    public List<Task> getSubtasks(String parentId) {
        if (parentId == null) return new ArrayList<>();
        rebuildSubtasksCacheIfNeeded();
        java.util.List<Task> list = cachedSubtasksByParent.get(parentId);
        return list == null ? new ArrayList<>() : new ArrayList<>(list); // Return mutable copy
    }

    /**
     * Returns a Reminder targeting the given taskId, or null if none.
     */
    public Reminder getReminderForTask(String taskId) {
        if (taskId == null) return null;
        for (Reminder r : getReminders()) {
            if (taskId.equals(r.getTaskId())) return r;
        }
        return null;
    }

    /**
     * Convenience: get checklist display name by id (returns null if not found)
     */
    public String getChecklistNameById(String checklistId) {
        if (checklistId == null) return null;
        for (Checklist c : getCustomChecklists()) {
            if (checklistId.equals(c.getId())) return c.getName();
        }
        return null;
    }

    public java.util.Set<Checklist> getCustomChecklists() {
        return repository.getChecklists();
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
        // Sanitize incoming task list: ensure headings reference a parent and only one heading per parent
        if (tasks != null) {
            java.util.Set<String> seenHeadingParents = new java.util.HashSet<>();
            java.util.Iterator<Task> it = tasks.iterator();
            while (it.hasNext()) {
                Task t = it.next();
                if (t.getType() == TaskType.HEADING) {
                    String pid = t.getParentId();
                    if (pid == null || pid.trim().isEmpty() || seenHeadingParents.contains(pid)) {
                        // remove invalid or duplicate heading
                        it.remove();
                        continue;
                    }
                    seenHeadingParents.add(pid);
                }
            }
        }
        repository.setTasks(tasks);
        invalidateSubtasksCache();
        notifyListeners();
    }

    /**
     * Invalidate cached subtasks map. Call after any data-modifying operation.
     */
    private void invalidateSubtasksCache() {
        subtasksCacheValid = false;
        cachedSubtasksByParent.clear();
    }

    /**
     * Rebuild the subtasks-by-parent cache if it's invalid.
     */
    private synchronized void rebuildSubtasksCacheIfNeeded() {
        if (subtasksCacheValid) return;
        java.util.Map<String, java.util.List<Task>> map = new java.util.HashMap<>();
        for (Task t : getAllTasks()) {
            // Treat HEADING entries as GUI-only and not as real subtasks when building subtask cache
            if (t.getType() == TaskType.HEADING) continue;
            String pid = t.getParentId();
            if (pid != null) {
                map.computeIfAbsent(pid, k -> new java.util.ArrayList<>()).add(t);
            }
        }
        // Preserve persisted ordering for subtasks (use global task order)
        for (java.util.Map.Entry<String, java.util.List<Task>> e : map.entrySet()) {
            java.util.List<Task> list = e.getValue();
            cachedSubtasksByParent.put(e.getKey(), java.util.Collections.unmodifiableList(list));
        }
        subtasksCacheValid = true;
    }

    /**
     * Returns direct subtasks for the given parent id, cached and pre-sorted by name.
     */
    public java.util.List<Task> getSubtasksSorted(String parentId) {
        if (parentId == null) return java.util.Collections.emptyList();
        rebuildSubtasksCacheIfNeeded();
        java.util.List<Task> list = cachedSubtasksByParent.get(parentId);
        return list == null ? java.util.Collections.emptyList() : list;
    }

    public List<Reminder> getReminders() {
        return repository.getReminders();
    }

    public void addReminder(Reminder reminder) {
        repository.addReminder(reminder);
        notifyListeners();
    }

    public void removeReminder(Reminder reminder) {
        repository.removeReminder(reminder);
        notifyListeners();
    }

    public void addChecklist(Checklist checklist) {
        repository.addChecklist(checklist);
        notifyListeners();
    }

    public void removeChecklist(Checklist checklist) {
        repository.removeChecklist(checklist);
        notifyListeners();
    }

    public void updateChecklistName(Checklist checklist, String newName) {
        repository.updateChecklistName(checklist, newName);
        notifyListeners();
    }

    /**
     * Moves a task to a different custom checklist.
     */
    public void moveTaskToChecklist(Task task, Checklist newChecklist) {
        task.setChecklistId(newChecklist.getId());
        updateTask(task);
    }

    /**
     * Manually creates a backup of all data.
     */
    public void createManualBackup() {
        if (repository instanceof XMLTaskRepository xmlRepo) {
            xmlRepo.createManualBackup();
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
        if (!trimmed.matches("[\\p{L}\\p{N}\\s\\-_.()!]+")) {
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
