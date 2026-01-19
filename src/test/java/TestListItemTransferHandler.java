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
import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.Point;
import java.util.List;

public class TestListItemTransferHandler {
    @Test
    void testCanImport() {
        DefaultListModel<Task> morningModel = new DefaultListModel<>();
        DefaultListModel<Task> eveningModel = new DefaultListModel<>();
        TaskManager taskManager = new TaskManager(new XMLTaskRepository());
        JList<Task> list = new JList<>(morningModel);
        ListItemTransferHandler handler = new ListItemTransferHandler(list, morningModel, eveningModel, taskManager);

        // Mock TransferSupport
        TransferHandler.TransferSupport support = new TransferHandler.TransferSupport(list, new StringSelection("test"));
        assertTrue(handler.canImport(support));

        TransferHandler.TransferSupport support2 = new TransferHandler.TransferSupport(list, new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[0];
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return false;
            }

            @Override
            public Object getTransferData(DataFlavor flavor) {
                return null;
            }
        });
        assertFalse(handler.canImport(support2));
    }

    @Test
    void testGetSourceActions() {
        DefaultListModel<Task> morningModel = new DefaultListModel<>();
        DefaultListModel<Task> eveningModel = new DefaultListModel<>();
        TaskManager taskManager = new TaskManager(new XMLTaskRepository());
        JList<Task> list = new JList<>(morningModel);
        ListItemTransferHandler handler = new ListItemTransferHandler(list, morningModel, eveningModel, taskManager);

        assertEquals(TransferHandler.MOVE, handler.getSourceActions(list));
    }

    @Test
    void testCreateTransferable() {
        DefaultListModel<Task> morningModel = new DefaultListModel<>();
        morningModel.addElement(new Task("Test", TaskType.MORNING, null));
        DefaultListModel<Task> eveningModel = new DefaultListModel<>();
        TaskManager taskManager = new TaskManager(new XMLTaskRepository());
        JList<Task> list = new JList<>(morningModel);
        list.setSelectedIndex(0);
        ListItemTransferHandler handler = new ListItemTransferHandler(list, morningModel, eveningModel, taskManager);

        Transferable transferable = handler.createTransferable(list);
        assertNotNull(transferable);
        assertTrue(transferable.isDataFlavorSupported(DataFlavor.stringFlavor));
        try {
            String data = (String) transferable.getTransferData(DataFlavor.stringFlavor);
            assertEquals("morning;0", data);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testImportDataInvalid() {
        DefaultListModel<Task> morningModel = new DefaultListModel<>();
        DefaultListModel<Task> eveningModel = new DefaultListModel<>();
        TaskManager taskManager = new TaskManager(new XMLTaskRepository());
        JList<Task> list = new JList<>(morningModel);
        ListItemTransferHandler handler = new ListItemTransferHandler(list, morningModel, eveningModel, taskManager);

        // Invalid transferable
        Transferable transferable = new StringSelection("invalid");
        TransferHandler.TransferSupport support = new TransferHandler.TransferSupport(list, transferable);
        assertFalse(handler.importData(support));
    }
}