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
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
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
        this(list, manager, model == morning && morning != null
                ? new TaskMovePlanner.Destination(TaskType.MORNING, null)
                : model == evening && evening != null
                    ? new TaskMovePlanner.Destination(TaskType.EVENING, null)
                    : TaskDropController.resolveDestination(manager, checklistName, list),
                updateAllPanels);
    }

    public TaskTransferHandler(JList<Task> list, TaskManager manager,
            TaskMovePlanner.Destination destination, Runnable updateAllPanels) {
        this.list = list;
        this.taskManager = manager;
        this.destination = java.util.Objects.requireNonNull(destination);
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
    public Image getDragImage() {
        List<Task> selected = list.getSelectedValuesList();
        if (selected.isEmpty()) {
            return null;
        }
        boolean multiple = selected.size() > 1;
        String text = multiple ? selected.size() + " tasks" : selected.get(0).getName();
        if (!multiple && text.length() > 20) {
            text = text.substring(0, 17) + "...";
        }
        int width = multiple ? 180 : 200;
        BufferedImage image = new BufferedImage(width, 25, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(multiple ? new Color(100, 150, 200, 220)
                    : new Color(240, 240, 240, 200));
            graphics.fillRoundRect(0, 0, width, 25, 5, 5);
            graphics.setColor(multiple ? Color.BLUE : Color.GRAY);
            graphics.drawRoundRect(0, 0, width - 1, 24, 5, 5);
            graphics.setColor(multiple ? Color.WHITE : Color.BLACK);
            graphics.setFont(new Font("SansSerif", multiple ? Font.BOLD : Font.PLAIN, 12));
            graphics.drawString(text, 5, 17);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    @Override
    public Point getDragImageOffset() {
        return new Point(10, 10);
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
