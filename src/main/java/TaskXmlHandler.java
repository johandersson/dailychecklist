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
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/**
 * Handles XML operations for task data persistence.
 */
public class TaskXmlHandler {
    private final String fileName;
    private final DocumentBuilderFactory documentBuilderFactory;
    private final TransformerFactory transformerFactory;
    private final Map<String, java.util.Date> dateCache = new HashMap<>();

    public TaskXmlHandler(String fileName) {
        this.fileName = fileName;
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.transformerFactory = TransformerFactory.newInstance();
    }

    /**
     * Gets the parsed Date for a date string, using cache to avoid repeated parsing.
     */
    private java.util.Date getParsedDate(String dateStr) throws ParseException {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        if (!dateCache.containsKey(dateStr)) {
            dateCache.put(dateStr, new SimpleDateFormat("yyyy-MM-dd").parse(dateStr));
        }
        return dateCache.get(dateStr);
    }

    /**
     * Reads and parses the XML document.
     */
    public Document readDocument() throws ParserConfigurationException, SAXException, IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            return createNewDocument();
        }

        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        return builder.parse(file);
    }

    /**
     * Creates a new empty XML document with root element.
     */
    private Document createNewDocument() throws ParserConfigurationException {
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element root = document.createElement("tasks");
        document.appendChild(root);
        return document;
    }

    /**
     * Writes the document to the XML file.
     */
    public void writeDocument(Document document) throws TransformerException {
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty("encoding", "UTF-8");
        transformer.setOutputProperty("indent", "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(document);
        // Write via a temp file then move into place. Retry briefly on transient IO errors
        // (file-locks are common on Windows when another process touches the file).
        java.nio.file.Path target = java.nio.file.Paths.get(fileName);
        java.nio.file.Path parent = target.toAbsolutePath().getParent();
        if (parent == null) parent = java.nio.file.Paths.get(".");
        java.nio.file.Path temp = parent.resolve(target.getFileName().toString() + ".tmp");

        int attempts = 0;
        int maxAttempts = 5;
        long backoffMs = 50;
        while (true) {
            try {
                try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(temp.toFile()), "UTF-8")) {
                    StreamResult result = new StreamResult(writer);
                    transformer.transform(source, result);
                }
                // Move temp into final location, replacing existing file
                java.nio.file.Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                return;
            } catch (java.io.IOException e) {
                attempts++;
                // If temp exists, try to delete it to avoid stale files
                try { java.nio.file.Files.deleteIfExists(temp); } catch (Exception ex) { /* ignore */ }
                if (attempts >= maxAttempts) {
                    throw new TransformerException("Failed to write XML file: " + e.getMessage(), e);
                }
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new TransformerException("Interrupted while writing XML file", ie);
                }
                backoffMs *= 2;
            }
        }
    }

    /**
     * Ensure the XML data file exists. Creates parent directories and an empty document if missing.
     */
    public void ensureFileExists() throws ParserConfigurationException, TransformerException, IOException {
        File file = new File(fileName);
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists()) {
            Document doc = createNewDocument();
            writeDocument(doc);
        }
    }

    /**
     * Parses all tasks from the XML document.
     */
    public List<Task> parseAllTasks() throws ParserConfigurationException, SAXException, IOException {
        List<Task> tasks = new ArrayList<>();
        Document document = readDocument();
        NodeList nodeList = document.getElementsByTagName("task");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                try {
                    Task task = parseTaskFromElement((Element) node);
                    if (validateTask(task)) {
                        tasks.add(task);
                    } else {
                        // Log invalid task but continue
                        System.err.println("Skipping invalid task in XML: " + task);
                    }
                } catch (Exception e) {
                    // Log parsing error but continue with other tasks
                    System.err.println("Error parsing task element: " + e.getMessage());
                }
            }
        }

        return tasks;
    }

    /**
     * Parses a single task from an XML element.
     */
    private Task parseTaskFromElement(Element element) {
        // Get required attributes and elements with null checks
        String id = element.getAttribute("id");
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Task ID is missing or empty");
        }

        NodeList nameNodes = element.getElementsByTagName("name");
        if (nameNodes.getLength() == 0) {
            throw new IllegalArgumentException("Task name is missing");
        }
        String name = nameNodes.item(0).getTextContent();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Task name is empty");
        }

        NodeList typeNodes = element.getElementsByTagName("type");
        if (typeNodes.getLength() == 0) {
            throw new IllegalArgumentException("Task type is missing");
        }
        TaskType type;
        try {
            type = TaskType.valueOf(typeNodes.item(0).getTextContent());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid task type: " + typeNodes.item(0).getTextContent());
        }

        String checklistId = null;
        NodeList checklistIdNodes = element.getElementsByTagName("checklistId");
        if (checklistIdNodes.getLength() > 0) {
            String content = checklistIdNodes.item(0).getTextContent();
            if (content != null && !content.trim().isEmpty()) {
                checklistId = content.trim();
            }
        }

        String parentId = null;
        NodeList parentIdNodes = element.getElementsByTagName("parentId");
        if (parentIdNodes.getLength() > 0) {
            String content = parentIdNodes.item(0).getTextContent();
            if (content != null && !content.trim().isEmpty()) {
                parentId = content.trim();
            }
        }

        String weekday = null;
        NodeList weekdayNodes = element.getElementsByTagName("weekday");
        if (weekdayNodes.getLength() > 0) {
            weekday = weekdayNodes.item(0).getTextContent();
            if (weekday != null && weekday.trim().isEmpty()) {
                weekday = null;
            }
        }

        NodeList doneNodes = element.getElementsByTagName("done");
        boolean done = doneNodes.getLength() > 0 && Boolean.parseBoolean(doneNodes.item(0).getTextContent());

        NodeList doneDateNodes = element.getElementsByTagName("doneDate");
        String doneDate = doneDateNodes.getLength() > 0 ? doneDateNodes.item(0).getTextContent() : null;

        // Parse note if present
        String note = null;
        NodeList noteNodes = element.getElementsByTagName("note");
        if (noteNodes.getLength() > 0) {
            note = noteNodes.item(0).getTextContent();
            if (note != null && note.trim().isEmpty()) {
                note = null;
            }
        }

        // Use new constructor with parentId for backwards compatibility
        Task task = new Task(id, name, type, weekday, done, doneDate, checklistId, parentId);
        task.setNote(note);
        return task;
    }

    /**
     * Creates an XML element for a task.
     */
    public Element createTaskElement(Document document, Task task) {
        Element taskElement = document.createElement("task");
        taskElement.setAttribute("id", task.getId());

        Element nameElement = document.createElement("name");
        nameElement.setTextContent(task.getName());
        taskElement.appendChild(nameElement);

        Element typeElement = document.createElement("type");
        typeElement.setTextContent(task.getType().name());
        taskElement.appendChild(typeElement);

        if (task.getWeekday() != null) {
            Element weekdayElement = document.createElement("weekday");
            weekdayElement.setTextContent(task.getWeekday());
            taskElement.appendChild(weekdayElement);
        }

        Element doneElement = document.createElement("done");
        doneElement.setTextContent(String.valueOf(task.isDone()));
        taskElement.appendChild(doneElement);

        Element doneDateElement = document.createElement("doneDate");
        doneDateElement.setTextContent(task.getDoneDate());
        taskElement.appendChild(doneDateElement);

        if (task.getChecklistId() != null && !task.getChecklistId().trim().isEmpty()) {
            Element checklistIdElement = document.createElement("checklistId");
            checklistIdElement.setTextContent(task.getChecklistId().trim());
            taskElement.appendChild(checklistIdElement);
        }

        if (task.getParentId() != null && !task.getParentId().trim().isEmpty()) {
            Element parentIdElement = document.createElement("parentId");
            parentIdElement.setTextContent(task.getParentId().trim());
            taskElement.appendChild(parentIdElement);
        }

        if (task.getNote() != null && !task.getNote().trim().isEmpty()) {
            Element noteElement = document.createElement("note");
            noteElement.setTextContent(task.getNote());
            taskElement.appendChild(noteElement);
        }

        return taskElement;
    }

    /**
     * Adds a task to the XML document.
     */
    public void addTask(Task task) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        Document document = readDocument();
        Element root = document.getDocumentElement();
        Element taskElement = createTaskElement(document, task);
        root.appendChild(taskElement);
        writeDocument(document);
    }

    /**
     * Updates a task in the XML document.
     */
    public void updateTask(Task task) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        Document document = readDocument();
        NodeList nodeList = document.getElementsByTagName("task");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (element.getAttribute("id").equals(task.getId())) {
                    // Update existing task element
                    updateTaskElement(element, task);
                    writeDocument(document);
                    return;
                }
            }
        }
    }

    /**
     * Updates an existing task XML element with new task data.
     */
    private void updateTaskElement(Element taskElement, Task task) {
                // Update or create parentId element
                NodeList parentIdNodes = taskElement.getElementsByTagName("parentId");
                if (task.getParentId() != null && !task.getParentId().trim().isEmpty()) {
                    Element parentIdElement;
                    if (parentIdNodes.getLength() > 0) {
                        parentIdElement = (Element) parentIdNodes.item(0);
                    } else {
                        parentIdElement = taskElement.getOwnerDocument().createElement("parentId");
                        taskElement.appendChild(parentIdElement);
                    }
                    parentIdElement.setTextContent(task.getParentId().trim());
                } else if (parentIdNodes.getLength() > 0) {
                    taskElement.removeChild(parentIdNodes.item(0));
                }
        Document document = taskElement.getOwnerDocument();

        // Update or create name element
        updateOrCreateElement(taskElement, "name", task.getName());

        // Update or create type element
        updateOrCreateElement(taskElement, "type", task.getType().name());

        // Update or create done element
        updateOrCreateElement(taskElement, "done", String.valueOf(task.isDone()));

        // Update or create doneDate element
        updateOrCreateElement(taskElement, "doneDate", task.getDoneDate());

        // Handle weekday (optional)
        NodeList weekdayNodes = taskElement.getElementsByTagName("weekday");
        if (task.getWeekday() != null) {
            Element weekdayElement;
            if (weekdayNodes.getLength() > 0) {
                weekdayElement = (Element) weekdayNodes.item(0);
            } else {
                weekdayElement = document.createElement("weekday");
                taskElement.appendChild(weekdayElement);
            }
            weekdayElement.setTextContent(task.getWeekday());
        } else if (weekdayNodes.getLength() > 0) {
            taskElement.removeChild(weekdayNodes.item(0));
        }

        // Handle checklist id
        NodeList checklistIdNodes = taskElement.getElementsByTagName("checklistId");
        if (task.getChecklistId() != null) {
            Element checklistIdElement;
            if (checklistIdNodes.getLength() > 0) {
                checklistIdElement = (Element) checklistIdNodes.item(0);
            } else {
                checklistIdElement = document.createElement("checklistId");
                taskElement.appendChild(checklistIdElement);
            }
            checklistIdElement.setTextContent(task.getChecklistId());
        } else if (checklistIdNodes.getLength() > 0) {
            taskElement.removeChild(checklistIdNodes.item(0));
        }

        // Handle note (optional)
        NodeList noteNodes = taskElement.getElementsByTagName("note");
        if (task.getNote() != null && !task.getNote().trim().isEmpty()) {
            Element noteElement;
            if (noteNodes.getLength() > 0) {
                noteElement = (Element) noteNodes.item(0);
            } else {
                noteElement = document.createElement("note");
                taskElement.appendChild(noteElement);
            }
            noteElement.setTextContent(task.getNote());
        } else if (noteNodes.getLength() > 0) {
            taskElement.removeChild(noteNodes.item(0));
        }
    }

    /**
     * Updates an existing element or creates it if it doesn't exist.
     */
    private void updateOrCreateElement(Element parent, String tagName, String textContent) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        Element element;
        if (nodes.getLength() > 0) {
            element = (Element) nodes.item(0);
        } else {
            element = parent.getOwnerDocument().createElement(tagName);
            parent.appendChild(element);
        }
        element.setTextContent(textContent);
    }

    /**
     * Removes a task from the XML document.
     */
    public void removeTask(Task task) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        Document document = readDocument();
        NodeList nodeList = document.getElementsByTagName("task");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (element.getAttribute("id").equals(task.getId())) {
                    element.getParentNode().removeChild(element);
                    writeDocument(document);
                    return;
                }
            }
        }
    }

    /**
     * Replaces all tasks in the XML document.
     */
    public void setAllTasks(List<Task> tasks) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        // Validate all tasks before saving
        for (Task task : tasks) {
            if (!validateTask(task)) {
                throw new IllegalArgumentException("Invalid task: " + task);
            }
        }

        Document document = readDocument();
        Element root = document.getDocumentElement();

        // Remove all existing task elements
        NodeList existingTasks = root.getElementsByTagName("task");
        for (int i = existingTasks.getLength() - 1; i >= 0; i--) {
            root.removeChild(existingTasks.item(i));
        }

        // Add all new tasks
        for (Task task : tasks) {
            Element taskElement = createTaskElement(document, task);
            root.appendChild(taskElement);
        }

        writeDocument(document);

    }

    /**
     * Update multiple tasks in the document and write once.
     */
    public void updateTasks(List<Task> tasks) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        if (tasks == null || tasks.isEmpty()) return;
        Document document = readDocument();
        Element root = document.getDocumentElement();
        NodeList nodeList = document.getElementsByTagName("task");

        // Build a map of existing elements by id for quick lookup
        Map<String, Element> existing = new HashMap<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                String id = el.getAttribute("id");
                if (id != null && !id.trim().isEmpty()) existing.put(id, el);
            }
        }

        for (Task task : tasks) {
            Element element = existing.get(task.getId());
            if (element != null) {
                updateTaskElement(element, task);
            } else {
                Element taskElement = createTaskElement(document, task);
                root.appendChild(taskElement);
            }
        }

        writeDocument(document);
    }

    /**
     * Checks if a task's done date is in the past and resets it if needed.
     */
    public void checkAndResetPastDoneDate(Task task, String today) throws ParseException {
        if (task.isDone() && task.getDoneDate() != null) {
            java.util.Date doneDate = task.getParsedDoneDate(); // Use lazy parsing from Task
            if (doneDate == null) return; // Skip if parsing failed
            java.util.Date todayDate = getParsedDate(today); // Still need to parse today
            if (doneDate.before(todayDate)) {
                task.setDone(false);
                task.setDoneDate(null);
            }
        }
    }

    /**
     * Validates a task before saving.
     */
    public static boolean validateTask(Task task) {
        if (task == null) return false;
        if (task.getId() == null || task.getId().trim().isEmpty()) return false;
        if (task.getName() == null || task.getName().trim().isEmpty()) return false;
        if (task.getType() == null) return false;
        // Checklist ID can be null for daily tasks
        if (task.getChecklistId() != null && task.getChecklistId().trim().isEmpty()) {
            task.setChecklistId(null); // Normalize empty to null
        }
        return true;
    }
}