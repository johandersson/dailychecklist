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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestTask {

    @Test
    public void testTaskConstructorWithId() {
        Task task = new Task("id1", "Test Task", TaskType.MORNING, "Monday", true, "2023-10-01");
        assertEquals("id1", task.getId());
        assertEquals("Test Task", task.getName());
        assertEquals(TaskType.MORNING, task.getType());
        assertEquals("Monday", task.getWeekday());
        assertTrue(task.isDone());
        assertEquals("2023-10-01", task.getDoneDate());
    }

    @Test
    public void testTaskConstructorWithoutId() {
        Task task = new Task("Test Task", TaskType.EVENING, "Tuesday");
        assertNotNull(task.getId());
        assertEquals("Test Task", task.getName());
        assertEquals(TaskType.EVENING, task.getType());
        assertEquals("Tuesday", task.getWeekday());
        assertFalse(task.isDone());
        assertNull(task.getDoneDate());
    }

    @Test
    public void testSetters() {
        Task task = new Task("Test", TaskType.MORNING, null);
        task.setName("Updated");
        task.setType(TaskType.EVENING);
        task.setWeekday("Wednesday");
        task.setDone(true);
        task.setDoneDate(new java.util.Date(1696118400000L)); // 2023-10-01

        assertEquals("Updated", task.getName());
        assertEquals(TaskType.EVENING, task.getType());
        assertEquals("Wednesday", task.getWeekday());
        assertTrue(task.isDone());
        assertEquals("2023-10-01", task.getDoneDate());
    }

    @Test
    public void testEqualsAndHashCode() {
        Task task1 = new Task("id1", "Test", TaskType.MORNING, null, false, null);
        Task task2 = new Task("id1", "Test", TaskType.MORNING, null, false, null);
        Task task3 = new Task("id2", "Test", TaskType.MORNING, null, false, null);

        assertEquals(task1, task2);
        assertNotEquals(task1, task3);
        assertEquals(task1.hashCode(), task2.hashCode());
        assertNotEquals(task1.hashCode(), task3.hashCode());
    }

    @Test
    public void testToString() {
        Task task = new Task("id1", "Test", TaskType.MORNING, "Monday", true, "2023-10-01");
        String expected = "Task [id=id1, name=Test, type=MORNING, weekday=Monday, done=true]";
        assertEquals(expected, task.toString());
    }

    @Test
    public void testTaskWithNullWeekday() {
        Task task = new Task("Test", TaskType.MORNING, null);
        assertNull(task.getWeekday());
        assertFalse(task.isDone());
    }

    @Test
    public void testTaskSetDoneWithoutDate() {
        Task task = new Task("Test", TaskType.MORNING, null);
        task.setDone(true);
        assertTrue(task.isDone());
        assertNull(task.getDoneDate());
    }

    @Test
    public void testTaskSetDoneDate() {
        Task task = new Task("Test", TaskType.MORNING, null);
        task.setDoneDate(new java.util.Date(1696118400000L)); // 2023-10-01
        assertEquals("2023-10-01", task.getDoneDate());
    }
}
