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
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

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
                    return true;
                }
            } else {
                // Moving from a different checklist
                boolean isSourceDaily = "MORNING".equals(sourceChecklistName) || "EVENING".equals(sourceChecklistName);
                boolean isTargetDaily = "MORNING".equals(checklistName) || "EVENING".equals(checklistName);
                if (isSourceDaily != isTargetDaily) {
                    return false; // Disallow moving between daily and custom checklists
                }

                // Remove from source model if applicable
                if (sourceModel != null) {
                    int currentIndex = -1;
                    for (int i = 0; i < sourceModel.getSize(); i++) {
                        if (sourceModel.get(i).getId().equals(taskId)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    if (currentIndex >= 0) {
                        sourceModel.remove(currentIndex);
                    }
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
                // Add to the target list
                listModel.add(dropIndex, task);
                // Update all panels to reflect the move
                if (updateAllPanels != null) {
                    updateAllPanels.run();
                }
                return true;
            }
            return false;
        } catch (UnsupportedFlavorException | IOException e) {
            return false;
        }
    }

    private Task findTaskById(String taskId) {
        return taskManager.getTaskById(taskId);
    }

    private static int getDropIndex(TransferSupport support) {
        JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
        return dropLocation.getIndex();
    }
}