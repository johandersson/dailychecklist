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
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class DailyChecklist {
    private static JFrame frame;
    private static TaskManager checklistManager;
    private TaskUpdater taskUpdater;
    private SettingsManager settingsManager;
    private ChecklistPanel checklistPanel;
    private AddTaskPanel addTaskPanel;
    private static CustomChecklistsOverviewPanel customChecklistsOverviewPanel;
    private static JTabbedPane tabbedPane;
    private static FocusTimer focusTimerInstance = FocusTimer.getInstance();
    private static boolean reminderDialogShowing = false;
    private static Queue<Reminder> pendingReminders = new ConcurrentLinkedQueue<>();

    public DailyChecklist() {
        settingsManager = new SettingsManager();
        settingsManager.load();
        taskUpdater = new TaskUpdater();
        initializeTaskManager();
        if (!GraphicsEnvironment.isHeadless()) {
            frame = new JFrame();
            initializeUI();
        }
        checklistPanel = new ChecklistPanel(checklistManager, taskUpdater);
        addTaskPanel = new AddTaskPanel(checklistManager, () -> {
            checklistPanel.updateTasks();
            customChecklistsOverviewPanel.updateTasks();
        });
        customChecklistsOverviewPanel = new CustomChecklistsOverviewPanel(checklistManager, () -> {
            checklistPanel.updateTasks();
        });
        if (!GraphicsEnvironment.isHeadless()) {
            addTabbedPane();
            frame.setJMenuBar(MenuBarBuilder.build(frame, checklistManager, () -> {
                checklistPanel.updateTasks();
                customChecklistsOverviewPanel.updateTasks();
            }));
            setTitleWithDate();
        }
        checklistPanel.setShowWeekdayTasks(settingsManager.getShowWeekdayTasks());
        checklistPanel.updateTasks();
        customChecklistsOverviewPanel.updateTasks();
        if (!GraphicsEnvironment.isHeadless()) {
            KeyBindingManager.bindKeys(frame.getRootPane(), frame, checklistManager, () -> {
                checklistPanel.updateTasks();
                customChecklistsOverviewPanel.updateTasks();
            });
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowActivated(WindowEvent e) {
                    settingsManager.checkDateAndUpdate(checklistManager, () -> {
                        checklistPanel.updateTasks();
                        customChecklistsOverviewPanel.updateTasks();
                    }, checklistPanel.isShowWeekdayTasks());
                }
            });
        }

        // Start reminder check thread
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // Check every 10 seconds
                    List<Reminder> reminders = checklistManager.getReminders();
                    LocalDateTime now = LocalDateTime.now();
                    for (Reminder r : reminders) {
                        LocalDateTime reminderTime = LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
                        if (!reminderTime.isAfter(now)) { // Time has passed or is now
                            // Only show reminders from the last 5 minutes to avoid showing very old ones
                            // Automatically remove reminders older than 1 hour
                            if (now.minusMinutes(5).isBefore(reminderTime)) {
                                // Add reminder to queue instead of showing immediately
                                pendingReminders.add(r);
                                showNextReminder();
                            } else if (now.minusHours(1).isAfter(reminderTime)) {
                                // Automatically remove reminders older than 1 hour
                                checklistManager.removeReminder(r);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Shows the next reminder from the queue if no dialog is currently showing
     */
    private static void showNextReminder() {
        if (reminderDialogShowing || pendingReminders.isEmpty()) {
            return;
        }

        Reminder r = pendingReminders.poll(); // Remove from queue
        if (r == null) return;

        SwingUtilities.invokeLater(() -> {
            // Prevent multiple reminder dialogs
            reminderDialogShowing = true;

            // Create always-on-top reminder dialog
            JDialog reminderDialog = new JDialog(frame, "⏰ Reminder", true);
            reminderDialog.setAlwaysOnTop(true);
            reminderDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            reminderDialog.setResizable(false);

            String checklistName = r.getChecklistName();
            if (checklistName == null || checklistName.trim().isEmpty()) {
                checklistName = "Unknown Checklist";
            }

            // Format the reminder time nicely
            String timeString = String.format("%02d:%02d", r.getHour(), r.getMinute());
            String dateString = String.format("%d/%d/%d", r.getMonth(), r.getDay(), r.getYear());

            // Create HTML-formatted message with better styling
            String htmlMessage = "<html><body style='font-family: Arial, sans-serif; font-size: 12px; padding: 10px;'>" +
                "<div style='text-align: center; margin-bottom: 15px;'>" +
                "<h2 style='color: #2E86AB; margin: 0 0 5px 0; font-size: 16px;'>⏰ Reminder</h2>" +
                "<div style='font-size: 14px; font-weight: bold; color: #333; margin-bottom: 8px;'>" + checklistName + "</div>" +
                "<div style='color: #666; font-size: 11px;'>Scheduled for: " + timeString + " on " + dateString + "</div>" +
                "</div>" +
                "<div style='text-align: center; color: #555; font-style: italic;'>" +
                "It's time to check your tasks!" +
                "</div>" +
                "</body></html>";

            JLabel messageLabel = new JLabel(htmlMessage);
            messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            JButton openButton = new JButton("📂 Open Checklist");
            openButton.setToolTipText("Open the checklist in the application");
            JButton doneButton = new JButton("✅ Done");
            doneButton.setToolTipText("Mark this reminder as completed");

            openButton.addActionListener(e -> {
                // Switch to custom checklists tab
                tabbedPane.setSelectedIndex(1);
                String name = r.getChecklistName();
                if (name == null || name.trim().isEmpty()) {
                    name = "Unknown Checklist";
                }
                customChecklistsOverviewPanel.selectChecklistByName(name);
                frame.setVisible(true);
                frame.toFront();
                frame.requestFocus();
                reminderDialogShowing = false;
                reminderDialog.dispose();
                // Show next reminder in queue after a short delay
                SwingUtilities.invokeLater(() -> {
                    try {
                        Thread.sleep(500); // Small delay to prevent rapid-fire dialogs
                        showNextReminder();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                });
            });

            doneButton.addActionListener(e -> {
                checklistManager.removeReminder(r);
                reminderDialogShowing = false;
                reminderDialog.dispose();
                // Show next reminder in queue after a short delay
                SwingUtilities.invokeLater(() -> {
                    try {
                        Thread.sleep(500); // Small delay to prevent rapid-fire dialogs
                        showNextReminder();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                });
            });

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            buttonPanel.add(openButton);
            buttonPanel.add(doneButton);

            reminderDialog.setLayout(new BorderLayout());
            reminderDialog.add(messageLabel, BorderLayout.CENTER);
            reminderDialog.add(buttonPanel, BorderLayout.SOUTH);

            reminderDialog.pack();
            reminderDialog.setLocationRelativeTo(frame);
            reminderDialog.setVisible(true);
        });
    }

    private void initializeUI() {
        frame.setTitle("Daily Checklist");
        frame.setSize(1400, 900);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setUIFonts("Yu Gothic UI", 16);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
    }

    private void initializeTaskManager() {
        checklistManager = new TaskManager(new XMLTaskRepository());
    }

    private void addTabbedPane() {
        tabbedPane = new JTabbedPane();
        JSplitPane dailySplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, checklistPanel, addTaskPanel);
        dailySplit.setResizeWeight(0.7);
        tabbedPane.add("Checklist", dailySplit);
        tabbedPane.add("Custom checklists", customChecklistsOverviewPanel);
        frame.add(tabbedPane);
    }

    private void setTitleWithDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        frame.setTitle("Daily Checklist - " + sdf.format(new Date()));
    }

    static void setUIFonts(String fontName, int fontSize) {
        UIManager.put("Label.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("Button.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("TextField.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("RadioButton.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("CheckBox.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("TabbedPane.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("TitledBorder.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("List.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("TextArea.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("TextPane.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("TextField.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("ComboBox.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("Menu.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("MenuItem.font", new Font(fontName, Font.PLAIN, fontSize));
        UIManager.put("ToolTip.font", new Font(fontName, Font.PLAIN, fontSize));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DailyChecklist app = new DailyChecklist();
            app.setVisible(true);
        });
    }

    public void setVisible(boolean visible) {
        if (frame != null) {
            frame.setVisible(visible);
        }
    }

    public void bringToFront() {
        if (frame != null) {
            frame.setExtendedState(JFrame.NORMAL);
            frame.toFront();
            frame.requestFocus();
        }
    }
}

