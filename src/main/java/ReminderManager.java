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

    public ReminderManager(String reminderFileName, String taskFileName) {
        this.reminderFileName = reminderFileName;
        this.taskFileName = taskFileName;
    }

    /**
     * Gets all reminders, using cache if available.
     */
    public List<Reminder> getReminders() {
        if (cachedReminders != null && !remindersDirty) {
            return new ArrayList<>(cachedReminders);
        }

        List<Reminder> reminders = loadRemindersFromProperties();
        if (reminders.isEmpty()) {
            reminders = loadRemindersFromXml();
            if (!reminders.isEmpty()) {
                saveRemindersToProperties(reminders);
            }
        }

        cachedReminders = new ArrayList<>(reminders);
        remindersDirty = false;
        return reminders;
    }

    /**
     * Loads reminders from the properties file.
     */
    private List<Reminder> loadRemindersFromProperties() {
        List<Reminder> reminders = new ArrayList<>();
        Properties props = new Properties();

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(reminderFileName), StandardCharsets.UTF_8)) {
            props.load(reader);

            int reminderCount = 0;
            boolean exceededLimit = false;

            for (String key : props.stringPropertyNames()) {
                if (reminderCount >= MemorySafetyManager.MAX_REMINDERS) {
                    exceededLimit = true;
                    break;
                }

                String value = props.getProperty(key);
                String[] parts = value.split(",");
                if (parts.length == 6) {
                    Reminder reminder = new Reminder(
                            parts[0], // checklistName
                            Integer.parseInt(parts[1]), // year
                            Integer.parseInt(parts[2]), // month
                            Integer.parseInt(parts[3]), // day
                            Integer.parseInt(parts[4]), // hour
                            Integer.parseInt(parts[5])  // minute
                    );
                    reminders.add(reminder);
                    reminderCount++;
                }
            }

            if (exceededLimit) {
                MemorySafetyManager.checkReminderLimit(MemorySafetyManager.MAX_REMINDERS + 1);
            }
        } catch (IOException e) {
            // File doesn't exist or can't be read
        }

        return reminders;
    }

    /**
     * Loads reminders from XML for backwards compatibility.
     */
    private List<Reminder> loadRemindersFromXml() {
        List<Reminder> reminders = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new java.io.File(taskFileName));
            NodeList nodeList = document.getElementsByTagName("reminder");

            boolean exceededLimit = false;

            for (int i = 0; i < nodeList.getLength() && i < MemorySafetyManager.MAX_REMINDERS; i++) {
                if (i >= MemorySafetyManager.MAX_REMINDERS) {
                    exceededLimit = true;
                    break;
                }

                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    Reminder reminder = new Reminder(
                            element.getAttribute("checklistName"),
                            Integer.parseInt(element.getAttribute("year")),
                            Integer.parseInt(element.getAttribute("month")),
                            Integer.parseInt(element.getAttribute("day")),
                            Integer.parseInt(element.getAttribute("hour")),
                            Integer.parseInt(element.getAttribute("minute"))
                    );
                    reminders.add(reminder);
                }
            }

            if (exceededLimit) {
                MemorySafetyManager.checkReminderLimit(MemorySafetyManager.MAX_REMINDERS + 1);
            }
        } catch (Exception e) {
            // Ignore errors for backwards compatibility
        }

        return reminders;
    }

    /**
     * Saves reminders to the properties file.
     */
    private void saveRemindersToProperties(List<Reminder> reminders) {
        Properties props = new Properties();
        for (int i = 0; i < reminders.size(); i++) {
            Reminder r = reminders.get(i);
            String key = "reminder." + i;
            String value = r.getChecklistName() + "," + r.getYear() + "," + r.getMonth() + "," +
                          r.getDay() + "," + r.getHour() + "," + r.getMinute();
            props.setProperty(key, value);
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(reminderFileName), StandardCharsets.UTF_8)) {
            props.store(writer, "Daily Checklist Reminders");
        } catch (IOException e) {
            // Ignore errors
        }
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
        reminders.removeIf(r -> Objects.equals(r.getChecklistName(), reminder.getChecklistName()) &&
                               r.getYear() == reminder.getYear() &&
                               r.getMonth() == reminder.getMonth() &&
                               r.getDay() == reminder.getDay() &&
                               r.getHour() == reminder.getHour() &&
                               r.getMinute() == reminder.getMinute());
        saveRemindersToProperties(reminders);
        cachedReminders = reminders;
        remindersDirty = false;
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
            LocalDateTime reminderTime = LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
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

            // Skip reminders for checklists that are already opened
            if (openedChecklists != null && openedChecklists.contains(checklistName)) {
                continue;
            }

            LocalDateTime reminderTime = LocalDateTime.of(
                    reminder.getYear(),
                    reminder.getMonth(),
                    reminder.getDay(),
                    reminder.getHour(),
                    reminder.getMinute()
            );

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
        return reminders.stream().anyMatch(reminder -> Objects.equals(reminder.getChecklistName(), checklistName));
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
}