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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestAddTaskPanel {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    private TaskManager taskManager;
    private AddTaskPanel addTaskPanel;

    @BeforeEach
    void setUp() {
        taskManager = new TaskManager(new XMLTaskRepository());
        addTaskPanel = new AddTaskPanel(taskManager, () -> {});
    }

    @Test
    void testInstantiation() {
        // Just test that it can be instantiated without exception
        assertNotNull(addTaskPanel);
    }
}