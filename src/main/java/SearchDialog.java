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
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

public class SearchDialog {
    public static void showSearchDialog(java.awt.Component parent, TaskManager taskManager, DailyChecklist dailyChecklist) {
        JDialog dialog = new JDialog((java.awt.Frame) null, "Search", true);
        dialog.setIconImage(DailyChecklist.createAppIcon());
        dialog.setLayout(new BorderLayout());

        JPanel searchPanel = new JPanel(new FlowLayout());
        JTextField searchField = new JTextField(28);
        searchField.setFont(FontManager.getTaskListFont());
        JButton searchButton = new JButton("Search");
        searchButton.setFont(FontManager.getButtonFont());
        javax.swing.JCheckBox searchAllWeekdayBox = new javax.swing.JCheckBox("Include all weekday tasks");
        searchAllWeekdayBox.setToolTipText("When checked, weekday-specific tasks for any weekday will be included in results.");
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(searchAllWeekdayBox);

        // Unified results list (tasks and custom lists together)
        javax.swing.DefaultListModel<Object> unifiedModel = new javax.swing.DefaultListModel<>();
        JList<Object> unifiedList = new JList<>(unifiedModel);

        CheckboxListCellRenderer taskRenderer = new CheckboxListCellRenderer(true); // Show checklist info in search results
        ChecklistCellRenderer checklistRenderer = new ChecklistCellRenderer(taskManager);

        unifiedList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            if (value instanceof Task t) {
                return taskRenderer.getListCellRendererComponent((JList) list, t, index, isSelected, cellHasFocus);
            } else if (value instanceof Checklist c) {
                // Build a simple label component using checklist renderer but show document icon for search
                javax.swing.JLabel lbl = new javax.swing.JLabel();
                lbl.setFont(FontManager.getTaskListFont());
                lbl.setOpaque(true);
                lbl.setText(c.getName());
                java.awt.Color selBg = new java.awt.Color(184, 207, 229);
                lbl.setBackground(isSelected ? selBg : java.awt.Color.WHITE);
                lbl.setForeground(java.awt.Color.BLACK);
                // Always show the checklist document icon for search hits (no reminder clock/timestamp)
                javax.swing.Icon icon = IconCache.getChecklistDocumentIcon();
                lbl.setIcon(icon);
                // Slightly tighter gap to better horizontally align with task checkmark
                lbl.setIconTextGap(6);
                // Align checklist icon/text to match task list layout (task textStartX ~= 40)
                // JLabel layouts: leftInset + iconWidth + iconTextGap ~= textStartX
                // Nudge left slightly (-3) to better match checkbox alignment
                int leftInset = Math.max(2, 40 - (icon.getIconWidth() + lbl.getIconTextGap()) - 3);
                lbl.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, leftInset, 0, 0));
                lbl.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
                return lbl;
            } else {
                return new javax.swing.JLabel(value == null ? "" : value.toString());
            }
        });
        unifiedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        unifiedList.setSelectionBackground(new java.awt.Color(184, 207, 229)); // Consistent selection color
        unifiedList.setSelectionForeground(java.awt.Color.BLACK);
        // Ensure consistent vertical sizing for both checklist and task rows
        unifiedList.setFixedCellHeight(50);

        // (Open button enabling will be wired after the button is created below)
        
        // Double-click handler for unified list
        unifiedList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Object sel = unifiedList.getSelectedValue();
                    if (sel instanceof Task t) {
                        dailyChecklist.jumpToTask(t);
                        dialog.dispose();
                    } else if (sel instanceof Checklist c) {
                        dailyChecklist.showCustomChecklist(c.getName());
                        dialog.dispose();
                    }
                }
            }
        });
        
        // (previously used resultList) unifiedScroll will be used below

        Runnable performSearch = () -> {
            String query = searchField.getText().toLowerCase();
            boolean includeAllWeekday = searchAllWeekdayBox.isSelected();
            String currentWeekday = java.time.LocalDateTime.now().getDayOfWeek().toString().toLowerCase();
            List<Task> allTasks = taskManager.getAllTasks();
            List<Task> results = allTasks.stream()
                .filter(task -> task.getName().toLowerCase().contains(query))
                .filter(task -> {
                    if (task.getWeekday() == null) return true;
                    if (includeAllWeekday) return true;
                    return task.getWeekday().toLowerCase().equals(currentWeekday);
                })
                .collect(Collectors.toList());

            List<Checklist> allLists = taskManager.getCustomChecklists().stream()
                .filter(c -> c.getName().toLowerCase().contains(query))
                .collect(Collectors.toList());

            // Populate unified model: lists first, then tasks
            unifiedModel.clear();
            for (Checklist c : allLists) unifiedModel.addElement(c);
            for (Task t : results) unifiedModel.addElement(t);
        };

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { performSearch.run(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { performSearch.run(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { performSearch.run(); }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton goToButton = new JButton("Open");
        // Disabled until an item is selected
        goToButton.setEnabled(false);
        goToButton.setFont(FontManager.getButtonFont());
        JButton closeButton = new JButton("Close");
        closeButton.setFont(FontManager.getButtonFont());
        buttonPanel.add(goToButton);
        buttonPanel.add(closeButton);

        // Now wire selection -> button enable/disable
        unifiedList.addListSelectionListener(e -> {
            boolean enabled = unifiedList.getSelectedValue() != null;
            goToButton.setEnabled(enabled);
        });

        JScrollPane unifiedScroll = new JScrollPane(unifiedList);
        unifiedScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        dialog.add(searchPanel, BorderLayout.NORTH);
        dialog.add(unifiedScroll, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> performSearch.run());

        goToButton.addActionListener(e -> {
            Object sel = unifiedList.getSelectedValue();
            if (sel == null) {
                javax.swing.JOptionPane.showMessageDialog(dialog, "Please select an item to open.", "No selection", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (sel instanceof Task t) {
                dailyChecklist.jumpToTask(t);
                dialog.dispose();
            } else if (sel instanceof Checklist c) {
                dailyChecklist.showCustomChecklist(c.getName());
                dialog.dispose();
            }
        });

        closeButton.addActionListener(e -> dialog.dispose());

        dialog.setSize(640, 420);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}