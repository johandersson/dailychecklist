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
        StreamResult result = new StreamResult(new File(fileName));
        transformer.transform(source, result);
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
                Task task = parseTaskFromElement((Element) node);
                tasks.add(task);
            }
        }

        return tasks;
    }

    /**
     * Parses a single task from an XML element.
     */
    private Task parseTaskFromElement(Element element) {
        String checklistName = null;
        NodeList checklistNameNodes = element.getElementsByTagName("checklistName");
        if (checklistNameNodes.getLength() > 0) {
            String content = checklistNameNodes.item(0).getTextContent();
            if (content != null && !content.trim().isEmpty()) {
                checklistName = content;
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

        return new Task(
                element.getAttribute("id"),
                element.getElementsByTagName("name").item(0).getTextContent(),
                TaskType.valueOf(element.getElementsByTagName("type").item(0).getTextContent()),
                weekday,
                Boolean.parseBoolean(element.getElementsByTagName("done").item(0).getTextContent()),
                element.getElementsByTagName("doneDate").item(0).getTextContent(),
                checklistName
        );
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

        if (task.getChecklistName() != null && !task.getChecklistName().trim().isEmpty()) {
            Element checklistNameElement = document.createElement("checklistName");
            checklistNameElement.setTextContent(task.getChecklistName());
            taskElement.appendChild(checklistNameElement);
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

        // Handle checklist name
        NodeList checklistNameNodes = taskElement.getElementsByTagName("checklistName");
        if (task.getChecklistName() != null) {
            Element checklistNameElement;
            if (checklistNameNodes.getLength() > 0) {
                checklistNameElement = (Element) checklistNameNodes.item(0);
            } else {
                checklistNameElement = document.createElement("checklistName");
                taskElement.appendChild(checklistNameElement);
            }
            checklistNameElement.setTextContent(task.getChecklistName());
        } else if (checklistNameNodes.getLength() > 0) {
            taskElement.removeChild(checklistNameNodes.item(0));
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
     * Checks if a task's done date is in the past and resets it if needed.
     */
    public void checkAndResetPastDoneDate(Task task, String today) throws ParseException {
        if (task.isDone() && task.getDoneDate() != null) {
            java.util.Date doneDate = getParsedDate(task.getDoneDate());
            java.util.Date todayDate = getParsedDate(today);
            if (doneDate.before(todayDate)) {
                task.setDone(false);
                task.setDoneDate(null);
            }
        }
    }
}