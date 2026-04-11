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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Manages reminder data persistence and operations.
 */
public class ReminderManager {
    private final String reminderFileName;
    private final String taskFileName;
    private List<Reminder> cachedReminders;
    private boolean remindersDirty = true;
    private Component parentComponent;

    public ReminderManager(String reminderFileName, String taskFileName) {
        this.reminderFileName = reminderFileName;
        this.taskFileName = taskFileName;
    }

    /**
     * Sets the parent component for error dialogs.
     */
    public void setParentComponent(Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    /**
     * Gets all reminders, using cache if available.
     */
    public List<Reminder> getReminders() {
        if (cachedReminders != null && !remindersDirty) {
            return new ArrayList<>(cachedReminders);
        }

        List<Reminder> reminders = ReminderStore.loadFromProperties(reminderFileName, parentComponent);
        if (reminders.isEmpty()) {
            reminders = ReminderStore.loadFromXml(taskFileName, parentComponent);
            if (!reminders.isEmpty()) {
                ReminderStore.saveToProperties(reminders, reminderFileName);
            }
        }

        // Remove any checklist-level reminders for the built-in daily lists
        // These should not exist (only task-level reminders allowed for Morning/Evening)
        boolean cleaned = removeChecklistLevelDailyReminders(reminders);
        if (cleaned) {
            // Persist cleaned reminders back to properties so they don't reappear
            ReminderStore.saveToProperties(reminders, reminderFileName);
        }

        cachedReminders = new ArrayList<>(reminders);
        remindersDirty = false;
        return reminders;
    }

    

    

    /**
     * Saves reminders to the properties file.
     */
    private void saveRemindersToProperties(List<Reminder> reminders) {
        ReminderStore.saveToProperties(reminders, reminderFileName);
    }

    /**
     * Adds a reminder.
     */
    public void addReminder(Reminder reminder) {
        List<Reminder> reminders = getReminders();
        reminders.add(reminder);
        saveRemindersToProperties(reminders);
        cachedReminders = reminders;
        remindersDirty = false;
    }

    /**
     * Removes a reminder.
     */
    public void removeReminder(Reminder reminder) {
        List<Reminder> reminders = getReminders();
        reminders.removeIf(r -> {
                       if (!Objects.equals(r.getChecklistName(), reminder.getChecklistName())) return false;
                       if (r.isRecurring() && reminder.isRecurring()) {
                           return r.getDaysBitmask() == reminder.getDaysBitmask()
                                   && r.getHour() == reminder.getHour()
                                   && r.getMinute() == reminder.getMinute()
                                   && Objects.equals(r.getTaskId(), reminder.getTaskId());
                       } else {
                           return r.getYear() == reminder.getYear() &&
                                   r.getMonth() == reminder.getMonth() &&
                                   r.getDay() == reminder.getDay() &&
                                   r.getHour() == reminder.getHour() &&
                                   r.getMinute() == reminder.getMinute() &&
                                   Objects.equals(r.getTaskId(), reminder.getTaskId());
                       }
                   });
        saveRemindersToProperties(reminders);
        cachedReminders = reminders;
        remindersDirty = false;
    }

    private java.time.LocalDateTime computeNextOccurrence(Reminder r, java.time.LocalDateTime now) {
        if (r == null) return null;
        if (!r.isRecurring()) {
            try {
                return java.time.LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
            } catch (Exception e) {
                return null;
            }
        }
        // recurring: search next 7 days inclusive
        for (int offset = 0; offset < 7; offset++) {
            java.time.LocalDate date = now.toLocalDate().plusDays(offset);
            int dow = date.getDayOfWeek().getValue(); // 1=Mon..7=Sun
            if ((r.getDaysBitmask() & (1 << (dow - 1))) != 0) {
                java.time.LocalDateTime cand = date.atTime(r.getHour(), r.getMinute());
                if (cand.isAfter(now) || !cand.isAfter(now)) {
                    // return the first matching day (could be today even if past)
                    return cand;
                }
            }
        }
        return null;
    }

    /**
     * Gets reminders that are due within the next specified minutes.
     * More efficient than checking all reminders by filtering upfront.
     */
    public List<Reminder> getDueReminders(int minutesAhead, Set<String> openedChecklists) {
        List<Reminder> allReminders = getReminders();
        List<Reminder> dueReminders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Reminder r : allReminders) {
            LocalDateTime reminderTime = computeNextOccurrence(r, now);
            if (reminderTime == null) continue;
            // Show reminders that are:
            // 1. Due within the next minutesAhead minutes, OR
            // 2. Overdue but within the last hour (to avoid showing very old reminders)
            boolean isUpcoming = reminderTime.isAfter(now) && !reminderTime.isAfter(now.plusMinutes(minutesAhead));
            boolean isRecentlyOverdue = !reminderTime.isAfter(now) && reminderTime.isAfter(now.minusHours(1));

            if (isUpcoming || isRecentlyOverdue) {
                // Skip reminders for checklists that don't exist anymore
                String checklistName = r.getChecklistName();
                if (checklistName == null) continue;
                if (!checklistExists(checklistName)) continue;

                // Do not show checklist-level reminders for built-in daily lists (Morning/Evening)
                if (r.getTaskId() == null && ("MORNING".equalsIgnoreCase(checklistName) || "EVENING".equalsIgnoreCase(checklistName))) {
                    continue;
                }

                // Skip reminders for checklists that have been opened in this session, but not for recently overdue
                if (isRecentlyOverdue || (openedChecklists == null || !openedChecklists.contains(checklistName))) {
                    dueReminders.add(r);
                }
            }
        }
        return dueReminders;
    }

