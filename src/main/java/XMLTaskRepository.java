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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLTaskRepository implements TaskRepository {
    private static String FILE_NAME = System.getProperty("user.home") + File.separator + "dailychecklist-tasks.xml";
    private static String REMINDER_FILE_NAME = System.getProperty("user.home") + File.separator + "dailychecklist-reminders.properties";

    // Caching
    private List<Task> cachedTasks = null;
    private List<Reminder> cachedReminders = null;
    private boolean tasksDirty = false;
    private boolean remindersDirty = false;

    @Override
    public void initialize() {
        // Create the XML file if it doesn't exist
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            try {
                Document document = createDocument();
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                DOMSource source = new DOMSource(document);
                StreamResult result = new StreamResult(file);
                transformer.transform(source, result);
            } catch (ParserConfigurationException | TransformerException e) {
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(null, "Failed to initialize task file: " + e.getMessage(), "Initialization Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    @Override
    public List<Task> getDailyTasks() {
        List<Task> tasks = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(System.currentTimeMillis()));
        try {
            Document document = readDocument();
            NodeList nodeList = document.getElementsByTagName("task");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String checklistName = null;
                    NodeList checklistNameNodes = element.getElementsByTagName("checklistName");
                    if (checklistNameNodes.getLength() > 0) {
                        checklistName = checklistNameNodes.item(0).getTextContent();
                    }
                    Task task = new Task(
                            element.getAttribute("id"),
                            element.getElementsByTagName("name").item(0).getTextContent(),
                            TaskType.valueOf(element.getElementsByTagName("type").item(0).getTextContent()),
                            null,
                            Boolean.parseBoolean(element.getElementsByTagName("done").item(0).getTextContent()),
                            element.getElementsByTagName("doneDate").item(0).getTextContent(),
                            checklistName
                    );
                    checkIfDoneDateIsInThePast(task, today);
                    tasks.add(task);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to load daily tasks: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return tasks;
    }

    private void checkIfDoneDateIsInThePast(Task task, String today) {
        if (task.isDone() && task.getDoneDate() != null) {
            try {
                java.util.Date doneDate = new SimpleDateFormat("yyyy-MM-dd").parse(task.getDoneDate());
                java.util.Date todayDate = new SimpleDateFormat("yyyy-MM-dd").parse(today);
                if (doneDate.before(todayDate)) {
                    task.setDone(false);
                    task.setDoneDate(null);
                    updateTask(task);
                }
            } catch (ParseException e) {
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(null, "Failed to parse date: " + e.getMessage(), "Date Parse Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    @Override
    public List<Task> getAllTasks() {
        if (cachedTasks != null && !tasksDirty) {
            return new ArrayList<>(cachedTasks);
        }

        List<Task> tasks = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(System.currentTimeMillis()));
        try {
            Document document = readDocument();
            NodeList nodeList = document.getElementsByTagName("task");

            // Memory safety check: prevent loading extremely large datasets
            if (MemorySafetyManager.checkTaskLimit(nodeList.getLength())) {
                // Load only the first MAX_TASKS tasks
                for (int i = 0; i < MemorySafetyManager.MAX_TASKS && i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        String weekday = element.getElementsByTagName("weekday").item(0) != null ? element.getElementsByTagName("weekday").item(0).getTextContent() : null;
                        if (weekday != null && weekday.isEmpty()) weekday = null;
                        if (weekday != null) weekday = weekday.toLowerCase();
                        Task task = new Task(
                                element.getAttribute("id"),
                                element.getElementsByTagName("name").item(0).getTextContent(),
                                TaskType.valueOf(element.getElementsByTagName("type").item(0).getTextContent()),
                                weekday,
                                element.getElementsByTagName("done").item(0) != null ? Boolean.parseBoolean(element.getElementsByTagName("done").item(0).getTextContent()) : false,
                                element.getElementsByTagName("doneDate").item(0) != null ? element.getElementsByTagName("doneDate").item(0).getTextContent() : null,
                                element.getElementsByTagName("checklistName").item(0) != null ? element.getElementsByTagName("checklistName").item(0).getTextContent() : null
                        );
                        checkIfDoneDateIsInThePast(task, today);
                        tasks.add(task);
                    }
                }
            } else {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        String weekday = element.getElementsByTagName("weekday").item(0) != null ? element.getElementsByTagName("weekday").item(0).getTextContent() : null;
                        if (weekday != null && weekday.isEmpty()) weekday = null;
                        if (weekday != null) weekday = weekday.toLowerCase();
                        Task task = new Task(
                                element.getAttribute("id"),
                                element.getElementsByTagName("name").item(0).getTextContent(),
                                TaskType.valueOf(element.getElementsByTagName("type").item(0).getTextContent()),
                                weekday,
                                element.getElementsByTagName("done").item(0) != null ? Boolean.parseBoolean(element.getElementsByTagName("done").item(0).getTextContent()) : false,
                                element.getElementsByTagName("doneDate").item(0) != null ? element.getElementsByTagName("doneDate").item(0).getTextContent() : null,
                                element.getElementsByTagName("checklistName").item(0) != null ? element.getElementsByTagName("checklistName").item(0).getTextContent() : null
                        );
                        checkIfDoneDateIsInThePast(task, today);
                        tasks.add(task);
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to load all tasks: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        cachedTasks = new ArrayList<>(tasks);
        tasksDirty = false;
        return tasks;
    }

    @Override
    public void addTask(Task task) {
        try {
            Document document = readDocument();
            Element root = document.getDocumentElement();
            Element taskElement = document.createElement("task");
            taskElement.setAttribute("id", task.getId());
            Element nameElement = document.createElement("name");
            nameElement.setTextContent(task.getName());
            taskElement.appendChild(nameElement);
            Element typeElement = document.createElement("type");
            typeElement.setTextContent(task.getType().name());
            taskElement.appendChild(typeElement);
            Element weekdayElement = document.createElement("weekday");
            weekdayElement.setTextContent(task.getWeekday());
            taskElement.appendChild(weekdayElement);
            Element doneElement = document.createElement("done");
            doneElement.setTextContent(String.valueOf(task.isDone()));
            taskElement.appendChild(doneElement);
            Element doneDateElement = document.createElement("doneDate");
            doneDateElement.setTextContent(task.getDoneDate());
            taskElement.appendChild(doneDateElement);
            if (task.getChecklistName() != null) {
                Element checklistNameElement = document.createElement("checklistName");
                checklistNameElement.setTextContent(task.getChecklistName());
                taskElement.appendChild(checklistNameElement);
            }
            root.appendChild(taskElement);
            writeDocument(document);
            tasksDirty = true; // Mark cache as dirty
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to add task: " + e.getMessage(), "Add Task Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void updateTask(Task task) {
        try {
            Document document = readDocument();
            NodeList nodeList = document.getElementsByTagName("task");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    if (element.getAttribute("id").equals(task.getId())) {
                        element.getElementsByTagName("name").item(0).setTextContent(task.getName());
                        element.getElementsByTagName("type").item(0).setTextContent(task.getType().name());
                        element.getElementsByTagName("weekday").item(0).setTextContent(task.getWeekday());
                        element.getElementsByTagName("done").item(0).setTextContent(String.valueOf(task.isDone()));
                        element.getElementsByTagName("doneDate").item(0).setTextContent(task.getDoneDate());
                        // Handle checklistName
                        NodeList checklistNodes = element.getElementsByTagName("checklistName");
                        if (checklistNodes.getLength() > 0) {
                            element.removeChild(checklistNodes.item(0));
                        }
                        if (task.getChecklistName() != null) {
                            Element checklistNameElement = document.createElement("checklistName");
                            checklistNameElement.setTextContent(task.getChecklistName());
                            element.appendChild(checklistNameElement);
                        }
                        writeDocument(document);
                        tasksDirty = true; // Mark cache as dirty
                        return;
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to update task: " + e.getMessage(), "Update Task Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void removeTask(Task task) {
        try {
            Document document = readDocument();
            NodeList nodeList = document.getElementsByTagName("task");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    if (element.getAttribute("id").equals(task.getId())) {
                        node.getParentNode().removeChild(node);
                        writeDocument(document);
                        tasksDirty = true; // Mark cache as dirty
                        return;
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to remove task: " + e.getMessage(), "Remove Task Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public boolean hasUndoneTasks() {
        try {
            Document document = readDocument();
            NodeList nodeList = document.getElementsByTagName("task");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    if (!Boolean.parseBoolean(element.getElementsByTagName("done").item(0).getTextContent())) {
                        return true;
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to check for undone tasks: " + e.getMessage(), "Check Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    @Override
    public void setTasks(List<Task> tasks) {
        try {
            Document document = createDocument();
            Element root = document.getDocumentElement();
            for (Task task : tasks) {
                Element taskElement = document.createElement("task");
                taskElement.setAttribute("id", task.getId());
                Element nameElement = document.createElement("name");
                nameElement.setTextContent(task.getName());
                taskElement.appendChild(nameElement);
                Element typeElement = document.createElement("type");
                typeElement.setTextContent(task.getType().name());
                taskElement.appendChild(typeElement);
                Element weekdayElement = document.createElement("weekday");
                weekdayElement.setTextContent(task.getWeekday());
                taskElement.appendChild(weekdayElement);
                Element doneElement = document.createElement("done");
                doneElement.setTextContent(String.valueOf(task.isDone()));
                taskElement.appendChild(doneElement);
                Element doneDateElement = document.createElement("doneDate");
                doneDateElement.setTextContent(task.getDoneDate());
                taskElement.appendChild(doneDateElement);
                root.appendChild(taskElement);
            }
            writeDocument(document);
            cachedTasks = new ArrayList<>(tasks); // Update cache
            tasksDirty = false;
        } catch (ParserConfigurationException | TransformerException e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to set tasks: " + e.getMessage(), "Set Tasks Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Document createDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element root = document.createElement("tasks");
        document.appendChild(root);
        return document;
    }

    private Document readDocument() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Secure XML processing - prevent XXE attacks
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new File(FILE_NAME));
    }

    private void writeDocument(Document document) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        // Secure XSLT processing - prevent XXE attacks
        factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
        Transformer transformer = factory.newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(new File(FILE_NAME));
        transformer.transform(source, result);
    }

    @Override
    public List<Reminder> getReminders() {
        if (cachedReminders != null && !remindersDirty) {
            return new ArrayList<>(cachedReminders);
        }

        List<Reminder> reminders = new ArrayList<>();
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(REMINDER_FILE_NAME)) {
            props.load(fis);

            // Memory safety check: prevent loading extremely large reminder datasets
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
                MemorySafetyManager.checkReminderLimit(MemorySafetyManager.MAX_REMINDERS + 1); // Trigger warning
            }
        } catch (IOException e) {
            // Try to load from XML for backwards compatibility
            try {
                Document document = readDocument();
                NodeList nodeList = document.getElementsByTagName("reminder");

                // Memory safety check for XML reminders too
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
                    MemorySafetyManager.checkReminderLimit(MemorySafetyManager.MAX_REMINDERS + 1); // Trigger warning
                }

                // Migrate to Properties file
                saveRemindersToProperties(reminders);
            } catch (Exception ex) {
                // Ignore errors for backwards compatibility
            }
        }

        cachedReminders = new ArrayList<>(reminders);
        remindersDirty = false;
        return reminders;
    }

    private void saveRemindersToProperties(List<Reminder> reminders) {
        Properties props = new Properties();
        for (int i = 0; i < reminders.size(); i++) {
            Reminder r = reminders.get(i);
            String key = "reminder." + i;
            String value = r.getChecklistName() + "," + r.getYear() + "," + r.getMonth() + "," +
                          r.getDay() + "," + r.getHour() + "," + r.getMinute();
            props.setProperty(key, value);
        }
        try (FileOutputStream fos = new FileOutputStream(REMINDER_FILE_NAME)) {
            props.store(fos, "Daily Checklist Reminders");
        } catch (IOException e) {
            // Ignore errors
        }
    }

    @Override
    public void addReminder(Reminder reminder) {
        List<Reminder> reminders = getReminders();
        reminders.add(reminder);
        saveRemindersToProperties(reminders);
        cachedReminders = reminders;
        remindersDirty = false;
    }

    @Override
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
    public List<Reminder> getDueReminders(int minutesAhead, java.util.Set<String> openedChecklists) {
        List<Reminder> allReminders = getReminders();
        List<Reminder> dueReminders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Reminder r : allReminders) {
            // Skip reminders for checklists that have been opened in this session
            String checklistName = r.getChecklistName();
            if (checklistName != null && openedChecklists != null && openedChecklists.contains(checklistName)) {
                continue;
            }

            LocalDateTime reminderTime = LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
            // Show reminders that are:
            // 1. Due within the next minutesAhead minutes, OR
            // 2. Overdue but within the last hour (to avoid showing very old reminders)
            boolean isUpcoming = reminderTime.isAfter(now) && !reminderTime.isAfter(now.plusMinutes(minutesAhead));
            boolean isRecentlyOverdue = !reminderTime.isAfter(now) && reminderTime.isAfter(now.minusHours(1));

            if (isUpcoming || isRecentlyOverdue) {
                dueReminders.add(r);
            }
        }
        return dueReminders;
    }

    /**
     * Gets the next upcoming reminder time efficiently.
     * Returns null if no future reminders exist.
     */
    public LocalDateTime getNextReminderTime(java.util.Set<String> openedChecklists) {
        List<Reminder> reminders = getReminders();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextTime = null;

        for (Reminder r : reminders) {
            // Skip reminders for checklists that have been opened in this session
            String checklistName = r.getChecklistName();
            if (checklistName != null && openedChecklists != null && openedChecklists.contains(checklistName)) {
                continue;
            }

            LocalDateTime reminderTime = LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
            if (reminderTime.isAfter(now) && (nextTime == null || reminderTime.isBefore(nextTime))) {
                nextTime = reminderTime;
            }
        }
        return nextTime;
    }
}