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
import javax.swing.JList;
import java.awt.Component;

public class TestCheckboxListCellRenderer {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void testGetListCellRendererComponent() {
        CheckboxListCellRenderer renderer = new CheckboxListCellRenderer();
        JList<Task> list = new JList<>();
        Task task = new Task("Test Task", TaskType.MORNING, null);
        Component component = renderer.getListCellRendererComponent(list, task, 0, false, false);
        assertNotNull(component);
        assertEquals(renderer, component);
    }

    @Test
    void testGetListCellRendererComponentWeekday() {
        CheckboxListCellRenderer renderer = new CheckboxListCellRenderer();
        JList<Task> list = new JList<>();
        Task task = new Task("Test Task", TaskType.MORNING, "monday");
        Component component = renderer.getListCellRendererComponent(list, task, 0, false, false);
        assertNotNull(component);
    }
}