import java.awt.Component;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Responsible for loading and saving reminders from/to persistent stores.
 * Separated from `ReminderManager` to improve single-responsibility and testability.
 */
public final class ReminderStore {
    private ReminderStore() {}

    public static List<Reminder> loadFromProperties(String reminderFileName, Component parentComponent) {
        List<Reminder> reminders = new ArrayList<>();
        Properties props = new Properties();
        boolean hasOldFormat = false;

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

                // Try pipe delimiter first (new format), fall back to comma (old format)
                String[] parts = value.split("\\|");
                if (parts.length < 6) {
                    // Fall back to comma delimiter for backwards compatibility
                    parts = value.split(",");
                    hasOldFormat = true;
                }

                if (parts.length >= 6) {
                    try {
                        String checklistName = parts[0].trim();
                        int year = Integer.parseInt(parts[1].trim());
                        int month = Integer.parseInt(parts[2].trim());
                        int day = Integer.parseInt(parts[3].trim());
                        int hour = Integer.parseInt(parts[4].trim());
                        int minute = Integer.parseInt(parts[5].trim());
                        String taskId = null;
                        int daysBitmask = 0;

                        // Additional optional key=value tokens may follow (e.g. taskId=..., days=1,3,5)
                        if (parts.length >= 7) {
                            for (int p = 6; p < parts.length; p++) {
                                String token = parts[p].trim();
                                if (token.startsWith("taskId=")) {
                                    taskId = token.substring("taskId=".length()).trim();
                                } else if (token.startsWith("days=")) {
                                    String list = token.substring("days=".length()).trim();
                                    // parse comma separated 1..7 values (1=Mon)
                                    String[] ds = list.split(",");
                                    for (String d : ds) {
                                        try {
                                            int val = Integer.parseInt(d.trim());
                                            if (val >= 1 && val <= 7) {
                                                daysBitmask |= 1 << (val - 1);
                                            }
                                        } catch (NumberFormatException ignore) {}
                                    }
                                } else if (!token.isEmpty()) {
                                    // older format: single taskId without key
                                    if (taskId == null) taskId = token;
                                }
                            }
                        }

                        Reminder reminder;
                        if (daysBitmask != 0) {
                            // recurring reminder
                            reminder = new Reminder(checklistName, daysBitmask, hour, minute, taskId);
                        } else {
                            reminder = new Reminder(checklistName, year, month, day, hour, minute, taskId);
                        }
                        reminders.add(reminder);
                        reminderCount++;
                    } catch (NumberFormatException e) {
                        // Show error for this specific reminder but continue loading others
                        final String errorMsg = "Invalid reminder data in properties file (key: " + key + ", value: " + value + ")\\n\\n" +
                                "This reminder will be skipped. If the reminder name contains commas, it will be fixed when you next add or remove a reminder.";
                        if (parentComponent != null) {
                            javax.swing.SwingUtilities.invokeLater(() -> ErrorDialog.showError(parentComponent, errorMsg, e));
                        }
                    }
                }
            }

            if (exceededLimit) {
                MemorySafetyManager.checkReminderLimit(MemorySafetyManager.MAX_REMINDERS + 1);
            }

            // If we loaded any old format data successfully, re-save in new format
            if (hasOldFormat && !reminders.isEmpty()) {
                saveToProperties(reminders, reminderFileName);
            }
        } catch (IOException e) {
            // File doesn't exist or can't be read
        }

        return reminders;
    }

    public static List<Reminder> loadFromXml(String taskFileName, Component parentComponent) {
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
                    try {
                        String checklistName = element.getAttribute("checklistName").trim();
                        int year = Integer.parseInt(element.getAttribute("year").trim());
                        int month = Integer.parseInt(element.getAttribute("month").trim());
                        int day = Integer.parseInt(element.getAttribute("day").trim());
                        int hour = Integer.parseInt(element.getAttribute("hour").trim());
                        int minute = Integer.parseInt(element.getAttribute("minute").trim());
                        String taskId = null;
                        int daysBitmask = 0;
                        if (element.hasAttribute("taskId")) {
                            taskId = element.getAttribute("taskId").trim();
                        }
                        if (element.hasAttribute("days")) {
                            String list = element.getAttribute("days").trim();
                            String[] ds = list.split(",");
                            for (String d : ds) {
                                try {
                                    int val = Integer.parseInt(d.trim());
                                    if (val >= 1 && val <= 7) daysBitmask |= 1 << (val - 1);
                                } catch (NumberFormatException ignore) {}
                            }
                        }
                        Reminder reminder = (daysBitmask != 0)
                                ? new Reminder(checklistName, daysBitmask, hour, minute, taskId)
                                : new Reminder(checklistName, year, month, day, hour, minute, taskId);
                        reminders.add(reminder);
                    } catch (NumberFormatException e) {
                        // Show error for this specific reminder but continue loading others
                        final String errorMsg = "Invalid reminder data in XML file (reminder #" + i + ")";
                        if (parentComponent != null) {
                            javax.swing.SwingUtilities.invokeLater(() -> ErrorDialog.showError(parentComponent, errorMsg, e));
                        }
                    }
                }
            }

            if (exceededLimit) {
                MemorySafetyManager.checkReminderLimit(MemorySafetyManager.MAX_REMINDERS + 1);
            }
        } catch (NumberFormatException e) {
            // NumberFormatException is handled above for individual reminders
            throw e;
        } catch (Exception e) {
            // Show error dialog for other XML parsing errors
            final String errorMsg = "Failed to load reminders from XML file";
            if (parentComponent != null) {
                javax.swing.SwingUtilities.invokeLater(() -> ErrorDialog.showError(parentComponent, errorMsg, e));
            }
        }

        return reminders;
    }

    public static void saveToProperties(List<Reminder> reminders, String reminderFileName) {
        Properties props = new Properties();
        for (int i = 0; i < reminders.size(); i++) {
            Reminder r = reminders.get(i);
            String key = "reminder." + i;
            StringBuilder sb = new StringBuilder();
            sb.append(r.getChecklistName()).append('|').append(r.getYear()).append('|').append(r.getMonth()).append('|')
                .append(r.getDay()).append('|').append(r.getHour()).append('|').append(r.getMinute());
            // New format uses key=value tokens for optional data so parsing is unambiguous
            if (r.getTaskId() != null) {
                sb.append('|').append("taskId=").append(r.getTaskId());
            }
            if (r.isRecurring()) {
                // convert bitmask to 1..7 comma list
                StringBuilder days = new StringBuilder();
                for (int d = 1; d <= 7; d++) {
                    if ((r.getDaysBitmask() & (1 << (d - 1))) != 0) {
                        if (days.length() > 0) days.append(',');
                        days.append(d);
                    }
                }
                if (days.length() > 0) {
                    sb.append('|').append("days=").append(days.toString());
                }
            }
            String value = sb.toString();
            props.setProperty(key, value);
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(reminderFileName), StandardCharsets.UTF_8)) {
            props.store(writer, "Daily Checklist Reminders");
        } catch (IOException e) {
            // Ignore errors
        }
    }

}
