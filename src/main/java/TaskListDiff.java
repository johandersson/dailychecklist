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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes differences between two task lists.
 */
public class TaskListDiff {
    private final List<Task> currentTasks;
    private final List<Task> backupTasks;
    private final Map<String, Task> currentMap = new HashMap<>();
    private final Map<String, Task> backupMap = new HashMap<>();
    private int added = 0;
    private int removed = 0;
    private int modified = 0;

    public TaskListDiff(List<Task> currentTasks, List<Task> backupTasks) {
        this.currentTasks = currentTasks;
        this.backupTasks = backupTasks;

        // Build maps
        for (Task task : currentTasks) {
            currentMap.put(task.getId(), task);
        }
        for (Task task : backupTasks) {
            backupMap.put(task.getId(), task);
        }

        // Calculate differences
        calculateDiff();
    }

    private void calculateDiff() {
        for (Task backupTask : backupTasks) {
            Task currentTask = currentMap.get(backupTask.getId());
            if (currentTask == null) {
                removed++;
            } else if (!tasksEqual(backupTask, currentTask)) {
                modified++;
            }
        }

        for (Task currentTask : currentTasks) {
            if (!backupMap.containsKey(currentTask.getId())) {
                added++;
            }
        }
    }

    private boolean tasksEqual(Task t1, Task t2) {
        return t1.getName().equals(t2.getName()) &&
               t1.getType() == t2.getType() &&
               java.util.Objects.equals(t1.getWeekday(), t2.getWeekday()) &&
               t1.isDone() == t2.isDone() &&
               java.util.Objects.equals(t1.getDoneDate(), t2.getDoneDate()) &&
               java.util.Objects.equals(t1.getChecklistId(), t2.getChecklistId());
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current tasks: ").append(currentTasks.size()).append("\n");
        sb.append("Backup tasks: ").append(backupTasks.size()).append("\n");
        sb.append("Tasks added since backup: ").append(added).append("\n");
        sb.append("Tasks removed since backup: ").append(removed).append("\n");
        sb.append("Tasks modified since backup: ").append(modified).append("\n");
        return sb.toString();
    }

    public String getDetailedChanges() {
        StringBuilder sb = new StringBuilder();

        // Added tasks
        if (added > 0) {
            sb.append("ADDED TASKS:\n");
            for (Task task : currentTasks) {
                if (!backupMap.containsKey(task.getId())) {
                    sb.append("+ ").append(taskToString(task)).append("\n");
                }
            }
            sb.append("\n");
        }

        // Removed tasks
        if (removed > 0) {
            sb.append("REMOVED TASKS:\n");
            for (Task task : backupTasks) {
                if (!currentMap.containsKey(task.getId())) {
                    sb.append("- ").append(taskToString(task)).append("\n");
                }
            }
            sb.append("\n");
        }

        // Modified tasks
        if (modified > 0) {
            sb.append("MODIFIED TASKS:\n");
            for (Task backupTask : backupTasks) {
                Task currentTask = currentMap.get(backupTask.getId());
                if (currentTask != null && !tasksEqual(backupTask, currentTask)) {
                    sb.append("~ ").append(taskToString(backupTask)).append(" -> ").append(taskToString(currentTask)).append("\n");
                }
            }
        }

        return sb.toString();
    }

    private String taskToString(Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(task.getType()).append("] ");
        sb.append(task.getName());
        if (task.getChecklistId() != null) {
            sb.append(" (").append(task.getChecklistId()).append(")");
        }
        if (task.getWeekday() != null) {
            sb.append(" [").append(task.getWeekday()).append("]");
        }
        if (task.isDone()) {
            sb.append(" [DONE]");
        }
        return sb.toString();
    }
}