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
 */import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

public class ListItemTransferHandler extends TransferHandler {
    private JList<Task> list;
    private final DefaultListModel<Task> morningListModel;
    private final DefaultListModel<Task> eveningListModel;
    private final TaskManager checklistManager;

    public ListItemTransferHandler(JList<Task> list,
                                   DefaultListModel<Task> morningListModel,
                                   DefaultListModel<Task> eveningListModel,
                                   TaskManager checklistManager) {
        this.list = list;
        this.morningListModel = morningListModel;
        this.eveningListModel = eveningListModel;
        this.checklistManager = checklistManager;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    /**
     * When starting a drag, we now encode the source list type and index.
     * For example, "morning;2" means an item at index 2 in the morning list.
     */
    @Override
    protected Transferable createTransferable(JComponent c) {
        int index = list.getSelectedIndex();
        // Determine which list this handler is attached to by comparing models.
        String sourceType = (list.getModel() == morningListModel) ? "morning" : "evening";
        return new StringSelection(sourceType + ";" + index);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    /**
     * In importData we first parse the transferable data to figure out its source.
     * Then, we remove the task from the appropriate list and insert it into the drop target's model.
     * If dragging within the same list, we adjust the dropIndex accordingly.
     */
    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support) || !support.isDrop()) {
            return false;
        }

        DefaultListModel<Task> targetModel = (DefaultListModel<Task>) list.getModel();
        int dropIndex = getDropIndex(support);

        try {
            // The transferable contains the source type and index in the format "source;index"
            String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            String[] parts = data.split(";");
            if (parts.length != 2) {
                return false;
            }
            String sourceType = parts[0];
            int sourceIndex = Integer.parseInt(parts[1]);

            // Determine the source model based on the tag
            DefaultListModel<Task> sourceModel = sourceType.equals("morning") ? morningListModel : eveningListModel;

            // If dragging within the same list, adjust dropIndex if needed
            if (sourceModel == targetModel && sourceIndex < dropIndex) {
                dropIndex--;
            }

            // Remove the task from its source list and add it to the drop target's list
            Task draggedTask = sourceModel.remove(sourceIndex);
            targetModel.add(dropIndex, draggedTask);

            // Save the new order. This method combines tasks from both models.
            saveTaskOrder();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static int getDropIndex(TransferSupport support) {
        JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
        return dropLocation.getIndex();
    }

    /**
     * Combines tasks from the morning and evening models into one list and sends it to the TaskManager.
     */
    private void saveTaskOrder() {
        List<Task> tasks = new ArrayList<>();

        // For tasks in the morning list, update their type to MORNING.
        for (int i = 0; i < morningListModel.getSize(); i++) {
            Task t = morningListModel.get(i);
            t.setType(TaskType.MORNING); // update type
            tasks.add(t);
        }

        // For tasks in the evening list, update their type to EVENING.
        for (int i = 0; i < eveningListModel.getSize(); i++) {
            Task t = eveningListModel.get(i);
            t.setType(TaskType.EVENING); // update type
            tasks.add(t);
        }

        checklistManager.setTasks(tasks);
    }

}

