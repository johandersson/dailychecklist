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
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.TransferHandler;

public class ChecklistListTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;
    private final DefaultListModel<Checklist> listModel;
    private final transient TaskManager taskManager;
    private final transient Runnable updateTasks;
    private final transient Consumer<String> onChecklistDrop;

    public ChecklistListTransferHandler(DefaultListModel<Checklist> listModel,
            TaskManager taskManager, Runnable updateTasks, Consumer<String> onChecklistDrop) {
        this.listModel = listModel;
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        this.onChecklistDrop = onChecklistDrop;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDrop() && support.getComponent() instanceof JList<?>
                && TaskTransferable.supports(support.getTransferable());
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        int index = ((JList.DropLocation) support.getDropLocation()).getIndex();
        if (index < 0 || index >= listModel.size()) {
            return false;
        }
        Checklist target = listModel.get(index);
        try {
            List<String> ids = TaskTransferable.readIds(support.getTransferable());
            TaskDropController.validateCustomSelection(taskManager, ids);
            TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(ids,
                    new TaskMovePlanner.Destination(TaskType.CUSTOM, target.getId()), null, null);
            return TaskDropController.submit(taskManager, intent, null, updateTasks,
                    () -> {
                        if (onChecklistDrop != null) {
                            onChecklistDrop.accept(target.getName());
                        }
                    });
        } catch (UnsupportedFlavorException | IOException | IllegalArgumentException e) {
            TaskDropController.showFailure(support.getComponent(), e);
            return false;
        }
    }
}
