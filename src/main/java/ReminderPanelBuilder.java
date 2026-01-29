import java.awt.Component;
import java.awt.FlowLayout;
import java.time.Duration;
import java.time.LocalDateTime;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 * Builds the reminder status UI for checklist panels.
 */
public final class ReminderPanelBuilder {
    private ReminderPanelBuilder() {}

    public static void buildReminderPanelUI(JPanel panel, Reminder r, String extraInfo, TaskManager taskManager, Runnable updateAllPanels, Runnable updateTasks, Component parentForDialogs) {
        panel.removeAll();
        if (r == null) {
            JLabel noReminderLabel = new JLabel("No reminder set");
            noReminderLabel.setFont(FontManager.getSmallFont());
            noReminderLabel.setForeground(java.awt.Color.GRAY);
            panel.add(noReminderLabel, java.awt.BorderLayout.WEST);
            return;
        }

        ReminderClockIcon.State state = computeState(r);
        JLabel textLabel = createTextLabel(extraInfo, state);
        String tip = tooltipFor(r);
        java.awt.event.MouseAdapter ma = createPopupMouseAdapter(r, taskManager, updateAllPanels, updateTasks, parentForDialogs);
        textLabel.addMouseListener(ma);

        JPanel small = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        small.setOpaque(false);
        javax.swing.JLabel iconLabel = createIconLabel(r, state, tip, ma);
        textLabel.setToolTipText(tip);
        small.add(iconLabel);
        small.add(textLabel);
        panel.add(small, java.awt.BorderLayout.WEST);
    }

    private static void showReminderPopup(java.awt.event.MouseEvent e, Reminder reminder, TaskManager taskManager, Runnable updateAllPanels, Runnable updateTasks, Component parentForDialogs) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem removeItem = new JMenuItem("Remove reminder");
        removeItem.addActionListener(ae -> {
            int res = javax.swing.JOptionPane.showConfirmDialog(parentForDialogs, "Remove this reminder?", "Confirm", javax.swing.JOptionPane.YES_NO_OPTION);
            if (res == javax.swing.JOptionPane.YES_OPTION) {
                taskManager.removeReminder(reminder);
                if (updateAllPanels != null) updateAllPanels.run();
                if (updateTasks != null) updateTasks.run();
            }
        });
        menu.add(removeItem);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private static ReminderClockIcon.State computeState(Reminder r) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime remTime = LocalDateTime.of(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
        if (remTime.isBefore(now)) {
            return Duration.between(remTime, now).toHours() > 1 ? ReminderClockIcon.State.VERY_OVERDUE : ReminderClockIcon.State.OVERDUE;
        } else if (remTime.isBefore(now.plusMinutes(60))) {
            return ReminderClockIcon.State.DUE_SOON;
        } else {
            return ReminderClockIcon.State.FUTURE;
        }
    }

    private static JLabel createTextLabel(String extraInfo, ReminderClockIcon.State state) {
        String labelText = (extraInfo != null) ? ("Reminder for: " + extraInfo) : "Reminder set";
        JLabel textLabel = new JLabel(labelText);
        textLabel.setFont(FontManager.getSmallMediumFont());
        java.awt.Color dueSoonColor = new java.awt.Color(204, 102, 0);
        switch (state) {
            case OVERDUE -> textLabel.setForeground(java.awt.Color.RED);
            case DUE_SOON -> textLabel.setForeground(dueSoonColor);
            default -> textLabel.setForeground(java.awt.Color.BLUE);
        }
        return textLabel;
    }

    private static String tooltipFor(Reminder r) {
        String tip = String.format("Reminder: %04d-%02d-%02d %02d:%02d", r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute());
        return "<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>" + tip + "</p></html>";
    }

    private static javax.swing.JLabel createIconLabel(Reminder r, ReminderClockIcon.State state, String tooltip, java.awt.event.MouseAdapter ma) {
        javax.swing.Icon icon = IconCache.getReminderClockIcon(r.getHour(), r.getMinute(), state, false);
        javax.swing.JLabel iconLabel = new javax.swing.JLabel(icon);
        iconLabel.setToolTipText(tooltip);
        iconLabel.addMouseListener(ma);
        return iconLabel;
    }

    private static java.awt.event.MouseAdapter createPopupMouseAdapter(Reminder r, TaskManager taskManager, Runnable updateAllPanels, Runnable updateTasks, Component parentForDialogs) {
        return new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) { if (SwingUtilities.isRightMouseButton(e)) showReminderPopup(e, r, taskManager, updateAllPanels, updateTasks, parentForDialogs); }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) { if (SwingUtilities.isRightMouseButton(e)) showReminderPopup(e, r, taskManager, updateAllPanels, updateTasks, parentForDialogs); }
        };
    }
}
