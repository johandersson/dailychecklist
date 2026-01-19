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
 */import java.awt.GraphicsEnvironment;
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

public class XMLTaskRepository implements TaskRepository {
    private static String FILE_NAME = System.getProperty("user.home") + File.separator + "dailychecklist-tasks.xml";

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
                    Task task = new Task(
                            element.getAttribute("id"),
                            element.getElementsByTagName("name").item(0).getTextContent(),
                            TaskType.valueOf(element.getElementsByTagName("type").item(0).getTextContent()),
                            null,
                            Boolean.parseBoolean(element.getElementsByTagName("done").item(0).getTextContent()),
                            element.getElementsByTagName("doneDate").item(0).getTextContent()
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
        List<Task> tasks = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(System.currentTimeMillis()));
        try {
            Document document = readDocument();
            NodeList nodeList = document.getElementsByTagName("task");
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
                            element.getElementsByTagName("doneDate").item(0) != null ? element.getElementsByTagName("doneDate").item(0).getTextContent() : null
                    );
                    checkIfDoneDateIsInThePast(task, today);
                    tasks.add(task);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to load all tasks: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
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
            root.appendChild(taskElement);
            writeDocument(document);
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
                        writeDocument(document);
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
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new File(FILE_NAME));
    }

    private void writeDocument(Document document) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(new File(FILE_NAME));
        transformer.transform(source, result);
    }
}