    /**
     * Gets the next reminder time for unopened checklists.
     */
    public LocalDateTime getNextReminderTime(Set<String> openedChecklists) {
        List<Reminder> allReminders = getReminders();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextTime = null;

        for (Reminder reminder : allReminders) {
            // Skip reminders for checklists that don't exist anymore
            String checklistName = reminder.getChecklistName();
            if (checklistName == null || !checklistExists(checklistName)) {
                continue;
            }

            // Ignore checklist-level reminders for built-in daily lists
            if (reminder.getTaskId() == null && ("MORNING".equalsIgnoreCase(checklistName) || "EVENING".equalsIgnoreCase(checklistName))) {
                continue;
            }

            // Skip reminders for checklists that are already opened
            if (openedChecklists != null && openedChecklists.contains(checklistName)) {
                continue;
            }

            LocalDateTime reminderTime = computeNextOccurrence(reminder, now);
            if (reminderTime == null) continue;
            if (reminderTime.isAfter(now)) {
                if (nextTime == null || reminderTime.isBefore(nextTime)) {
                    nextTime = reminderTime;
                }
            }
        }

        return nextTime;
    }

    /**
     * Checks if a checklist has any reminders.
     */
    public boolean hasReminders(String checklistName) {
        List<Reminder> reminders = getReminders();
        if (checklistName == null) return false;
        if (!checklistExists(checklistName)) return false;
        return reminders.stream()
                .filter(reminder -> Objects.equals(reminder.getChecklistName(), checklistName))
                .anyMatch(reminder -> {
                    // For built-in daily lists, only count task-level reminders
                    if ("MORNING".equalsIgnoreCase(checklistName) || "EVENING".equalsIgnoreCase(checklistName)) {
                        return reminder.getTaskId() != null;
                    }
                    return true;
                });
    }

    /**
     * Returns true if the given checklist name or id exists in the checklist-names properties
     * or is a built-in daily checklist name (MORNING/EVENING).
     */
    private boolean checklistExists(String checklistName) {
        if (checklistName == null) return false;
        // Built-in daily checklists
        if ("MORNING".equalsIgnoreCase(checklistName) || "EVENING".equalsIgnoreCase(checklistName)) return true;

        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(ApplicationConfiguration.CHECKLIST_NAMES_FILE_PATH), java.nio.charset.StandardCharsets.UTF_8)) {
            props.load(reader);
            for (String key : props.stringPropertyNames()) {
                String val = props.getProperty(key);
                if (checklistName.equals(key) || checklistName.equals(val)) return true;
            }
        } catch (IOException e) {
            // If file unreadable, assume no match
        }
        return false;
    }

    /**
     * Marks the reminder cache as dirty.
     */
    public void markDirty() {
        remindersDirty = true;
    }

    /**
     * Remove checklist-level reminders that target built-in daily lists (Morning/Evening).
     * Returns true if any reminders were removed.
     */
    private boolean removeChecklistLevelDailyReminders(List<Reminder> reminders) {
        if (reminders == null || reminders.isEmpty()) return false;
        boolean changed = reminders.removeIf(r -> r.getTaskId() == null && r.getChecklistName() != null &&
                ("MORNING".equalsIgnoreCase(r.getChecklistName()) || "EVENING".equalsIgnoreCase(r.getChecklistName())));
        if (changed) {
            // mark cache dirty when called directly
            remindersDirty = false; // we'll reset cache after saving
        }
        return changed;
    }
}