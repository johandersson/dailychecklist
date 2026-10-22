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
import java.time.LocalDate;
import java.util.Arrays;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class TaskUpdaterSnapshotTest {
    @Test
    public void reorderedProjectionDoesNotDuplicateInterveningRows() throws Exception {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("a");
        model.addElement("b");
        model.addElement("c");
        SwingUtilities.invokeAndWait(() -> TaskUpdater.syncModel(model, java.util.List.of("c", "b", "a")));
        assertEquals(java.util.List.of("c", "b", "a"), java.util.Collections.list(model.elements()));
    }

    @Test
    public void rendersHierarchyFromOneSnapshotWithoutManagerReads() throws Exception {
        Task parent = new Task("parent", TaskType.MORNING, null, null);
        Task child = new Task("child", TaskType.MORNING, null, null, parent.getId());
        Task heading = new Task("heading", TaskType.HEADING, null, null, parent.getId());
        DefaultListModel<Task> morning = new DefaultListModel<>();
        DefaultListModel<Task> evening = new DefaultListModel<>();
        SwingUtilities.invokeAndWait(() ->
            new TaskUpdater().updateTasks(Arrays.asList(parent, heading, child), morning, evening));
        assertEquals(3, morning.size());
        assertSame(heading, morning.get(0));
        assertSame(parent, morning.get(1));
        assertSame(child, morning.get(2));
        assertTrue(evening.isEmpty());
    }

    @Test
    public void weekdayFilteringOnlyChangesProjection() throws Exception {
        String otherDay = LocalDate.now().plusDays(1).getDayOfWeek().name();
        Task parent = new Task("parent", TaskType.MORNING, null, null);
        Task hidden = new Task("hidden", TaskType.MORNING, otherDay, null);
        Task child = new Task("child", TaskType.MORNING, otherDay, null, parent.getId());
        java.util.List<Task> snapshot = java.util.List.of(parent, hidden, child);
        DefaultListModel<Task> morning = new DefaultListModel<>();
        DefaultListModel<Task> evening = new DefaultListModel<>();
        SwingUtilities.invokeAndWait(() ->
            new TaskUpdater().updateTasks(snapshot, morning, evening, false, null));
        assertEquals(1, morning.size());
        assertSame(parent, morning.get(0));
        assertEquals(3, snapshot.size());
        SwingUtilities.invokeAndWait(() ->
            new TaskUpdater().updateTasks(snapshot, morning, evening, true, null));
        assertEquals(3, morning.size());
        assertSame(child, morning.get(1));
        assertSame(hidden, morning.get(2));
    }
}
