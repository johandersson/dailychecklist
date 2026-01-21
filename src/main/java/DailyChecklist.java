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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class DailyChecklist {
    private JFrame frame;
    private TaskManager checklistManager;
    private TaskUpdater taskUpdater;
    private SettingsManager settingsManager;
    private ChecklistPanel checklistPanel;
    private AddTaskPanel addTaskPanel;
    private CustomChecklistsOverviewPanel customChecklistsOverviewPanel;
    private JTabbedPane tabbedPane;
    private ReminderQueue reminderQueue;
    private java.util.Set<String> openedChecklists = new java.util.HashSet<>();
    private java.util.Set<Reminder> shownReminders = new java.util.HashSet<>();
    private TaskRepository repository;

    public DailyChecklist() {
        initializeComponents(null);
    }

    public DailyChecklist(ApplicationLifecycleManager lifecycleManager) {
        initializeComponents(lifecycleManager);
    }

    public TaskRepository getRepository() {
        return repository;
    }

    private void initializeComponents(ApplicationLifecycleManager lifecycleManager) {
        if (lifecycleManager != null) {
            // Use provided lifecycle manager components
            this.repository = lifecycleManager.getRepository();
            this.settingsManager = lifecycleManager.getSettingsManager();
        } else {
            // Create components manually
            this.settingsManager = new SettingsManager();
            this.settingsManager.load();
            this.repository = new XMLTaskRepository();
            this.repository.initialize();
            this.repository.start();
        }

        this.taskUpdater = new TaskUpdater();
        this.checklistManager = new TaskManager(repository);
        this.checklistPanel = new ChecklistPanel(checklistManager, taskUpdater);
        this.checklistPanel.updateTasks();

        if (!GraphicsEnvironment.isHeadless()) {
            frame = new JFrame();
            // Update settings manager with frame as parent component
            this.settingsManager = new SettingsManager(frame);
            this.settingsManager.load();
            
            // Set parent component for repository error dialogs
            if (repository instanceof XMLTaskRepository) {
                ((XMLTaskRepository) repository).setParentComponent(frame);
            }
            
            initializeUI();
        }
        addTaskPanel = new AddTaskPanel(checklistManager, tasks -> {
            checklistPanel.updateTasks();
            checklistPanel.scrollToAndHighlightTasks(tasks);
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
                    // Use optimized method to get only due reminders
                    List<Reminder> dueReminders = checklistManager.getDueReminders(5, openedChecklists);
                    LocalDateTime now = LocalDateTime.now();
                    
                    for (Reminder r : dueReminders) {
                        // Only add reminders that haven't been shown in this session
                        if (!shownReminders.contains(r)) {
                            reminderQueue.addReminder(r);
                            shownReminders.add(r);
                        }
                        
                        // Also check for old reminders to clean up (less frequent check)
                        LocalDateTime reminderTime = LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
                        if (now.minusHours(1).isAfter(reminderTime)) {
                            checklistManager.removeReminder(r);
                            shownReminders.remove(r); // Clean up from shown reminders too
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
     * Shows a reminder dialog using the ReminderDialog class.
     */
    private void showReminderDialog(Reminder reminder) {
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
                    // Remove all reminders for this checklist
                    List<Reminder> allReminders = checklistManager.getReminders();
                    allReminders.stream()
                        .filter(r -> Objects.equals(r.getChecklistName(), reminder.getChecklistName()))
                        .forEach(checklistManager::removeReminder);
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
    private void rescheduleReminder(Reminder originalReminder, int minutesLater) {
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
    private void rescheduleReminderTomorrow(Reminder originalReminder) {
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

        // Set the application icon
        frame.setIconImage(createAppIcon());

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
        repository = new XMLTaskRepository(frame);
        checklistManager = new TaskManager(repository);
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

    public JFrame getFrame() {
        return frame;
    }

    /**
     * Creates a programmatic icon that looks like the checked checkbox from the app.
     */
    private Image createAppIcon() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = image.createGraphics();

        // Enable anti-aliasing
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // Clear background to transparent
        g2.setComposite(java.awt.AlphaComposite.Clear);
        g2.fillRect(0, 0, size, size);
        g2.setComposite(java.awt.AlphaComposite.SrcOver);

        // Define checkbox dimensions (centered)
        int checkboxSize = 24;
        int checkboxX = (size - checkboxSize) / 2;
        int checkboxY = (size - checkboxSize) / 2;

        // Draw subtle shadow
        g2.setColor(new Color(200, 200, 200, 100));
        g2.fillRoundRect(checkboxX + 1, checkboxY + 1, checkboxSize, checkboxSize, 6, 6);

        // Draw checkbox outline
        g2.setColor(new Color(120, 120, 120));
        g2.drawRoundRect(checkboxX, checkboxY, checkboxSize, checkboxSize, 6, 6);

        // Fill checkbox with white
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(checkboxX + 1, checkboxY + 1, checkboxSize - 2, checkboxSize - 2, 6, 6);

        // Draw checkmark
        g2.setColor(new Color(76, 175, 80)); // Material green
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int offsetX = checkboxX + 3;
        int offsetY = checkboxY + 6;
        g2.drawLine(offsetX + 2, offsetY + 6, offsetX + 7, offsetY + 11);
        g2.drawLine(offsetX + 7, offsetY + 11, offsetX + 15, offsetY + 1);

        g2.dispose();
        return image;
    }

    public void shutdown() {
        if (repository != null) {
            repository.shutdown();
        }
    }
}

