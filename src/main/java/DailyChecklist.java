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
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
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
    private static ReminderQueue reminderQueue;
    private static java.util.Set<String> openedChecklists = new java.util.HashSet<>();

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

        // Initialize reminder queue
        reminderQueue = new ReminderQueue(reminder -> showReminderDialog(reminder));

        // Start reminder check thread
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // Check every 10 seconds
                    List<Reminder> reminders = checklistManager.getReminders();
                    LocalDateTime now = LocalDateTime.now();
                    for (Reminder r : reminders) {
                        // Skip reminders for checklists that have been opened in this session
                        String checklistName = r.getChecklistName();
                        if (checklistName != null && openedChecklists.contains(checklistName)) {
                            continue;
                        }
                        
                        LocalDateTime reminderTime = LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
                        if (!reminderTime.isAfter(now)) { // Time has passed or is now
                            // Only show reminders from the last 5 minutes to avoid showing very old ones
                            // Automatically remove reminders older than 1 hour
                            if (now.minusMinutes(5).isBefore(reminderTime)) {
                                reminderQueue.addReminder(r);
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
    /**
     * Shows a reminder dialog using the ReminderDialog class.
     */
    private static void showReminderDialog(Reminder reminder) {
        SwingUtilities.invokeLater(() -> {
            ReminderDialog dialog = new ReminderDialog(frame, reminder,
                // On Open action
                () -> {
                    // Switch to custom checklists tab
                    tabbedPane.setSelectedIndex(1);
                    String name = reminder.getChecklistName();
                    if (name == null || name.trim().isEmpty()) {
                        name = "Unknown Checklist";
                    }
                    // Mark this checklist as opened for this session
                    openedChecklists.add(name);
                    customChecklistsOverviewPanel.selectChecklistByName(name);
                    frame.setVisible(true);
                    frame.toFront();
                    frame.requestFocus();
                },
                // On Done action
                () -> {
                    checklistManager.removeReminder(reminder);
                },
                // On Remind Later (15 minutes)
                () -> {
                    rescheduleReminder(reminder, 15);
                },
                // On Remind Tomorrow
                () -> {
                    rescheduleReminderTomorrow(reminder);
                }
            );

            dialog.setVisible(true);
            // Notify queue that dialog was dismissed
            reminderQueue.onReminderDismissed();
        });
    }

    /**
     * Reschedules a reminder to occur in the specified number of minutes from now.
     */
    private static void rescheduleReminder(Reminder originalReminder, int minutesLater) {
        LocalDateTime newTime = LocalDateTime.now().plusMinutes(minutesLater);
        Reminder newReminder = new Reminder(
            originalReminder.getChecklistName(),
            newTime.getYear(),
            newTime.getMonthValue(),
            newTime.getDayOfMonth(),
            newTime.getHour(),
            newTime.getMinute()
        );
        checklistManager.removeReminder(originalReminder);
        checklistManager.addReminder(newReminder);
    }

    /**
     * Reschedules a reminder to occur tomorrow at the same time.
     */
    private static void rescheduleReminderTomorrow(Reminder originalReminder) {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        Reminder newReminder = new Reminder(
            originalReminder.getChecklistName(),
            tomorrow.getYear(),
            tomorrow.getMonthValue(),
            tomorrow.getDayOfMonth(),
            originalReminder.getHour(),
            originalReminder.getMinute()
        );
        checklistManager.removeReminder(originalReminder);
        checklistManager.addReminder(newReminder);
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

