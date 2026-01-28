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
import java.util.function.Consumer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.TransferHandler;

@SuppressWarnings("serial")
public class ChecklistListTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;
    private transient final DefaultListModel<Checklist> listModel;
    private transient final TaskManager taskManager;
    private transient final Runnable updateTasks;
    private final Consumer<String> onChecklistDrop;

    public ChecklistListTransferHandler(DefaultListModel<Checklist> listModel, TaskManager taskManager, Runnable updateTasks, Consumer<String> onChecklistDrop) {
        this.listModel = listModel;
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        this.onChecklistDrop = onChecklistDrop;
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

        String targetChecklistName = listModel.get(index).getName();

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

            // Find the target checklist by name
            Checklist targetChecklist = null;
            for (Checklist checklist : taskManager.getCustomChecklists()) {
                if (targetChecklistName.equals(checklist.getName())) {
                    targetChecklist = checklist;
                    break;
                }
            }
            if (targetChecklist == null) {
                return false;
            }

            // Only allow moving between custom checklists
            if (!isCustomChecklist(sourceChecklistName) || !isCustomChecklist(targetChecklistName)) {
                return false;
            }

            taskManager.moveTaskToChecklist(task, targetChecklist);
            updateTasks.run();
            // Notify the overview panel to select the target checklist in the UI
            if (onChecklistDrop != null) {
                onChecklistDrop.accept(targetChecklistName);
            } else {
                // fallback: try to select in the JList
                try {
                    Object comp = support.getComponent();
                    if (comp instanceof JList<?> list) {
                        list.setSelectedValue(targetChecklistName, true);
                    }
                } catch (Exception ignored) {
                }
            }
            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            return false;
        }
    }

    private boolean isCustomChecklist(String name) {
        return name != null && !name.equals("MORNING") && !name.equals("EVENING");
    }
}