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
import java.util.List;
import java.util.ArrayList;
import javax.swing.DefaultListModel;

public class TestTaskUpdater {

    @Test
    public void testUpdateTasksShowWeekdayTrue() {
        TaskUpdater updater = new TaskUpdater();
        DefaultListModel<Task> morningModel = new DefaultListModel<>();
        DefaultListModel<Task> eveningModel = new DefaultListModel<>();
        
        List<Task> tasks = List.of(
            new Task("1", "Daily Morning", TaskType.MORNING, null, false, null),
            new Task("2", "Weekday Morning", TaskType.MORNING, "monday", false, null),
            new Task("3", "Daily Evening", TaskType.EVENING, null, false, null),
            new Task("4", "Weekday Evening", TaskType.EVENING, "tuesday", false, null)
        );
        
        updater.updateTasks(tasks, morningModel, eveningModel, true, "monday");
        
        assertEquals(2, morningModel.size());
        assertEquals(1, eveningModel.size());
        assertEquals("Daily Morning", morningModel.get(0).getName());
        assertEquals("Weekday Morning", morningModel.get(1).getName());
        assertEquals("Daily Evening", eveningModel.get(0).getName());
    }

    @Test
    public void testUpdateTasksShowWeekdayFalse() {
        TaskUpdater updater = new TaskUpdater();
        DefaultListModel<Task> morningModel = new DefaultListModel<>();
        DefaultListModel<Task> eveningModel = new DefaultListModel<>();
        
        List<Task> tasks = List.of(
            new Task("1", "Daily Morning", TaskType.MORNING, null, false, null),
            new Task("2", "Weekday Morning", TaskType.MORNING, "monday", false, null),
            new Task("3", "Daily Evening", TaskType.EVENING, null, false, null),
            new Task("4", "Weekday Evening", TaskType.EVENING, "tuesday", false, null)
        );
        
        updater.updateTasks(tasks, morningModel, eveningModel, false);
        
        assertEquals(1, morningModel.size());
        assertEquals(1, eveningModel.size());
        assertEquals("Daily Morning", morningModel.get(0).getName());
        assertEquals("Daily Evening", eveningModel.get(0).getName());
    }

    @Test
    public void testUpdateTasksEmptyList() {
        TaskUpdater updater = new TaskUpdater();
        DefaultListModel<Task> morningModel = new DefaultListModel<>();
        DefaultListModel<Task> eveningModel = new DefaultListModel<>();
        
        updater.updateTasks(new ArrayList<>(), morningModel, eveningModel, true);
        
        assertEquals(0, morningModel.size());
        assertEquals(0, eveningModel.size());
    }
}