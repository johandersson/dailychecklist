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
    private final DefaultListModel<Task> morningListModel;
    private final DefaultListModel<Task> eveningListModel;

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
        this.morningListModel = morningListModel;
        this.eveningListModel = eveningListModel;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        int index = list.getSelectedIndex();
        if (index >= 0) {
            Task task = listModel.get(index);
            return new StringSelection(checklistName + ";" + task.getId());
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
        int index = list.getSelectedIndex();
        if (index >= 0) {
            Task task = listModel.get(index);
            return createDragImage(task);
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

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support) || !support.isDrop()) {
            return false;
        }

        int dropIndex = getDropIndex(support);

        try {
            String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            String[] parts = data.split(";", 2);
            if (parts.length != 2) {
                return false;
            }
            String sourceChecklistName = parts[0];
            String taskId = parts[1];

            // Find the task by ID
            Task task = findTaskById(taskId);
            if (task == null) {
                return false;
            }

            // Determine source model
            DefaultListModel<Task> sourceModel = null;
            if ("MORNING".equals(sourceChecklistName)) {
                sourceModel = morningListModel;
            } else if ("EVENING".equals(sourceChecklistName)) {
                sourceModel = eveningListModel;
            }

            if (sourceChecklistName.equals(checklistName)) {
                // Reordering within the same checklist
                // Find the current index of the task
                int currentIndex = -1;
                for (int i = 0; i < listModel.getSize(); i++) {
                    if (listModel.get(i).getId().equals(taskId)) {
                        currentIndex = i;
                        break;
                    }
                }
                if (currentIndex >= 0) {
                    if (currentIndex < dropIndex) {
                        dropIndex--;
                    }
                    listModel.remove(currentIndex);
                    listModel.add(dropIndex, task);
                    
                    // Persist the new order in the underlying data
                    persistTaskOrder(listModel, checklistName);
                    
                    return true;
                }
            } else {
                // Moving from a different checklist
                boolean isSourceDaily = "MORNING".equals(sourceChecklistName) || "EVENING".equals(sourceChecklistName);
                boolean isTargetDaily = "MORNING".equals(checklistName) || "EVENING".equals(checklistName);
                if (isSourceDaily != isTargetDaily) {
                    return false; // Disallow moving between daily and custom checklists
                }

                // Update the task's properties
                if (("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) && !("MORNING".equals(sourceChecklistName) || "EVENING".equals(sourceChecklistName))) {
                    // Moving to daily from custom
                    task.setChecklistName(null);
                    task.setType("MORNING".equals(checklistName) ? TaskType.MORNING : TaskType.EVENING);
                } else if (!("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) && ("MORNING".equals(sourceChecklistName) || "EVENING".equals(sourceChecklistName))) {
                    // Moving to custom from daily
                    task.setChecklistName(checklistName);
                    task.setType(TaskType.CUSTOM);
                } else if (("MORNING".equals(sourceChecklistName) || "EVENING".equals(sourceChecklistName)) && ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) && !sourceChecklistName.equals(checklistName)) {
                    // Moving between morning and evening
                    task.setChecklistName(null);
                    task.setType("MORNING".equals(checklistName) ? TaskType.MORNING : TaskType.EVENING);
                } else if (!("MORNING".equals(sourceChecklistName) || "EVENING".equals(sourceChecklistName)) && !("MORNING".equals(checklistName) || "EVENING".equals(checklistName))) {
                    // Moving between custom checklists
                    task.setChecklistName(checklistName);
                }

                taskManager.updateTask(task);
                // Restore focus to the target list for custom checklists
                if (!("MORNING".equals(checklistName) || "EVENING".equals(checklistName))) {
                    list.requestFocusInWindow();
                }
                return true;
            }
        } catch (UnsupportedFlavorException | IOException e) {
            return false;
        }
        return false;
    }

    private void persistTaskOrder(DefaultListModel<Task> listModel, String checklistName) {
        List<Task> allTasks = new ArrayList<>(taskManager.getAllTasks());
        
        // Find the tasks that belong to this checklist in their current order
        List<Task> checklistTasks = new ArrayList<>();
        for (Task t : allTasks) {
            boolean belongsToChecklist = false;
            if ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) {
                belongsToChecklist = checklistName.equals(t.getType().toString());
            } else {
                belongsToChecklist = checklistName.equals(t.getChecklistName());
            }
            if (belongsToChecklist) {
                checklistTasks.add(t);
            }
        }
        
        // Reorder checklistTasks to match listModel order
        List<Task> reorderedTasks = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            Task task = listModel.get(i);
            int indexInChecklistTasks = checklistTasks.indexOf(task);
            if (indexInChecklistTasks >= 0) {
                reorderedTasks.add(checklistTasks.get(indexInChecklistTasks));
            }
        }
        
        // Replace the tasks in allTasks with the reordered ones
        allTasks.removeAll(checklistTasks);
        // Find the position to insert (after the last task of the same type/checklist)
        int insertIndex = 0;
        for (Task t : allTasks) {
            boolean belongsToChecklist = false;
            if ("MORNING".equals(checklistName) || "EVENING".equals(checklistName)) {
                belongsToChecklist = checklistName.equals(t.getType().toString());
            } else {
                belongsToChecklist = checklistName.equals(t.getChecklistName());
            }
            if (belongsToChecklist) {
                insertIndex = allTasks.indexOf(t) + 1;
            }
        }
        allTasks.addAll(insertIndex, reorderedTasks);
        
        // Save the reordered tasks
        taskManager.setTasks(allTasks);
    }

    private Task findTaskById(String taskId) {
        return taskManager.getTaskById(taskId);
    }

    private static int getDropIndex(TransferSupport support) {
        JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
        return dropLocation.getIndex();
    }
}