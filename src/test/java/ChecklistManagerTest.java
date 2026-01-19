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
 *//*
import checklistmanager.ChecklistManager;
import checklistmanager.DatabaseConnection;
import checklistmanager.Task;
import checklistmanager.TaskType;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
// Mock implementation of DatabaseConnection
class MockDatabaseConnection implements DatabaseConnection {
    private Connection connection;

    public MockDatabaseConnection(Connection connection) {
        this.connection = connection;
    }

    //avoid database connection closed
    @Override
    public Connection getConnection() throws SQLException {
        //avoid database connection closed
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        }
        // Return the in-memory connection
        return connection;
    }
}
class ChecklistManagerTest {
    private Connection connection;
    private ChecklistManager checklistManager;

    @BeforeEach
    void setUp() throws SQLException {
        // Create an in-memory SQLite database
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        DatabaseConnection mockDatabaseConnection = new MockDatabaseConnection(connection);
        checklistManager = new ChecklistManager(mockDatabaseConnection);

        // Initialize the database schema
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE tasks (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    weekday TEXT,
                    done INTEGER,
                    doneDate TEXT
                );
            """);
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void testAddTask() throws SQLException {
        Task task = new Task("1", "Test Task", TaskType.MORNING, null, false);
        checklistManager.addTask(task);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tasks WHERE id = '1'")) {
            assertTrue(rs.next());
            assertEquals("Test Task", rs.getString("name"));
            assertEquals("MORNING", rs.getString("type"));
            assertNull(rs.getString("weekday"));
            assertEquals(0, rs.getInt("done"));
        }
    }

    @Test
    void testGetDailyTasks() {
        Task task1 = new Task("1", "Daily Task 1", TaskType.MORNING, null, false);
        Task task2 = new Task("2", "Daily Task 2", TaskType.EVENING, "", false);
        Task task3 = new Task("3", "Weekday Task", TaskType.MORNING, "Monday", false);

        checklistManager.addTask(task1);
        checklistManager.addTask(task2);
        checklistManager.addTask(task3);

        List<Task> dailyTasks = checklistManager.getDailyTasks();
        assertEquals(2, dailyTasks.size());
        assertTrue(dailyTasks.contains(task1));
        assertTrue(dailyTasks.contains(task2));
    }

    @Test
    void testGetAllTasks() {
        Task task1 = new Task("1", "Task 1", TaskType.MORNING, null, false);
        Task task2 = new Task("2", "Task 2", TaskType.EVENING, "Monday", true);

        checklistManager.addTask(task1);
        checklistManager.addTask(task2);

        List<Task> allTasks = checklistManager.getAllTasks();
        assertEquals(2, allTasks.size());
        assertTrue(allTasks.contains(task1));
        assertTrue(allTasks.contains(task2));
    }

    @Test
    void testUpdateTask() throws SQLException {
        Task task = new Task("1", "Original Task", TaskType.MORNING, null, false);
        checklistManager.addTask(task);

        Task updatedTask = new Task("1", "Updated Task", TaskType.EVENING, "Tuesday", true);
        checklistManager.updateTask(updatedTask);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tasks WHERE id = '1'")) {
            assertTrue(rs.next());
            assertEquals("Updated Task", rs.getString("name"));
            assertEquals("EVENING", rs.getString("type"));
            assertEquals("Tuesday", rs.getString("weekday"));
            assertEquals(1, rs.getInt("done"));
        }
    }

    @Test
    void testRemoveTask() throws SQLException {
        Task task = new Task("1", "Task to Remove", TaskType.MORNING, null, false);
        checklistManager.addTask(task);

        checklistManager.removeTask(task);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tasks WHERE id = '1'")) {
            assertFalse(rs.next());
        }
    }

    @Test
    void testHasUndoneTasks() {
        Task task1 = new Task("1", "Undone Task", TaskType.MORNING, null, false);
        Task task2 = new Task("2", "Done Task", TaskType.EVENING, null, true);

        checklistManager.addTask(task1);
        checklistManager.addTask(task2);

        assertTrue(checklistManager.hasUndoneTasks());
    }

    @Test
    void testSetTasks() throws SQLException {
        Task task1 = new Task("1", "Task 1", TaskType.MORNING, null, false);
        Task task2 = new Task("2", "Task 2", TaskType.EVENING, "Monday", true);

        checklistManager.setTasks(List.of(task1, task2));

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tasks")) {
            assertTrue(rs.next());
            assertEquals("Task 1", rs.getString("name"));
            assertTrue(rs.next());
            assertEquals("Task 2", rs.getString("name"));
        }
    }
}*/

