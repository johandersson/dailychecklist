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
    private static volatile DailyChecklist instance;
    private JFrame frame;
    private TaskManager checklistManager;
    private TaskUpdater taskUpdater;
    private SettingsManager settingsManager;
    private ChecklistPanel checklistPanel;
    private AddTaskPanel addTaskPanel;
    private CustomChecklistsOverviewPanel customChecklistsOverviewPanel;
    private JTabbedPane tabbedPane;
    private ReminderQueue reminderQueue;
    private final java.util.Set<String> openedChecklists = new java.util.HashSet<>();
    private final java.util.Set<Reminder> shownReminders = new java.util.HashSet<>();
    private TaskRepository repository;

    public DailyChecklist() {
        initializeComponents(null);
        instance = this;
    }

    public DailyChecklist(ApplicationLifecycleManager lifecycleManager) {
        initializeComponents(lifecycleManager);
        instance = this;
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
            if (repository instanceof XMLTaskRepository xmlRepo) {
                xmlRepo.setParentComponent(frame);
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
            }, this));
            setTitleWithDate();
        }
        checklistPanel.setShowWeekdayTasks(settingsManager.getShowWeekdayTasks());
        checklistPanel.updateTasks();
        customChecklistsOverviewPanel.updateTasks();
        if (!GraphicsEnvironment.isHeadless()) {
            KeyBindingManager.bindKeys(frame.getRootPane(), frame, checklistManager, () -> {
                checklistPanel.updateTasks();
                customChecklistsOverviewPanel.updateTasks();
            }, this);
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
                    checkReminders();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    java.util.logging.Logger.getLogger(DailyChecklist.class.getName()).log(java.util.logging.Level.SEVERE, "Error in background thread", e);
                }
            }
        }).start();

        // Initial check for due reminders at startup
        checkReminders();
    }

    /**
     * Checks for due reminders and adds them to the queue if not already shown.
     */
    private void checkReminders() {
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
    }

    /**
     * Shows a reminder dialog using the ReminderDialog class.
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
                // On Dismiss action (old Done action)
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
                },
                // On Mark as Done action
                () -> {
                    // Remove all reminders for this checklist
                    List<Reminder> allReminders = checklistManager.getReminders();
                    allReminders.stream()
                        .filter(r -> Objects.equals(r.getChecklistName(), reminder.getChecklistName()))
                        .forEach(checklistManager::removeReminder);
                    
                    // Switch to custom checklists tab
                    tabbedPane.setSelectedIndex(1);
                    final String checklistName = reminder.getChecklistName();
                    if (checklistName == null || checklistName.trim().isEmpty()) {
                        // Skip reminders with no checklist name - do nothing
                        return;
                    }
                    // Mark this checklist as opened for this session
                    Checklist checklist = checklistManager.getCustomChecklists().stream()
                        .filter(c -> checklistName.equals(c.getName()))
                        .findFirst()
                        .orElse(null);
                    if (checklist != null) {
                        openedChecklists.add(checklist.getName());
                        customChecklistsOverviewPanel.selectChecklistByName(checklist.getName());
                        
                        // Mark all tasks in this checklist as done
                        List<Task> tasks = checklistManager.getTasks(TaskType.CUSTOM, checklist);
                        for (Task task : tasks) {
                            if (!task.isDone()) {
                                task.setDone(true);
                                task.setDoneDate(new Date(System.currentTimeMillis()));
                                checklistManager.updateTask(task);
                            }
                        }
                    }
                    
                    // Refresh the UI
                    checklistPanel.updateTasks();
                    customChecklistsOverviewPanel.updateTasks();
                    
                    frame.setVisible(true);
                    frame.toFront();
                    frame.requestFocus();
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
        Image appIcon = createAppIcon();
        frame.setIconImage(appIcon);
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
                    java.util.logging.Logger.getLogger(DailyChecklist.class.getName()).log(java.util.logging.Level.SEVERE, "Error updating reminders", e);
        }

        setUIFonts(FontManager.FONT_NAME, FontManager.SIZE_DEFAULT);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

            // No-op: main focus sink removed
    }

    /**
     * Request focus on the hidden main focus sink. Safe to call from dialogs.
     */
    public static void focusMainSink() {
        DailyChecklist inst = instance;
        if (inst == null) return;
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                java.awt.Container content = inst.frame.getContentPane();
                    for (java.awt.Component c : content.getComponents()) {
                    if (c instanceof javax.swing.JButton && !c.isVisible()) {
                        c.requestFocusInWindow();
                        return;
                    }
                }
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(DailyChecklist.class.getName()).log(java.util.logging.Level.SEVERE, "Error during some operation", e);
            }
        });
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
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        UIManager.put("Label.font", font);
        UIManager.put("Button.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("RadioButton.font", font);
        UIManager.put("CheckBox.font", font);
        UIManager.put("TabbedPane.font", font);
        UIManager.put("TitledBorder.font", font);
        UIManager.put("List.font", font);
        UIManager.put("TextArea.font", font);
        UIManager.put("TextPane.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("ComboBox.font", font);
        UIManager.put("Menu.font", font);
        UIManager.put("MenuItem.font", font);
        UIManager.put("ToolTip.font", font);
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
    public static Image createAppIcon() {
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

    public void jumpToTask(Task task) {
        String checklistId = task.getChecklistId();
        if (checklistId == null || checklistId.trim().isEmpty()) {
            // Daily checklist
            tabbedPane.setSelectedIndex(0);
            // If this is a weekday-specific task that is not for today, enable "show all" so it is visible
            if (task.getWeekday() != null) {
                String taskWeekday = task.getWeekday().toLowerCase();
                String currentWeekday = java.time.LocalDateTime.now().getDayOfWeek().toString().toLowerCase();
                if (!taskWeekday.equals(currentWeekday) && !checklistPanel.isShowWeekdayTasks()) {
                    checklistPanel.setShowWeekdayTasks(true);
                }
            }
            checklistPanel.scrollToTask(task);
        } else {
            // Custom checklist - find the checklist by ID
            Checklist checklist = checklistManager.getCustomChecklists().stream()
                .filter(c -> checklistId.equals(c.getId()))
                .findFirst()
                .orElse(null);
            if (checklist != null) {
                tabbedPane.setSelectedIndex(1);
                openedChecklists.add(checklist.getName());
                customChecklistsOverviewPanel.selectChecklistByName(checklist.getName());
                // Get the panel and scroll
                CustomChecklistPanel panel = (CustomChecklistPanel) customChecklistsOverviewPanel.getPanelMap().get(checklist.getId());
                if (panel != null) {
                    panel.scrollToTask(task);
                }
            }
        }
    }

    public void shutdown() {
        if (repository != null) {
            repository.shutdown();
        }
    }
}

