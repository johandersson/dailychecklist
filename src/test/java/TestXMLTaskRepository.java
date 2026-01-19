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
 */import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;
import java.lang.reflect.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TestXMLTaskRepository {

    private Path tempFile;

    @BeforeEach
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("tasks", ".xml");
        // Copy test XML
        Path testXml = Paths.get(getClass().getResource("/test-tasks.xml").toURI());
        Files.copy(testXml, tempFile, StandardCopyOption.REPLACE_EXISTING);
        // Set FILE_NAME
        Field field = XMLTaskRepository.class.getDeclaredField("FILE_NAME");
        field.setAccessible(true);
        field.set(null, tempFile.toString());
    }

    @AfterEach
    public void tearDown() throws Exception {
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void testGetAllTasks() {
        XMLTaskRepository repo = new XMLTaskRepository();
        List<Task> tasks = repo.getAllTasks();
        assertEquals(2, tasks.size());
        Task task1 = tasks.get(0);
        assertEquals("1", task1.getId());
        assertEquals("Test Task", task1.getName());
        assertEquals(TaskType.MORNING, task1.getType());
        assertNull(task1.getWeekday());
        assertFalse(task1.isDone());
        Task task2 = tasks.get(1);
        assertEquals("2", task2.getId());
        assertEquals("Weekday Task", task2.getName());
        assertEquals(TaskType.EVENING, task2.getType());
        assertEquals("monday", task2.getWeekday());
        assertTrue(task2.isDone());
    }

    @Test
    public void testAddTask() {
        XMLTaskRepository repo = new XMLTaskRepository();
        Task newTask = new Task("3", "New Task", TaskType.EVENING, "monday", false, null);
        repo.addTask(newTask);
        List<Task> tasks = repo.getAllTasks();
        assertEquals(3, tasks.size());
        Task added = tasks.stream().filter(t -> t.getId().equals("3")).findFirst().orElse(null);
        assertNotNull(added);
        assertEquals("New Task", added.getName());
        assertEquals(TaskType.EVENING, added.getType());
        assertEquals("monday", added.getWeekday());
    }

    @Test
    public void testUpdateTask() {
        XMLTaskRepository repo = new XMLTaskRepository();
        Task updatedTask = new Task("1", "Updated Task", TaskType.EVENING, "tuesday", true, "2025-11-19");
        repo.updateTask(updatedTask);
        List<Task> tasks = repo.getAllTasks();
        assertEquals(2, tasks.size());
        Task task = tasks.get(0);
        assertEquals("Updated Task", task.getName());
        assertEquals(TaskType.EVENING, task.getType());
        assertEquals("tuesday", task.getWeekday());
        assertTrue(task.isDone());
        assertEquals("2025-11-19", task.getDoneDate());
    }

    @Test
    public void testRemoveTask() {
        XMLTaskRepository repo = new XMLTaskRepository();
        Task taskToRemove = new Task("1", "Test Task", TaskType.MORNING, null, false, null);
        repo.removeTask(taskToRemove);
        List<Task> tasks = repo.getAllTasks();
        assertEquals(1, tasks.size());
    }

    @Test
    public void testHasUndoneTasks() {
        XMLTaskRepository repo = new XMLTaskRepository();
        assertTrue(repo.hasUndoneTasks());
        // Mark as done
        Task task = repo.getAllTasks().get(0);
        task.setDone(true);
        repo.updateTask(task);
        assertFalse(repo.hasUndoneTasks());
    }

    @Test
    public void testSetTasks() {
        XMLTaskRepository repo = new XMLTaskRepository();
        List<Task> newTasks = List.of(
            new Task("3", "Task 3", TaskType.MORNING, null, false, null),
            new Task("4", "Task 4", TaskType.EVENING, "wednesday", false, null)
        );
        repo.setTasks(newTasks);
        List<Task> tasks = repo.getAllTasks();
        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().anyMatch(t -> t.getId().equals("3")));
        assertTrue(tasks.stream().anyMatch(t -> t.getId().equals("4")));
    }

    @Test
    public void testInitialize() {
        XMLTaskRepository repo = new XMLTaskRepository();
        repo.initialize();
        // Just test that it doesn't throw exception
    }

    @Test
    public void testGetAllTasksCorruptedFile() throws Exception {
        // Write invalid XML
        Files.writeString(tempFile, "<invalid xml");
        XMLTaskRepository repo = new XMLTaskRepository();
        List<Task> tasks = repo.getAllTasks();
        // Should return empty list on error
        assertEquals(0, tasks.size());
    }

    @Test
    public void testGetAllTasksMissingFile() throws Exception {
        Files.deleteIfExists(tempFile);
        XMLTaskRepository repo = new XMLTaskRepository();
        List<Task> tasks = repo.getAllTasks();
        // Should return empty list on error
        assertEquals(0, tasks.size());
    }

    @Test
    public void testGetAllTasksEmptyFile() throws Exception {
        Files.writeString(tempFile, "");
        XMLTaskRepository repo = new XMLTaskRepository();
        List<Task> tasks = repo.getAllTasks();
        // Should return empty list on error
        assertEquals(0, tasks.size());
    }
}
