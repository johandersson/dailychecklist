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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class TestTaskTransferable {
    @Test
    void testGetTransferDataFlavors() {
        Task task = new Task("Test", TaskType.MORNING, null);
        TaskTransferable transferable = new TaskTransferable(task);
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        assertEquals(1, flavors.length);
        assertEquals(TaskTransferable.TASK_FLAVOR, flavors[0]);
    }

    @Test
    void testIsDataFlavorSupported() {
        Task task = new Task("Test", TaskType.MORNING, null);
        TaskTransferable transferable = new TaskTransferable(task);
        assertTrue(transferable.isDataFlavorSupported(TaskTransferable.TASK_FLAVOR));
        assertFalse(transferable.isDataFlavorSupported(DataFlavor.stringFlavor));
    }

    @Test
    void testGetTransferData() throws UnsupportedFlavorException, IOException {
        Task task = new Task("Test", TaskType.MORNING, null);
        TaskTransferable transferable = new TaskTransferable(task);
        Object data = transferable.getTransferData(TaskTransferable.TASK_FLAVOR);
        assertEquals(task, data);
    }

    @Test
    void testGetTransferDataUnsupported() {
        Task task = new Task("Test", TaskType.MORNING, null);
        TaskTransferable transferable = new TaskTransferable(task);
        assertThrows(UnsupportedFlavorException.class, () -> transferable.getTransferData(DataFlavor.stringFlavor));
    }
}