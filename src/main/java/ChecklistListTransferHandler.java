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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.TransferHandler;

public class ChecklistListTransferHandler extends TransferHandler {
    private final DefaultListModel<String> listModel;
    private final TaskManager taskManager;
    private final Runnable updateTasks;

    public ChecklistListTransferHandler(DefaultListModel<String> listModel, TaskManager taskManager, Runnable updateTasks) {
        this.listModel = listModel;
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support) || !support.isDrop()) {
            return false;
        }

        JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
        int index = dropLocation.getIndex();
        if (index < 0 || index >= listModel.getSize()) {
            return false;
        }

        String targetChecklistName = listModel.get(index);

        try {
            String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            String[] parts = data.split(";", 2);
            if (parts.length != 2) {
                return false;
            }
            String sourceChecklistName = parts[0];
            String taskId = parts[1];

            Task task = taskManager.getTaskById(taskId);
            if (task == null) {
                return false;
            }

            // Only allow moving between custom checklists
            if (!isCustomChecklist(sourceChecklistName) || !isCustomChecklist(targetChecklistName)) {
                return false;
            }

            task.setChecklistName(targetChecklistName);
            taskManager.updateTask(task);
            updateTasks.run();
            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            return false;
        }
    }

    private boolean isCustomChecklist(String name) {
        return name != null && !name.equals("MORNING") && !name.equals("EVENING");
    }
}