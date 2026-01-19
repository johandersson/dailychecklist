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

public class TestTaskInputPanel {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void testConstructor() {
        TaskInputPanel panel = new TaskInputPanel("Test Task", "10 minutes");
        assertNotNull(panel);
    }

    @Test
    void testGetTask() {
        TaskInputPanel panel = new TaskInputPanel("Test Task", "10 minutes");
        assertEquals("Test Task", panel.getTask());
    }

    @Test
    void testGetSelectedTime() {
        TaskInputPanel panel = new TaskInputPanel("Test Task", "10 minutes");
        assertEquals("10 minutes", panel.getSelectedTime());
    }

    @Test
    void testGetTimeInSeconds() {
        TaskInputPanel panel = new TaskInputPanel("Test Task", "10 minutes");
        assertEquals(10 * 60, panel.getTimeInSeconds());
    }
}