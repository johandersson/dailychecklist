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
 */import java.util.List;

public interface TaskRepository {
    void initialize();
    List<Task> getDailyTasks();
    List<Task> getAllTasks();
    void addTask(Task task);
    void updateTask(Task task);
    void removeTask(Task task);
    boolean hasUndoneTasks();
    void setTasks(List<Task> tasks);

    List<Reminder> getReminders();
    void addReminder(Reminder reminder);
    void removeReminder(Reminder reminder);

    /**
     * Gets reminders that are due within the next specified minutes.
     */
    List<Reminder> getDueReminders(int minutesAhead, java.util.Set<String> openedChecklists);

    /**
     * Gets the next upcoming reminder time efficiently.
     * Returns null if no future reminders exist.
     */
    java.time.LocalDateTime getNextReminderTime(java.util.Set<String> openedChecklists);

    /**
     * Gets all custom checklists.
     */
    java.util.Set<Checklist> getChecklists();

    /**
     * Adds a checklist to the persistent storage.
     */
    void addChecklist(Checklist checklist);

    /**
     * Removes a checklist from the persistent storage.
     */
    void removeChecklist(Checklist checklist);

    /**
     * Updates a checklist name.
     */
    void updateChecklistName(Checklist checklist, String newName);

    /**
     * Shuts down the repository and cleans up resources.
     */
    void shutdown();

    /**
     * Starts background services like automatic backups.
     * Should be called after the application is fully initialized.
     */
    default void start() {
        // Default implementation does nothing
    }
}
