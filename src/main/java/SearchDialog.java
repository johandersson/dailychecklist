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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

public class SearchDialog {

    public static void showSearchDialog(java.awt.Component parent, TaskManager taskManager, DailyChecklist dailyChecklist) {
        new SearchDialogUI(parent, taskManager, dailyChecklist).show();
    }
}


class SearchDialogUI {
    private final java.awt.Component parent;
    private final TaskManager taskManager;
    private final SearchController searchController;
    private final DailyChecklist dailyChecklist;
    private JDialog dialog;
    private SearchPanel searchPanel;
    private SearchButtonPanel buttonPanel;
    private SearchListPanel listPanel;

    SearchDialogUI(java.awt.Component parent, TaskManager taskManager, DailyChecklist dailyChecklist) {
        this.parent = parent;
        this.taskManager = taskManager;
        this.dailyChecklist = dailyChecklist;
        this.searchController = new SearchController(taskManager);
        buildUI();
    }

    private void buildUI() {
        dialog = new JDialog((java.awt.Frame) null, "Search", true);
        dialog.setIconImage(IconCache.getAppIcon());
        dialog.setLayout(new BorderLayout());

        searchPanel = new SearchPanel();
        buttonPanel = new SearchButtonPanel();
        listPanel = new SearchListPanel();

        listPanel.unifiedList.setCellRenderer(createCellRenderer());
        listPanel.unifiedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listPanel.unifiedList.setSelectionBackground(new java.awt.Color(184, 207, 229));
        listPanel.unifiedList.setSelectionForeground(new java.awt.Color(0, 0, 0));
        listPanel.unifiedList.setFixedCellHeight(50);

        dialog.add(searchPanel, BorderLayout.NORTH);
        dialog.add(listPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        addListeners();

        dialog.setSize(640, 420);
        dialog.setLocationRelativeTo(parent);
    }

    private javax.swing.ListCellRenderer<Object> createCellRenderer() {
        CheckboxListCellRenderer taskRenderer = new CheckboxListCellRenderer(true, taskManager);
        taskRenderer.setShowSubtaskBreadcrumb(true);
        taskRenderer.setShowAddSubtaskIcon(false);
        final JList<Task> taskListTemplate = new JList<>();
        return (list, value, index, isSelected, cellHasFocus) -> {
            if (value instanceof Task) {
                Task t = (Task) value;
                taskListTemplate.setSelectionBackground(list.getSelectionBackground());
                taskListTemplate.setSelectionForeground(list.getSelectionForeground());
                return taskRenderer.getListCellRendererComponent(taskListTemplate, t, index, isSelected, cellHasFocus);
            } else if (value instanceof Checklist) {
                return createChecklistLabel((Checklist) value, isSelected);
            } else {
                return new javax.swing.JLabel(value == null ? "" : value.toString());
            }
        };
    }

    private javax.swing.JLabel createChecklistLabel(Checklist c, boolean isSelected) {
        javax.swing.JLabel lbl = new javax.swing.JLabel();
        lbl.setFont(FontManager.getTaskListFont());
        lbl.setOpaque(true);
        lbl.setText(c.getName());
        java.awt.Color selBg = new java.awt.Color(184, 207, 229);
        lbl.setBackground(isSelected ? selBg : java.awt.Color.WHITE);
        lbl.setForeground(java.awt.Color.BLACK);
        javax.swing.Icon icon = IconCache.getChecklistDocumentIcon();
        lbl.setIcon(icon);
        lbl.setIconTextGap(6);
        int leftInset = Math.max(2, 40 - (icon.getIconWidth() + lbl.getIconTextGap()) - 3);
        lbl.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, leftInset, 0, 0));
        lbl.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        return lbl;
    }

    private void addListeners() {
        Runnable performSearch = this::performSearch;
        setupSearchFieldListeners(performSearch);
        setupListListeners(performSearch);
        setupButtonListeners();
    }

    private void setupSearchFieldListeners(Runnable performSearch) {
        searchPanel.searchButton.addActionListener(e -> performSearch.run());
        searchPanel.searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { performSearch.run(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { performSearch.run(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { performSearch.run(); }
        });
    }

    private void setupListListeners(Runnable performSearch) {
        listPanel.unifiedList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = listPanel.unifiedList.locationToIndex(e.getPoint());
                if (index < 0) return;
                Object value = listPanel.unifiedModel.getElementAt(index);
                if (value instanceof Task) {
                    int cellHeight = listPanel.unifiedList.getFixedCellHeight();
                    int yInCell = e.getY() - (index * cellHeight);
                    int checkboxX = UiLayout.CHECKBOX_X, checkboxY = cellHeight / 2 - UiLayout.CHECKBOX_SIZE / 2, checkboxSize = UiLayout.CHECKBOX_SIZE;
                    int x = e.getX();
                    if (x >= checkboxX && x <= checkboxX + checkboxSize &&
                        yInCell >= checkboxY && yInCell <= checkboxY + checkboxSize) {
                        Task t = (Task) value;
                        // Persist the change via TaskManager so all registered panels refresh
                        t.setDone(!t.isDone());
                        try {
                            taskManager.updateTaskImmediate(t);
                        } catch (Exception ignore) {
                            // If persistence fails, still refresh search view to reflect optimistic change
                        }
                        performSearch.run();
                        return;
                    }
                    if (e.getClickCount() == 2) {
                        dailyChecklist.jumpToTask((Task) value);
                        dialog.dispose();
                    }
                } else if (value instanceof Checklist) {
                    if (e.getClickCount() == 2) {
                        dailyChecklist.showCustomChecklist(((Checklist) value).getName());
                        dialog.dispose();
                    }
                }
            }
        });

        listPanel.unifiedList.addListSelectionListener(e -> {
            boolean enabled = listPanel.unifiedList.getSelectedValue() != null;
            buttonPanel.goToButton.setEnabled(enabled);
        });
    }

    private void setupButtonListeners() {
        buttonPanel.goToButton.addActionListener((var e) -> {
            Object sel = listPanel.unifiedList.getSelectedValue();
            if (sel == null) {
                javax.swing.JOptionPane.showMessageDialog(dialog, "Please select an item to open.", "No selection", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (sel instanceof Task) {
                dailyChecklist.jumpToTask((Task) sel);
                dialog.dispose();
            } else if (sel instanceof Checklist) {
                dailyChecklist.showCustomChecklist(((Checklist) sel).getName());
                dialog.dispose();
            }
        });

        buttonPanel.closeButton.addActionListener(e -> dialog.dispose());
    }

    private void performSearch() {
        String query = searchPanel.searchField.getText();
        boolean includeAllWeekday = searchPanel.searchAllWeekdayBox.isSelected();
        searchController.requestUpdateModel(query, includeAllWeekday, listPanel.unifiedModel);
    }

    public void show() {
        performSearch();
        dialog.setVisible(true);
    }
}