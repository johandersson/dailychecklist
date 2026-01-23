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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        JDialog dialog = new JDialog((java.awt.Frame) null, "Search Tasks", true);
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

        JList<Task> resultList = new JList<>();
        CheckboxListCellRenderer renderer = new CheckboxListCellRenderer(true); // Show checklist info in search results
        resultList.setCellRenderer(renderer);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setSelectionBackground(new java.awt.Color(184, 207, 229)); // Consistent selection color
        resultList.setSelectionForeground(java.awt.Color.BLACK);
        
        // Add double-click listener to jump to task
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Task selected = resultList.getSelectedValue();
                    if (selected != null) {
                        dailyChecklist.jumpToTask(selected);
                        dialog.dispose();
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(resultList);

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
            resultList.setListData(results.toArray(new Task[0]));
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
        JButton goToButton = new JButton("Go to Selected Task");
        goToButton.setFont(FontManager.getButtonFont());
        JButton closeButton = new JButton("Close");
        closeButton.setFont(FontManager.getButtonFont());
        buttonPanel.add(goToButton);
        buttonPanel.add(closeButton);

        dialog.add(searchPanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch.run();
            }
        });

        goToButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Task selected = resultList.getSelectedValue();
                if (selected != null) {
                    dailyChecklist.jumpToTask(selected);
                    dialog.dispose();
                }
            }
        });

        closeButton.addActionListener(e -> dialog.dispose());

        dialog.setSize(640, 420);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}