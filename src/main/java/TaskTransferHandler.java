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
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

public class TaskTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;
    private final JList<Task> list;
    private final transient TaskManager taskManager;
    private final TaskMovePlanner.Destination destination;
    private final transient Runnable updateAllPanels;

    public TaskTransferHandler(JList<Task> list, DefaultListModel<Task> model,
            TaskManager manager, String checklistName) {
        this(list, model, manager, checklistName, null);
    }

    public TaskTransferHandler(JList<Task> list, DefaultListModel<Task> model,
            TaskManager manager, String checklistName, Runnable updateAllPanels) {
        this(list, model, manager, checklistName, updateAllPanels, null, null);
    }

    public TaskTransferHandler(JList<Task> list, DefaultListModel<Task> model,
            TaskManager manager, String checklistName, Runnable updateAllPanels,
            DefaultListModel<Task> morning, DefaultListModel<Task> evening) {
        this.list = list;
        this.taskManager = manager;
        this.destination = model == morning && morning != null
                ? new TaskMovePlanner.Destination(TaskType.MORNING, null)
                : model == evening && evening != null
                    ? new TaskMovePlanner.Destination(TaskType.EVENING, null)
                    : TaskDropController.resolveDestination(manager, checklistName, list);
        this.updateAllPanels = updateAllPanels;
        TaskDropController.register(list, manager, () -> destination);
    }

    @Override
    protected Transferable createTransferable(JComponent component) {
        List<String> ids = list.getSelectedValuesList().stream().map(Task::getId).toList();
        return ids.isEmpty() ? null : new TaskTransferable(ids);
    }

    @Override
    public int getSourceActions(JComponent component) {
        return MOVE;
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
        try {
            List<String> ids = TaskTransferable.readIds(support.getTransferable());
            JList.DropLocation location = (JList.DropLocation) support.getDropLocation();
            TaskMovePlanner.Intent intent = TaskDropController.buildIntent(taskManager, ids,
                    destination, list.getModel(), location.getIndex(), location.isInsert());
            return TaskDropController.submit(taskManager, intent, list, updateAllPanels, null);
        } catch (UnsupportedFlavorException | IOException | IllegalArgumentException e) {
            TaskDropController.showFailure(list, e);
            return false;
        }
    }
}
