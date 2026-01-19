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
import java.lang.reflect.Field;
import javax.swing.DefaultListModel;

public class TestChecklistPanel {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    private TaskManager taskManager;
    private TaskUpdater taskUpdater;
    private ChecklistPanel checklistPanel;

    @BeforeEach
    void setUp() {
        taskManager = new TaskManager(new XMLTaskRepository());
        taskUpdater = new TaskUpdater();
        checklistPanel = new ChecklistPanel(taskManager, taskUpdater);
    }

    @Test
    void testSetShowWeekdayTasks() {
        checklistPanel.setShowWeekdayTasks(true);
        assertTrue(checklistPanel.isShowWeekdayTasks());

        checklistPanel.setShowWeekdayTasks(false);
        assertFalse(checklistPanel.isShowWeekdayTasks());
    }

    @Test
    void testUpdateTasks() throws Exception {
        // Add some test tasks
        Task morningTask = new Task("Morning Task", TaskType.MORNING, null);
        Task eveningTask = new Task("Evening Task", TaskType.EVENING, null);
        Task weekdayTask = new Task("Weekday Task", TaskType.MORNING, "monday");

        taskManager.addTask(morningTask);
        taskManager.addTask(eveningTask);
        taskManager.addTask(weekdayTask);

        // Get the models using reflection
        Field morningField = ChecklistPanel.class.getDeclaredField("morningListModel");
        morningField.setAccessible(true);
        DefaultListModel<Task> morningModel = (DefaultListModel<Task>) morningField.get(checklistPanel);

        Field eveningField = ChecklistPanel.class.getDeclaredField("eveningListModel");
        eveningField.setAccessible(true);
        DefaultListModel<Task> eveningModel = (DefaultListModel<Task>) eveningField.get(checklistPanel);

        // Test with showWeekdayTasks = false
        checklistPanel.setShowWeekdayTasks(false);
        checklistPanel.updateTasks();

        assertEquals(1, morningModel.getSize());
        assertEquals(1, eveningModel.getSize());

        // Test with showWeekdayTasks = true
        checklistPanel.setShowWeekdayTasks(true);
        checklistPanel.updateTasks();
    }

    @Test
    void testConstructor() throws Exception {
        // Check that the components are initialized
        Field morningListField = ChecklistPanel.class.getDeclaredField("morningTaskList");
        morningListField.setAccessible(true);
        assertNotNull(morningListField.get(checklistPanel));

        Field eveningListField = ChecklistPanel.class.getDeclaredField("eveningTaskList");
        eveningListField.setAccessible(true);
        assertNotNull(eveningListField.get(checklistPanel));

        Field checkboxField = ChecklistPanel.class.getDeclaredField("showWeekdayTasksCheckbox");
        checkboxField.setAccessible(true);
        assertNotNull(checkboxField.get(checklistPanel));
    }
}