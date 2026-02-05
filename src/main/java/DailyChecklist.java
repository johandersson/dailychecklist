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
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

@SuppressWarnings("this-escape")
public class DailyChecklist {
    private static volatile DailyChecklist instance;
    private JFrame frame;
    private TaskManager checklistManager;
    private TaskUpdater taskUpdater;
    private SettingsManager settingsManager;
    private ChecklistPanel checklistPanel;
    private AddTaskPanel addTaskPanel;
    private CustomChecklistsOverviewPanel customChecklistsOverviewPanel;
    private TodayPanel todayPanel;
    private JTabbedPane tabbedPane;
    private ReminderQueue reminderQueue;
    private final java.util.Set<String> openedChecklists = new java.util.HashSet<>();
    private final java.util.Set<Reminder> shownReminders = new java.util.HashSet<>();
    private TaskRepository repository;
    private transient java.util.concurrent.ScheduledExecutorService reminderScheduler;

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
        todayPanel = new TodayPanel(checklistManager);
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

        // Start reminder check task using a scheduled executor
        reminderScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ReminderChecker");
            t.setDaemon(true);
            return t;
        });
        reminderScheduler.scheduleAtFixedRate(() -> {
            try {
                checkReminders();
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(DailyChecklist.class.getName()).log(java.util.logging.Level.SEVERE, "Error in reminder task", e);
            }
        }, 10, 10, java.util.concurrent.TimeUnit.SECONDS);

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
            // Compute a friendly display title: prefer task name when reminder targets a task
            String displayTitle = null;
            String breadcrumb = null;
            if (reminder.getTaskId() != null) {
                Task t = checklistManager.getTaskById(reminder.getTaskId());
                if (t != null) displayTitle = t.getName();
                if (t != null && t.getParentId() != null) {
                    Task parent = checklistManager.getTaskById(t.getParentId());
                    String parentName = parent != null ? parent.getName() : null;
                    if (t.getType() == TaskType.CUSTOM) {
                        String checklistDisplay = checklistManager.getChecklistNameById(t.getChecklistId());
                        if (checklistDisplay != null && parentName != null) {
                            breadcrumb = checklistDisplay + " > " + parentName;
                        } else if (parentName != null) {
                            breadcrumb = parentName;
                        }
                    } else {
                        if (parentName != null) breadcrumb = parentName;
                    }
                }
            }

            ReminderDialog dialog = new ReminderDialog(frame, reminder, displayTitle, breadcrumb,
                handleReminderOpen(reminder),
                handleReminderDismiss(reminder),
                handleReminderRemindLater(reminder),
                handleReminderRemindTomorrow(reminder),
                handleReminderMarkAsDone(reminder)
            );

            dialog.setVisible(true);
            // Notify queue that dialog was dismissed
            reminderQueue.onReminderDismissed();
        });
    }

    // Handlers extracted from the previous inline lambdas for clarity and testability
    private Runnable handleReminderOpen(Reminder reminder) {
        return () -> {
            tabbedPane.setSelectedIndex(1);
            String name = reminder.getChecklistName();
            if (name == null || name.trim().isEmpty()) name = "Unknown Checklist";
            openedChecklists.add(name);
            customChecklistsOverviewPanel.selectChecklistByName(name);
            if (reminder.getTaskId() != null) {
                Task task = checklistManager.getAllTasks().stream().filter(t -> reminder.getTaskId().equals(t.getId())).findFirst().orElse(null);
                if (task != null) jumpToTask(task);
            }
            frame.setVisible(true);
            frame.toFront();
            frame.requestFocus();
        };
    }

    private Runnable handleReminderDismiss(Reminder reminder) {
        return () -> {
            if (reminder.getTaskId() != null) {
                checklistManager.removeReminder(reminder);
            } else {
                List<Reminder> allReminders = checklistManager.getReminders();
                allReminders.stream().filter(r -> Objects.equals(r.getChecklistName(), reminder.getChecklistName())).forEach(checklistManager::removeReminder);
            }
        };
    }

    private Runnable handleReminderRemindLater(Reminder reminder) {
        return () -> rescheduleReminder(reminder, 15);
    }

    private Runnable handleReminderRemindTomorrow(Reminder reminder) {
        return () -> rescheduleReminderTomorrow(reminder);
    }

    private Runnable handleReminderMarkAsDone(Reminder reminder) {
        return () -> {
            if (reminder.getTaskId() != null) {
                markTaskDoneAndFocus(reminder);
            } else {
                markChecklistDoneAndFocus(reminder);
            }
        };
    }

    // --- Reminder mark-as-done helpers ---
    private void markTaskDoneAndFocus(Reminder reminder) {
        Task t = checklistManager.getTaskById(reminder.getTaskId());
        if (t != null && !t.isDone()) {
            t.setDone(true);
            t.setDoneDate(new Date(System.currentTimeMillis()));
            checklistManager.updateTask(t);
        }
        checklistManager.removeReminder(reminder);
        checklistPanel.updateTasks();
        customChecklistsOverviewPanel.updateTasks();
        if (frame != null) {
            frame.setVisible(true);
            frame.toFront();
            frame.requestFocus();
        }
    }

    private void markChecklistDoneAndFocus(Reminder reminder) {
        String checklistName = reminder.getChecklistName();
        removeRemindersForChecklist(checklistName);
        if (checklistName == null || checklistName.trim().isEmpty()) return;

        Checklist checklist = findChecklistByName(checklistName);
        if (checklist != null) {
            openedChecklists.add(checklist.getName());
            customChecklistsOverviewPanel.selectChecklistByName(checklist.getName());
            markAllTasksDoneInChecklist(checklist);
        }

        checklistPanel.updateTasks();
        customChecklistsOverviewPanel.updateTasks();
        focusAppWindow();
    }

    private void removeRemindersForChecklist(String checklistName) {
        if (checklistName == null) return;
        List<Reminder> allReminders = checklistManager.getReminders();
        allReminders.stream().filter(r -> Objects.equals(r.getChecklistName(), checklistName)).forEach(checklistManager::removeReminder);
    }

    private Checklist findChecklistByName(String name) {
        if (name == null) return null;
        return checklistManager.getCustomChecklists().stream().filter(c -> name.equals(c.getName())).findFirst().orElse(null);
    }

    private void markAllTasksDoneInChecklist(Checklist checklist) {
        List<Task> tasks = checklistManager.getTasks(TaskType.CUSTOM, checklist);
        for (Task task : tasks) {
            if (!task.isDone()) {
                task.setDone(true);
                task.setDoneDate(new Date(System.currentTimeMillis()));
                checklistManager.updateTask(task);
            }
        }
    }

    private void focusAppWindow() {
        if (frame != null) {
            frame.setVisible(true);
            frame.toFront();
            frame.requestFocus();
        }
    }

    private Checklist findChecklistById(String id) {
        if (id == null) return null;
        return checklistManager.getCustomChecklists().stream()
            .filter(c -> id.equals(c.getId()))
            .findFirst()
            .orElse(null);
    }

    private void activateCustomChecklist(Checklist checklist) {
        tabbedPane.setSelectedIndex(1);
        if (checklist != null) {
            openedChecklists.add(checklist.getName());
            customChecklistsOverviewPanel.selectChecklistByName(checklist.getName());
        }
    }

    private void scrollToTaskInCustomPanelWithRetry(String targetId, Task task) {
        if (targetId == null) return;
        CustomChecklistPanel panel = customChecklistsOverviewPanel.getPanelMap().get(targetId);
        if (panel != null) {
            panel.scrollToTask(task);
            return;
        }

        final int[] attempts = {0};
        final int maxAttempts = 5;
        javax.swing.Timer retryTimer = new javax.swing.Timer(50, null);
        retryTimer.addActionListener(evt -> {
            attempts[0]++;
            CustomChecklistPanel p = customChecklistsOverviewPanel.getPanelMap().get(targetId);
            if (p != null) {
                p.scrollToTask(task);
                retryTimer.stop();
            } else if (attempts[0] >= maxAttempts) {
                retryTimer.stop();
            }
        });
        retryTimer.setRepeats(true);
        retryTimer.start();
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
            newTime.getMinute(),
            originalReminder.getTaskId()
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
            originalReminder.getMinute(),
            originalReminder.getTaskId()
        );
        checklistManager.removeReminder(originalReminder);
        checklistManager.addReminder(newReminder);
    }

    private void initializeUI() {
        frame.setTitle("Daily Checklist");
        frame.setSize(1400, 900);

        // Set the application icon (centralized and cached)
        frame.setIconImage(IconCache.getAppIcon());
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException e) {
            java.util.logging.Logger.getLogger(DailyChecklist.class.getName()).log(java.util.logging.Level.SEVERE, "Error updating look and feel", e);
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

    

    private void addTabbedPane() {
        tabbedPane = new JTabbedPane();
        JSplitPane dailySplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, checklistPanel, addTaskPanel);
        dailySplit.setResizeWeight(0.7);
        dailySplit.setDividerSize(4);
        dailySplit.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tabbedPane.add("Checklist", dailySplit);
        tabbedPane.add("Custom checklists", customChecklistsOverviewPanel);
        JScrollPane todayScrollPane = new javax.swing.JScrollPane(todayPanel);
        todayPanel.setScrollPaneContainer(todayScrollPane);
        tabbedPane.add("Today", todayScrollPane);
        frame.add(tabbedPane);
        // (No forced white backgrounds here) keep platform defaults for window backgrounds.
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

    /**
     * Returns the singleton application instance, or null if not initialized.
     */
    public static DailyChecklist getInstance() {
        return instance;
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
        if (task == null) return;
        String checklistId = task.getChecklistId();
        if (checklistId == null || checklistId.trim().isEmpty()) {
            jumpToDailyTask(task);
        } else {
            jumpToCustomTask(task);
        }
    }

    // --- Jump helpers (improve separation of concerns) ---
    private void jumpToDailyTask(Task task) {
        // Switch to main checklist tab and ensure weekday tasks are visible when needed
        tabbedPane.setSelectedIndex(0);
        if (task.getWeekday() != null) {
            String taskWeekday = task.getWeekday().toLowerCase();
            String currentWeekday = java.time.LocalDateTime.now().getDayOfWeek().toString().toLowerCase();
            if (!taskWeekday.equals(currentWeekday) && !checklistPanel.isShowWeekdayTasks()) {
                checklistPanel.setShowWeekdayTasks(true);
            }
        }
        checklistPanel.scrollToTask(task);
    }

    private void jumpToCustomTask(Task task) {
        Checklist checklist = findChecklistById(task.getChecklistId());
        if (checklist == null) return;

        activateCustomChecklist(checklist);
        scrollToTaskInCustomPanelWithRetry(checklist.getId(), task);
    }

    /**
     * Switches to the Custom Checklists tab and selects the checklist with given name.
     */
    public void showCustomChecklist(String checklistName) {
        if (checklistName == null) return;
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            tabbedPane.setSelectedIndex(1);
            openedChecklists.add(checklistName);
        }
        customChecklistsOverviewPanel.selectChecklistByName(checklistName);
    }

    public void shutdown() {
        if (repository != null) {
            repository.shutdown();
        }
        if (reminderQueue != null) {
            try {
                reminderQueue.shutdown();
            } catch (Exception ignore) {}
        }
        if (reminderScheduler != null) {
            try {
                reminderScheduler.shutdownNow();
            } catch (Exception ignore) {}
        }
    }
}

