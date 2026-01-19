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
import java.net.URISyntaxException;
import static org.junit.jupiter.api.Assertions.*;

public class TestTaskManager {

    private Path tempFile;
    private XMLTaskRepository repo;
    private TaskManager manager;

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
        repo = new XMLTaskRepository();
        manager = new TaskManager(repo);
    }

    @AfterEach
    public void tearDown() throws Exception {
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void testGetAllTasks() {
        List<Task> tasks = manager.getAllTasks();
        assertEquals(2, tasks.size());
        assertEquals("Test Task", tasks.get(0).getName());
    }

    @Test
    public void testAddTask() {
        Task newTask = new Task("New Task", TaskType.EVENING, "monday");
        manager.addTask(newTask);
        List<Task> tasks = manager.getAllTasks();
        assertEquals(3, tasks.size());
        assertTrue(tasks.stream().anyMatch(t -> t.getName().equals("New Task")));
    }

    @Test
    public void testUpdateTask() {
        Task task = manager.getAllTasks().get(0);
        task.setName("Updated");
        manager.updateTask(task);
        List<Task> tasks = manager.getAllTasks();
        assertEquals("Updated", tasks.get(0).getName());
    }

    @Test
    public void testRemoveTask() {
        Task task = manager.getAllTasks().get(0);
        manager.removeTask(task);
        List<Task> tasks = manager.getAllTasks();
        assertEquals(1, tasks.size());
    }

    @Test
    public void testHasUndoneTasks() {
        assertTrue(manager.hasUndoneTasks());
        Task task = manager.getAllTasks().get(0);
        task.setDone(true);
        manager.updateTask(task);
        assertFalse(manager.hasUndoneTasks());
    }
}
