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
import java.util.List;
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

/**
 * Handles XML operations for task data persistence.
 */
public class TaskXmlHandler {
    private final String fileName;
    private final DocumentBuilderFactory documentBuilderFactory;
    private final TransformerFactory transformerFactory;

    public TaskXmlHandler(String fileName) {
        this.fileName = fileName;
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.transformerFactory = TransformerFactory.newInstance();
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
            checklistName = checklistNameNodes.item(0).getTextContent();
        }

        return new Task(
                element.getAttribute("id"),
                element.getElementsByTagName("name").item(0).getTextContent(),
                TaskType.valueOf(element.getElementsByTagName("type").item(0).getTextContent()),
                element.getElementsByTagName("weekday").item(0).getTextContent(),
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
        taskElement.getElementsByTagName("name").item(0).setTextContent(task.getName());
        taskElement.getElementsByTagName("type").item(0).setTextContent(task.getType().name());
        taskElement.getElementsByTagName("weekday").item(0).setTextContent(task.getWeekday());
        taskElement.getElementsByTagName("done").item(0).setTextContent(String.valueOf(task.isDone()));
        taskElement.getElementsByTagName("doneDate").item(0).setTextContent(task.getDoneDate());

        // Handle checklist name
        NodeList checklistNameNodes = taskElement.getElementsByTagName("checklistName");
        if (task.getChecklistName() != null) {
            Element checklistNameElement;
            if (checklistNameNodes.getLength() > 0) {
                checklistNameElement = (Element) checklistNameNodes.item(0);
            } else {
                checklistNameElement = taskElement.getOwnerDocument().createElement("checklistName");
                taskElement.appendChild(checklistNameElement);
            }
            checklistNameElement.setTextContent(task.getChecklistName());
        } else if (checklistNameNodes.getLength() > 0) {
            taskElement.removeChild(checklistNameNodes.item(0));
        }
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
    public void checkAndResetPastDoneDate(Task task, String today) {
        if (task.isDone() && task.getDoneDate() != null) {
            try {
                java.util.Date doneDate = new SimpleDateFormat("yyyy-MM-dd").parse(task.getDoneDate());
                java.util.Date todayDate = new SimpleDateFormat("yyyy-MM-dd").parse(today);
                if (doneDate.before(todayDate)) {
                    task.setDone(false);
                    task.setDoneDate(null);
                }
            } catch (ParseException e) {
                showErrorDialog("Failed to parse date: " + e.getMessage(), "Date Parse Error");
            }
        }
    }

    /**
     * Shows an error dialog if not in headless mode.
     */
    private void showErrorDialog(String message, String title) {
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
        }
    }
}