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
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.undo.UndoManager;

@SuppressWarnings("serial")
public class CustomChecklistsOverviewPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JList<Checklist> checklistList;
    private DefaultListModel<Checklist> listModel;
    private final transient TaskManager taskManager;
    private final transient Runnable updateTasks;
    private JTextArea newChecklistField;
    private JButton createButton;
    private JSplitPane splitPane;
    private JPanel rightPanel;
    private CustomChecklistPanel currentChecklistPanel;
    private Checklist selectedChecklist;
    private AddTaskPanel currentAddPanel;
    private final Set<Checklist> allChecklists;
    private final Map<String, CustomChecklistPanel> panelMap = new HashMap<>();

    @SuppressWarnings("this-escape")
    public CustomChecklistsOverviewPanel(TaskManager taskManager, Runnable updateTasks) {
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        this.allChecklists = new java.util.HashSet<>();
        initialize();
        // Listen for model changes and refresh overview
        taskManager.addTaskChangeListener(() -> javax.swing.SwingUtilities.invokeLater(this::updateTasks));
    }

    private void initialize() {
        listModel = new DefaultListModel<>();
        checklistList = new JList<>(listModel) {
            private static final long serialVersionUID = 1L;
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int idx = locationToIndex(e.getPoint());
                if (idx < 0) return super.getToolTipText(e);
                java.awt.Rectangle cb = getCellBounds(idx, idx);
                if (cb == null) return super.getToolTipText(e);
                int relX = e.getX() - cb.x;
                int cellW = cb.width;
                // IconListCellRenderer reserves RIGHT_ICON_SPACE on right
                int rightAreaStart = cellW - IconListCellRenderer.RIGHT_ICON_SPACE;
                Checklist c = getModel().getElementAt(idx);
                if (c != null) {
                    // Find nearest reminder for this checklist
                    Reminder nearest = null;
                    if (taskManager != null) {
                        java.util.List<Reminder> reminders = taskManager.getReminders();
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        long bestDiff = Long.MAX_VALUE;
                        for (Reminder r : reminders) {
                            if (!java.util.Objects.equals(r.getChecklistName(), c.getName())) continue;
                            java.time.LocalDateTime dt = java.time.LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
                            long diff = Math.abs(java.time.Duration.between(now, dt).toMinutes());
                            if (diff < bestDiff) { bestDiff = diff; nearest = r; }
                        }
                    }
                    if (nearest != null) {
                        // Compute actual icon bounds so tooltip triggers where the icon is painted
                        javax.swing.Icon icon = IconCache.getReminderClockIcon(nearest.getHour(), nearest.getMinute(), ReminderClockIcon.State.FUTURE, true);
                        int iconW = icon != null ? icon.getIconWidth() : IconListCellRenderer.RIGHT_ICON_SPACE;
                        int iconStart = cellW - iconW - 6;
                        if (relX >= iconStart) {
                            String txt = String.format("Reminder: %04d-%02d-%02d %02d:%02d", nearest.getYear(), nearest.getMonth(), nearest.getDay(), nearest.getHour(), nearest.getMinute());
                            return "<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>" + txt + "</p></html>";
                        }
                    }
                }
                return super.getToolTipText(e);
            }
        };
        checklistList.setCellRenderer(new ChecklistCellRenderer(taskManager));
        checklistList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(checklistList);
        checklistList.setSelectionBackground(new java.awt.Color(184, 207, 229)); // Same as task lists
        checklistList.setSelectionForeground(java.awt.Color.BLACK);
        checklistList.setTransferHandler(new ChecklistListTransferHandler(
            listModel, taskManager, this::updateTasks,
            name -> javax.swing.SwingUtilities.invokeLater(() -> selectChecklistByName(name))
        ));
        checklistList.setDropMode(DropMode.ON);
        checklistList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Checklist selected = checklistList.getSelectedValue();
                selectChecklist(selected);
            }
        });
        checklistList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int index = checklistList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        // If clicked element is not selected, make it the sole selection.
                        if (!checklistList.isSelectedIndex(index)) {
                            checklistList.setSelectedIndex(index);
                        }
                        selectedChecklist = checklistList.getSelectedValue();
                        showChecklistPopup(e.getX(), e.getY());
                    }
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int index = checklistList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        if (!checklistList.isSelectedIndex(index)) {
                            checklistList.setSelectedIndex(index);
                        }
                        selectedChecklist = checklistList.getSelectedValue();
                        showChecklistPopup(e.getX(), e.getY());
                    }
                }
            }
        });

        newChecklistField = new JTextArea(3, 20);
        newChecklistField.setLineWrap(true);
        newChecklistField.setWrapStyleWord(true);
        newChecklistField.setFont(FontManager.getTaskListFont());
        newChecklistField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(java.awt.Color.GRAY, 1),
            javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        
        // Add undo/redo support
        final javax.swing.undo.UndoManager undoManager = new javax.swing.undo.UndoManager();
        newChecklistField.getDocument().addUndoableEditListener(undoManager);
        
        // Bind Ctrl+Z for undo and Ctrl+Y for redo
        newChecklistField.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("control Z"), "undo");
        newChecklistField.getActionMap().put("undo", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });
        
        newChecklistField.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("control Y"), "redo");
        newChecklistField.getActionMap().put("redo", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        });
        
        JScrollPane newChecklistScroll = new JScrollPane(newChecklistField);
        newChecklistScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        newChecklistScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        
        createButton = new JButton("Create Checklist(s)");
        createButton.addActionListener(e -> createNewChecklists());
        createButton.setToolTipText("Create one or more checklists (one per line)");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(newChecklistScroll, BorderLayout.CENTER);
        topPanel.add(createButton, BorderLayout.EAST);
        // Flatter look: reduce extra titled/etched borders and padding
        topPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(topPanel, BorderLayout.NORTH);
        javax.swing.JScrollPane leftScroll = new JScrollPane(checklistList);
        leftScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        leftPanel.add(leftScroll, BorderLayout.CENTER);
        leftPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));

        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));

        javax.swing.JScrollPane rightScroll = new JScrollPane(rightPanel);
        rightScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightScroll);
        splitPane.setResizeWeight(0.3);
        splitPane.setDividerSize(4);
        splitPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
        
        // Initialize with existing checklists (load off EDT)
        loadChecklistsInBackground();
    }

    private void loadChecklistsInBackground() {
        javax.swing.SwingWorker<java.util.Set<Checklist>, Void> worker = new javax.swing.SwingWorker<>() {
            @Override
            protected java.util.Set<Checklist> doInBackground() throws Exception {
                return taskManager.getCustomChecklists();
            }

            @Override
            protected void done() {
                try {
                    java.util.Set<Checklist> checklists = get();
                    if (checklists != null) {
                        allChecklists.clear();
                        allChecklists.addAll(checklists);
                    }
                    updateChecklistList();
                } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                    java.util.logging.Logger.getLogger(CustomChecklistsOverviewPanel.class.getName()).log(java.util.logging.Level.SEVERE, "Error loading checklists", e);
                }
            }
        };
        worker.execute();
    }

    private void createNewChecklists() {
        String input = newChecklistField.getText();
        if (input == null || input.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter at least one checklist name.");
            return;
        }
        
        String[] lines = input.split("\\n");
        
        // Limit to prevent excessive batch additions
        final int MAX_BATCH_CHECKLISTS = 100;
        if (lines.length > MAX_BATCH_CHECKLISTS) {
            JOptionPane.showMessageDialog(this, 
                "Too many lines (" + lines.length + "). Maximum allowed is " + MAX_BATCH_CHECKLISTS + " checklists at once.",
                "Limit Exceeded", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Set<Checklist> existing = taskManager.getCustomChecklists();
        java.util.List<Checklist> newChecklists = new java.util.ArrayList<>();
        java.util.List<String> skipped = new java.util.ArrayList<>();
        
        for (String line : lines) {
            String rawName = line.trim();
            if (rawName.isEmpty()) {
                continue; // Skip empty lines
            }
            
            String name = TaskManager.validateInputWithError(rawName, "Checklist name");
            if (name == null) {
                skipped.add(rawName + " (invalid name)");
                continue;
            }
            
            // Check if name already exists
            boolean nameExists = existing.stream().anyMatch(c -> name.equals(c.getName())) ||
                                 newChecklists.stream().anyMatch(c -> name.equals(c.getName()));
            if (nameExists) {
                skipped.add(name + " (already exists)");
                continue;
            }
            
            // Remove any orphaned tasks with this name
            List<Task> allTasks = taskManager.getAllTasks();
            for (Task task : allTasks) {
                if (task.getChecklistId() != null) {
                    Checklist taskChecklist = taskManager.getCustomChecklists().stream()
                        .filter(c -> task.getChecklistId().equals(c.getId()))
                        .findFirst().orElse(null);
                    if (taskChecklist != null && name.equals(taskChecklist.getName())) {
                        taskManager.removeTask(task);
                    }
                }
            }
            
            Checklist newChecklist = new Checklist(name);
            allChecklists.add(newChecklist);
            taskManager.addChecklist(newChecklist);
            newChecklists.add(newChecklist);
            existing.add(newChecklist); // Add to existing set for duplicate checking
        }
        
        newChecklistField.setText("");
        
        if (!newChecklists.isEmpty()) {
            updateChecklistList();
            // Select the last created checklist
            Checklist lastCreated = newChecklists.get(newChecklists.size() - 1);
            selectChecklist(lastCreated);
            updateTasks.run();
            checklistList.setSelectedValue(lastCreated, true);
            checklistList.revalidate();
            checklistList.repaint();
            
            String message = "Created " + newChecklists.size() + " checklist(s)";
            if (!skipped.isEmpty()) {
                message += "\n\nSkipped " + skipped.size() + " item(s):\n" + 
                          String.join("\n", skipped.subList(0, Math.min(10, skipped.size())));
                if (skipped.size() > 10) {
                    message += "\n... and " + (skipped.size() - 10) + " more";
                }
            }
            JOptionPane.showMessageDialog(this, message, "Checklists Created", JOptionPane.INFORMATION_MESSAGE);
        } else {
            String message = "No checklists were created";
            if (!skipped.isEmpty()) {
                message += "\n\nSkipped " + skipped.size() + " item(s):\n" + 
                          String.join("\n", skipped.subList(0, Math.min(10, skipped.size())));
                if (skipped.size() > 10) {
                    message += "\n... and " + (skipped.size() - 10) + " more";
                }
            }
            JOptionPane.showMessageDialog(this, message, "No Checklists Created", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Public method to select a checklist by name (used by reminders)
     */
    public void selectChecklistByName(String checklistName) {
        // Find checklist by name
        Checklist checklist = null;
        for (Checklist c : allChecklists) {
            if (checklistName.equals(c.getName())) {
                checklist = c;
                break;
            }
        }
        selectChecklist(checklist);
        if (checklist != null) {
            checklistList.setSelectedValue(checklist, true);
        }
    }

    private void selectChecklist(Checklist checklist) {
        selectedChecklist = checklist;
        if (currentChecklistPanel != null) {
            rightPanel.remove(currentChecklistPanel);
        }
        if (currentAddPanel != null) {
            rightPanel.remove(currentAddPanel);
        }
        if (checklist != null) {
            // Reuse existing panel from panelMap if available, otherwise create new one
            currentChecklistPanel = panelMap.get(checklist.getId());
            if (currentChecklistPanel == null) {
                currentChecklistPanel = new CustomChecklistPanel(taskManager, checklist, this::updateTasks);
                panelMap.put(checklist.getId(), currentChecklistPanel);
            }
            currentChecklistPanel.updateTasks();
            rightPanel.add(currentChecklistPanel);
            currentAddPanel = new AddTaskPanel(taskManager, tasks -> {
                if (currentChecklistPanel != null) {
                    currentChecklistPanel.updateTasks();
                    rightPanel.revalidate();
                    rightPanel.repaint();
                }
                updateTasks.run();
            }, selectedChecklist != null ? selectedChecklist.getName() : null);
            rightPanel.add(currentAddPanel);
        } else {
            currentChecklistPanel = null;
            currentAddPanel = null;
        }
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    public void updateTasks() {
        scheduleUpdate();
    }

    private volatile boolean updateScheduled = false;

    private void scheduleUpdate() {
        if (updateScheduled) return;
        updateScheduled = true;
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                doUpdateTasks();
            } finally {
                updateScheduled = false;
            }
        });
    }

    private void doUpdateTasks() {
        // Preserve selection
        Checklist previousSelection = checklistList.getSelectedValue();

        updateChecklistList();

        // Restore selection
        if (previousSelection != null && listModel.contains(previousSelection)) {
            checklistList.setSelectedValue(previousSelection, true);
        }

        if (currentChecklistPanel != null) {
            currentChecklistPanel.updateTasks();
            currentChecklistPanel.requestSelectionFocus();
        }
        // Update all panels to reflect any changes
        for (CustomChecklistPanel panel : panelMap.values()) {
            if (panel != null) panel.updateTasks();
        }
        checklistList.revalidate();
        checklistList.repaint();
    }

    private void updateChecklistList() {
        java.util.List<Checklist> desired = new java.util.ArrayList<>(allChecklists);
        TaskUpdater.syncModel(listModel, desired);

        // Clean up panelMap - remove panels for checklists that no longer exist
        panelMap.keySet().removeIf(checklistId -> allChecklists.stream().noneMatch(c -> checklistId.equals(c.getId())));
    }

    private void showChecklistPopup(int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        java.util.List<Checklist> selected = checklistList.getSelectedValuesList();
        int selCount = selected == null ? 0 : selected.size();

        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.setEnabled(selCount == 1);
        renameItem.addActionListener(e -> renameChecklist());
        menu.add(renameItem);

        JMenuItem deleteItem = new JMenuItem(selCount > 1 ? "Delete Selected" : "Delete");
        deleteItem.addActionListener(e -> deleteSelectedChecklists(selected));
        menu.add(deleteItem);

        // Reminder menu only valid for single selection
        if (selCount == 1 && selected.get(0) != null) {
            Checklist single = selected.get(0);
            
            // Add Copy Outline option
            menu.addSeparator();
            JMenuItem copyOutlineItem = new JMenuItem("Copy Outline to Clipboard");
            copyOutlineItem.addActionListener(e -> copyChecklistOutline(single));
            menu.add(copyOutlineItem);
            menu.addSeparator();
            
            boolean hasReminderForSelected = taskManager.getReminders().stream().anyMatch(r -> r.getChecklistName().equals(single.getName()));
            JMenuItem addReminderItem = new JMenuItem(hasReminderForSelected ? "Edit Reminder" : "Set Reminder");
            addReminderItem.addActionListener(e -> setReminder());
            menu.add(addReminderItem);
            if (taskManager.getReminders().stream().anyMatch(r -> r.getChecklistName().equals(single.getName()))) {
                JMenuItem removeReminderItem = new JMenuItem("Remove Reminder");
                removeReminderItem.addActionListener(e -> {
                    List<Reminder> allReminders = taskManager.getReminders();
                    List<Reminder> toRemove = allReminders.stream()
                            .filter(r -> r.getChecklistName().equals(single.getName()))
                            .toList();
                    if (toRemove.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "No reminders to remove for '" + single.getName() + "'.");
                        return;
                    }
                    int res = JOptionPane.showConfirmDialog(this, "Remove reminder(s) for '" + single.getName() + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (res == JOptionPane.YES_OPTION) {
                        toRemove.forEach(taskManager::removeReminder);
                        if (rightPanel != null && rightPanel.getComponentCount() > 0) {
                            java.awt.Component c = rightPanel.getComponent(0);
                            if (c instanceof CustomChecklistPanel panel) {
                                panel.getTaskList().requestFocusInWindow();
                            }
                        }
                    }
                });
                menu.add(removeReminderItem);
            }
        }

        menu.show(checklistList, x, y);
    }

    private void renameChecklist() {
        if (selectedChecklist == null) return;
        String oldName = selectedChecklist.getName();
        String rawNewName = JOptionPane.showInputDialog(this, "Enter new name:", oldName);
        String newName = TaskManager.validateInputWithError(rawNewName, "Checklist name");
        if (newName != null && !newName.equals(oldName)) {
            // Check if name already exists
            boolean nameExists = allChecklists.stream().anyMatch(c -> newName.equals(c.getName()) && !selectedChecklist.getId().equals(c.getId()));
            if (nameExists) {
                JOptionPane.showMessageDialog(this, "Checklist name already exists.");
                return;
            }

            // Update the checklist name
            taskManager.getCustomChecklists().stream()
                .filter(c -> selectedChecklist.getId().equals(c.getId()))
                .findFirst()
                .ifPresent(c -> taskManager.updateChecklistName(c, newName));

            // Also update reminders
            List<Reminder> reminders = taskManager.getReminders();
            for (Reminder r : reminders) {
                if (r.getChecklistName().equals(oldName)) {
                    taskManager.removeReminder(r);
                    Reminder newReminder = new Reminder(newName, r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
                    taskManager.addReminder(newReminder);
                }
            }
            updateTasks.run();
            // Update panelMap key
            if (panelMap.containsKey(selectedChecklist.getId())) {
                CustomChecklistPanel panel = panelMap.remove(selectedChecklist.getId());
                panelMap.put(selectedChecklist.getId(), panel);
            }
            selectChecklist(selectedChecklist);
        }
    }

    private void deleteChecklist() {
        if (selectedChecklist == null) return;
        String name = selectedChecklist.getName();

        // Determine if checklist contains any tasks. Only offer "Move to" options when there are tasks.
        boolean hasTasks = taskManager.getAllTasks().stream().anyMatch(t -> selectedChecklist.getId().equals(t.getChecklistId()));

        Object[] options;
        if (hasTasks) {
            options = new Object[]{"Delete list", "Move to morning", "Move to evening", "Cancel"};
        } else {
            options = new Object[]{"Delete list", "Cancel"};
        }

        int defaultOption = options.length - 1; // Cancel index
        int choice = JOptionPane.showOptionDialog(this, "What to do with the tasks in '" + name + "'?", "Delete Checklist", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[defaultOption]);

        if (choice < 0 || choice == defaultOption) {
            // User closed dialog or chose Cancel
            return;
        }

        if (!hasTasks) {
            // Only possible choice is Delete list (index 0)
            if (choice == 0) {
                // Delete list: remove any tasks (none expected) defensively
                List<Task> allTasks = taskManager.getAllTasks();
                java.util.List<Task> toRemove = new java.util.ArrayList<>();
                for (Task task : allTasks) {
                    if (task.getChecklistId() != null && task.getChecklistId().equals(selectedChecklist.getId())) {
                        toRemove.add(task);
                    }
                }
                for (Task t : toRemove) taskManager.removeTask(t);
            }
        } else {
            switch (choice) {
                case 0 -> {
                    // Delete list
                    List<Task> allTasks = taskManager.getAllTasks();
                    java.util.List<Task> toRemove = new java.util.ArrayList<>();
                    for (Task task : allTasks) {
                        if (task.getChecklistId() != null && task.getChecklistId().equals(selectedChecklist.getId())) {
                            toRemove.add(task);
                        }
                    }
                    for (Task t : toRemove) taskManager.removeTask(t);
                }
                case 1 -> // Move to morning
                    moveTasksToType(selectedChecklist.getId(), TaskType.MORNING);
                case 2 -> // Move to evening
                    moveTasksToType(selectedChecklist.getId(), TaskType.EVENING);
                default -> {
                    return;
                }
            }
        }

        // Remove all reminders for this checklist
        List<Reminder> allReminders = taskManager.getReminders();
        allReminders.stream()
            .filter(reminder -> Objects.equals(reminder.getChecklistName(), name))
            .forEach(taskManager::removeReminder);

        allChecklists.remove(selectedChecklist);  // Remove from tracked checklists
        taskManager.removeChecklist(selectedChecklist);  // Remove from persistent storage
        panelMap.remove(selectedChecklist.getId());  // Remove panel from cache
        updateTasks();  // Refresh the local checklist list
        updateTasks.run();  // Update other panels
        // After deletion, select the first checklist if available
        if (listModel.size() > 0) {
            Checklist firstChecklist = listModel.get(0);
            selectChecklist(firstChecklist);
            checklistList.setSelectedValue(firstChecklist, true);
        } else {
            selectChecklist(null);
            checklistList.clearSelection();
        }
    }

        private void deleteSelectedChecklists(java.util.List<Checklist> selected) {
            if (selected == null || selected.isEmpty()) return;
            java.util.Set<String> ids = new java.util.HashSet<>();
            java.util.Set<String> names = new java.util.HashSet<>();
            for (Checklist c : selected) { if (c != null) { ids.add(c.getId()); names.add(c.getName()); } }

                // Always delete tasks belonging to the selected custom checklists.
                // Do not offer options to move tasks to Morning/Evening when deleting custom lists.
                boolean single = selected.size() == 1;
                String title = single ? "Delete Checklist" : "Delete Checklists";
                String prompt = single
                    ? "Delete the selected checklist. Tasks contained in it will also be deleted."
                    : "Delete the selected checklists. Tasks contained in them will also be deleted.";
                String deleteLabel = single ? "Delete list" : "Delete lists";
                int choice = JOptionPane.showOptionDialog(this,
                    prompt,
                    title,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[] { deleteLabel, "Cancel" },
                    "Cancel");
                if (choice != 0) return;

            List<Task> allTasks = taskManager.getAllTasks();
            java.util.List<Task> toRemove = new java.util.ArrayList<>();
            for (Task task : allTasks) {
                if (task.getChecklistId() != null && ids.contains(task.getChecklistId())) {
                    toRemove.add(task);
                }
            }
            for (Task t : toRemove) taskManager.removeTask(t);

            // Remove reminders for deleted checklists
            removeRemindersForChecklists(names);

            for (Checklist c : selected) {
                if (c == null) continue;
                allChecklists.remove(c);
                taskManager.removeChecklist(c);
                panelMap.remove(c.getId());
            }

            updateTasks();
            updateTasks.run();

            if (listModel.size() > 0) {
                Checklist firstChecklist = listModel.get(0);
                selectChecklist(firstChecklist);
                checklistList.setSelectedValue(firstChecklist, true);
            } else {
                selectChecklist(null);
                checklistList.clearSelection();
            }
        }

        private void moveTasksToTypeMultiple(java.util.Set<String> checklistIds, TaskType type) {
            List<Task> allTasks = taskManager.getAllTasks();
            for (Task task : allTasks) {
                if (task.getChecklistId() != null && checklistIds.contains(task.getChecklistId())) {
                    task.setType(type);
                    task.setChecklistId(null);
                    task.setDone(false);
                    task.setDoneDate(null);
                    taskManager.updateTask(task);
                }
            }
        }

        private void removeRemindersForChecklists(java.util.Set<String> checklistNames) {
            if (checklistNames == null || checklistNames.isEmpty()) return;
            List<Reminder> allReminders = taskManager.getReminders();
            for (Reminder r : new java.util.ArrayList<>(allReminders)) {
                if (checklistNames.contains(r.getChecklistName())) {
                    taskManager.removeReminder(r);
                }
            }
        }

    private void moveTasksToType(String checklistId, TaskType type) {
        List<Task> allTasks = taskManager.getAllTasks();
        for (Task task : allTasks) {
            if (task.getChecklistId() != null && task.getChecklistId().equals(checklistId)) {
                task.setType(type);
                task.setChecklistId(null);
                task.setDone(false);
                task.setDoneDate(null);
                taskManager.updateTask(task);
            }
        }
    }

    private void setReminder() {
        if (selectedChecklist == null) return;

        // Check if a reminder already exists for this checklist
        List<Reminder> allReminders = taskManager.getReminders();
        Reminder existingReminder = allReminders.stream()
                .filter(r -> r.getChecklistName().equals(selectedChecklist.getName()))
                .findFirst()
                .orElse(null);

        // Save logical selection state
        final Checklist checklistToRestore = selectedChecklist;
        final String selectedTaskId;
        String _tmpSelectedTaskId = null;
                if (rightPanel != null && rightPanel.getComponentCount() > 0) {
            java.awt.Component c = rightPanel.getComponent(0);
                if (c instanceof CustomChecklistPanel panel) {
                    try {
                        Task sel = panel.getTaskList().getSelectedValue();
                        _tmpSelectedTaskId = sel == null ? null : sel.getId();
                    } catch (Exception ignore) {}
                }
            }
        selectedTaskId = _tmpSelectedTaskId;

        ReminderEditDialog dialog = new ReminderEditDialog(taskManager, selectedChecklist.getName(), existingReminder, null);
        dialog.setVisible(true);

        // After dialog returns (modal), reapply selection and focus
        if (checklistToRestore != null) {
            checklistList.setSelectedValue(checklistToRestore, true);
            // Restore selection and focus on right panel's task list if available
                if (rightPanel != null && rightPanel.getComponentCount() > 0) {
                java.awt.Component c = rightPanel.getComponent(0);
                if (c instanceof CustomChecklistPanel panel) {
                    JList<Task> list = panel.getTaskList();
                    if (selectedTaskId != null) {
                        // Try to restore selected task by id
                        for (int i = 0; i < list.getModel().getSize(); i++) {
                            Task t = list.getModel().getElementAt(i);
                            if (t != null && t.getId() != null && t.getId().equals(selectedTaskId)) {
                                list.setSelectedIndex(i);
                                list.ensureIndexIsVisible(i);
                                break;
                            }
                        }
                    }
                    list.requestFocusInWindow();
                }
            }
        }
    }
    
    /**
     * Copies the entire outline of a checklist (including tasks and subtasks) to the clipboard.
     */
    private void copyChecklistOutline(Checklist checklist) {
        if (checklist == null) return;
        
        // Get all tasks for this checklist
        List<Task> allTasks = taskManager.getTasks(TaskType.CUSTOM, checklist);
        if (allTasks == null || allTasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Checklist '" + checklist.getName() + "' has no tasks.",
                "Empty Checklist",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Organize tasks: parents followed by their subtasks
        java.util.List<Task> parents = new java.util.ArrayList<>();
        for (Task t : allTasks) {
            if (t.getParentId() == null) {
                parents.add(t);
            }
        }
        
        StringBuilder outline = new StringBuilder();
        outline.append("# ").append(checklist.getName()).append("\n\n");
        
        int taskNumber = 1;
        for (Task parent : parents) {
            // Add parent task
            outline.append(taskNumber++).append(". ").append(parent.getName());
            if (parent.getNote() != null && !parent.getNote().trim().isEmpty()) {
                outline.append(" [Note: ").append(parent.getNote().trim()).append("]");
            }
            outline.append("\n");
            
            // Add subtasks under this parent
            java.util.List<Task> subtasks = taskManager.getSubtasksSorted(parent.getId());
            if (subtasks != null && !subtasks.isEmpty()) {
                char subLetter = 'a';
                for (Task subtask : subtasks) {
                    if (checklist.getId().equals(subtask.getChecklistId())) {
                        outline.append("   ").append(subLetter++).append(". ").append(subtask.getName());
                        if (subtask.getNote() != null && !subtask.getNote().trim().isEmpty()) {
                            outline.append(" [Note: ").append(subtask.getNote().trim()).append("]");
                        }
                        outline.append("\n");
                    }
                }
            }
            outline.append("\n");
        }
        
        // Copy to clipboard
        try {
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(outline.toString());
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            
            JOptionPane.showMessageDialog(this,
                "Copied outline with " + parents.size() + " task(s) to clipboard!",
                "Outline Copied",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Failed to copy outline to clipboard: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public Map<String, CustomChecklistPanel> getPanelMap() {
        return panelMap;
    }
}