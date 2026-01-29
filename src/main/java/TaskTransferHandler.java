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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

@SuppressWarnings("serial")
public class TaskTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;
    private JList<Task> list;
    private final DefaultListModel<Task> listModel;
    private transient final TaskManager taskManager;
    private final String checklistName; // null for daily checklists
    private transient final Runnable updateAllPanels;
    

    public TaskTransferHandler(JList<Task> list, DefaultListModel<Task> listModel, TaskManager taskManager, String checklistName) {
        this(list, listModel, taskManager, checklistName, null);
    }

    public TaskTransferHandler(JList<Task> list, DefaultListModel<Task> listModel, TaskManager taskManager, String checklistName, Runnable updateAllPanels) {
        this(list, listModel, taskManager, checklistName, updateAllPanels, null, null);
    }

    public TaskTransferHandler(JList<Task> list, DefaultListModel<Task> listModel, TaskManager taskManager, String checklistName, Runnable updateAllPanels, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel) {
        this.list = list;
        this.listModel = listModel;
        this.taskManager = taskManager;
        this.checklistName = checklistName;
        this.updateAllPanels = updateAllPanels;
        // morning/evening list models are accepted by the factory for callers,
        // but not used by this handler directly.
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        int[] selectedIndices = list.getSelectedIndices();
        if (selectedIndices.length > 0) {
            StringBuilder data = new StringBuilder();
            // Represent null checklistName as empty string in the transfer payload
            data.append(checklistName == null ? "" : checklistName);
            for (int index : selectedIndices) {
                Task task = listModel.get(index);
                data.append(";").append(task.getId());
            }
            DebugLog.d("createTransferable: checklist=%s selectedCount=%d ids=%s", checklistName, selectedIndices.length, data.toString());
            return new StringSelection(data.toString());
        }
        return null;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        if (updateAllPanels != null) {
            updateAllPanels.run();
        }
    }

    @Override
    public Image getDragImage() {
        int[] selectedIndices = list.getSelectedIndices();
        if (selectedIndices.length > 0) {
            if (selectedIndices.length == 1) {
                Task task = listModel.get(selectedIndices[0]);
                return createDragImage(task);
            } else {
                // Multiple tasks selected
                return createMultiDragImage(selectedIndices.length);
            }
        }
        return null;
    }

    @Override
    public Point getDragImageOffset() {
        return new Point(10, 10); // Offset from cursor
    }

    private Image createDragImage(Task task) {
        String text = task.getName();
        if (text.length() > 20) {
            text = text.substring(0, 17) + "...";
        }

        // Create a simple text image
        BufferedImage image = new BufferedImage(200, 25, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw background
        g2d.setColor(new java.awt.Color(240, 240, 240, 200)); // Semi-transparent light gray
        g2d.fillRoundRect(0, 0, 200, 25, 5, 5);

        // Draw border
        g2d.setColor(java.awt.Color.GRAY);
        g2d.drawRoundRect(0, 0, 199, 24, 5, 5);

        // Draw text
        g2d.setColor(java.awt.Color.BLACK);
        g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
        g2d.drawString(text, 5, 17);

        g2d.dispose();
        return image;
    }

    private Image createMultiDragImage(int count) {
        String text = count + " tasks";
        if (text.length() > 15) {
            text = count + " items";
        }

        // Create a simple text image for multiple tasks
        BufferedImage image = new BufferedImage(180, 25, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw background
        g2d.setColor(new java.awt.Color(100, 150, 200, 220)); // Semi-transparent blue for multi-select
        g2d.fillRoundRect(0, 0, 180, 25, 5, 5);

        // Draw border
        g2d.setColor(java.awt.Color.BLUE);
        g2d.drawRoundRect(0, 0, 179, 24, 5, 5);

        // Draw text
        g2d.setColor(java.awt.Color.WHITE);
        g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        g2d.drawString(text, 5, 17);

        g2d.dispose();
        return image;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support) || !support.isDrop()) {
            return false;
        }

        try {
            DebugLog.d("importData: incoming drop on component=%s", support.getComponent().getClass().getSimpleName());
            TransferData transferData = extractTransferData(support);
            if (transferData == null) {
                return false;
            }

            int dropIndex = getDropIndex(support);
            DebugLog.d("importData: dropIndex=%d", dropIndex);

            // Delegate to helper that handles drop-on-item, insert-between and fallback moves
            JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
            boolean isInsert = dl.isInsert();
            Object comp = support.getComponent();
            if (comp instanceof JList) {
                @SuppressWarnings("unchecked")
                JList<Task> targetList = (JList<Task>) comp;
                // If this is a same-checklist drag and all moved tasks are top-level,
                // prefer same-checklist reorder immediately to avoid interpreting the
                // drop as a subtask insertion.
                boolean sameChecklist = java.util.Objects.equals(transferData.sourceChecklistName, checklistName);
                boolean movedAllTopLevel = true;
                for (Task mt : transferData.tasks) {
                    if (mt.getParentId() != null) { movedAllTopLevel = false; break; }
                }
                if (sameChecklist && movedAllTopLevel) {
                    DebugLog.d("importData: same-checklist top-level reorder shortcut: checklist=%s dropIndex=%d", checklistName, dropIndex);
                    if (handleSameChecklistReorder(transferData, dropIndex)) return true;
                    // If reorder didn't handle it, continue to normal handling below
                }

                if (tryHandleDropOnItemCases(targetList, transferData, dropIndex, isInsert)) return true;

                // If same-checklist reorder did not run or did not handle it, fall back to cross-checklist move
                return handleCrossChecklistMove(transferData, dropIndex);
            }
        } catch (UnsupportedFlavorException | IOException e) {
            return false;
        }
        return false;
    }

    private boolean tryHandleDropOnItemCases(JList<Task> targetList, TransferData transferData, int dropIndex, boolean isInsert) {
        // If the drag originates from the same checklist and all moved tasks are top-level,
        // prefer same-checklist reorders handled later instead of converting the drop into
        // a subtask insertion. This preserves the ability to reorder top-level tasks.
        boolean sameChecklist = java.util.Objects.equals(transferData.sourceChecklistName, checklistName);
        boolean movedAllTopLevel = true;
        for (Task mt : transferData.tasks) {
            if (mt.getParentId() != null) { movedAllTopLevel = false; break; }
        }

        if (!isInsert && dropIndex >= 0 && dropIndex < targetList.getModel().getSize()) {
            Task modelTarget = targetList.getModel().getElementAt(dropIndex);
            DebugLog.d("importData: dropOnItem targetIndex=%d targetId=%s isInsert=%s", dropIndex, modelTarget == null ? "<null>" : modelTarget.getId(), isInsert);
            if (modelTarget != null && modelTarget.getParentId() == null) {
                // Dropped onto top-level parent. If this is a same-checklist drag of top-level
                // tasks, don't treat it as "drop onto parent" — let same-checklist reorder handle it.
                if (!(sameChecklist && movedAllTopLevel)) {
                    int parentIndex = dropIndex;
                    if (TaskDropHandler.handleDropOnItem(transferData, targetList, parentIndex, -1, taskManager, updateAllPanels, checklistName)) return true;
                }
            } else {
                int parentIndexFallback = findNearestTopLevelParentIndex(targetList, dropIndex);
                DebugLog.d("importData: dropOnSubtask parentIndexFallback=%d", parentIndexFallback);
                if (parentIndexFallback != -1) {
                    int offset = computeInsertOffsetWithinParent(targetList, parentIndexFallback, dropIndex);
                    DebugLog.d("importData: computed offset within parent=%d", offset);
                    // If this is a same-checklist drag of top-level tasks, avoid interpreting
                    // the drop as a nested subtask insertion.
                    if (!(sameChecklist && movedAllTopLevel)) {
                        if (TaskDropHandler.handleDropOnItem(transferData, targetList, parentIndexFallback, offset, taskManager, updateAllPanels, checklistName)) return true;
                    }
                }
            }
        }

        if (isInsert) {
            int parentIndex = findNearestTopLevelParentIndex(targetList, dropIndex);
            DebugLog.d("importData: insertBetween parentIndex=%d", parentIndex);
            if (parentIndex != -1) {
                int offset = computeInsertOffsetWithinParent(targetList, parentIndex, dropIndex);
                DebugLog.d("importData: computed insert offset=%d", offset);
                // For same-checklist drags of top-level tasks, prefer reorder semantics.
                if (!(sameChecklist && movedAllTopLevel)) {
                    if (TaskDropHandler.handleDropOnItem(transferData, targetList, parentIndex, offset, taskManager, updateAllPanels, checklistName)) return true;
                }
            }
        }
        return false;
    }

    

    private TransferData extractTransferData(TransferHandler.TransferSupport support)
            throws UnsupportedFlavorException, IOException {
        String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
        DebugLog.d("extractTransferData: raw=%s", data == null ? "<null>" : data);
        String[] parts = data.split(";", -1);
        if (parts.length < 2) {
            return null;
        }

        String sourceChecklistName = parts[0].isEmpty() ? null : parts[0];

        // Extract all task IDs
        List<String> taskIds = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                taskIds.add(parts[i]);
            }
        }

        if (taskIds.isEmpty()) {
            return null;
        }

        // Find all tasks by IDs
        List<Task> tasks = new ArrayList<>();
        for (String taskId : taskIds) {
            Task task = findTaskById(taskId);
            if (task == null) {
                return null;
            }
            tasks.add(task);
        }
        DebugLog.d("extractTransferData: sourceChecklist=%s taskIds=%s", sourceChecklistName, taskIds.toString());

        return new TransferData(sourceChecklistName, tasks);
    }

    private boolean handleSameChecklistReorder(TransferData transferData, int dropIndex) {
        return TaskReorderHandler.performReorder(listModel, taskManager, checklistName, transferData.tasks, dropIndex);
    }

    private boolean handleCrossChecklistMove(TransferData transferData, int dropIndex) {
        // Delegate to TaskMoveHandler which encapsulates move logic and selection/jump ordering
        return TaskMoveHandler.performMove(list, listModel, taskManager, checklistName, updateAllPanels, transferData.tasks, dropIndex);
    }

    

    private Task findTaskById(String taskId) {
        return taskManager.getTaskById(taskId);
    }

    /**
     * Find nearest top-level parent index for a given insert drop index.
     * Search backwards from dropIndex-1; if none found, search forward from dropIndex.
     */
    private int findNearestTopLevelParentIndex(JList<Task> list, int dropIndex) {
        int size = list.getModel().getSize();
        // Clamp dropIndex into [0, size]
        if (dropIndex < 0) dropIndex = 0;
        if (dropIndex > size) dropIndex = size;

        // Search backward from dropIndex-1
        for (int i = dropIndex - 1; i >= 0; i--) {
            Task t = list.getModel().getElementAt(i);
            if (t != null && t.getParentId() == null) return i;
        }
        // If not found, search forward from dropIndex
        for (int i = dropIndex; i < size; i++) {
            Task t = list.getModel().getElementAt(i);
            if (t != null && t.getParentId() == null) return i;
        }
        return -1;
    }

    private int computeInsertOffsetWithinParent(JList<Task> list, int parentIndex, int dropIndex) {
        int size = list.getModel().getSize();
        if (parentIndex < 0 || parentIndex >= size) return 0;
        Task parent = list.getModel().getElementAt(parentIndex);
        if (parent == null) return 0;

        // Count existing subtasks immediately following the parent
        int subtaskCount = 0;
        int idx = parentIndex + 1;
        while (idx < size) {
            Task t = list.getModel().getElementAt(idx);
            if (t != null && parent.getId().equals(t.getParentId())) {
                subtaskCount++;
                idx++;
            } else {
                break;
            }
        }

        // Compute raw offset relative to first subtask slot
        int raw = dropIndex - (parentIndex + 1);
        if (raw < 0) return 0;
        if (raw > subtaskCount) return subtaskCount;
        return raw;
    }

    private static int getDropIndex(TransferSupport support) {
        JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
        if (dropLocation == null) {
            return 0;
        }
        int index = dropLocation.getIndex();
        // Ensure index is within valid bounds for the component
        JList<?> targetList = (JList<?>) support.getComponent();
        int maxIndex = targetList.getModel().getSize();
        if (index < 0) {
            return 0;
        }
        if (index > maxIndex) {
            return maxIndex;
        }
        return index;
    }
}