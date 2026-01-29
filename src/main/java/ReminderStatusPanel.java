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
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ReminderStatusPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public ReminderStatusPanel(TaskManager taskManager, String title) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Preserve previous behavior: no header content above Morning or Evening
        if (title.equalsIgnoreCase("Morning") || title.equalsIgnoreCase("Evening")) {
            JPanel empty = new JPanel();
            empty.setOpaque(false);
            empty.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            add(empty, BorderLayout.CENTER);
            return;
        }

        Reminder display = ReminderSelector.selectReminderForType(taskManager, title);
        if (display == null) {
            JLabel noReminderLabel = new JLabel("No reminder set");
            noReminderLabel.setFont(FontManager.getSmallFont());
            noReminderLabel.setForeground(java.awt.Color.GRAY);
            add(noReminderLabel, BorderLayout.WEST);
            return;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime remTime = ReminderSelector.reminderDateTime(display);
        ReminderClockIcon.State state = remTime.isBefore(now)
                ? (java.time.Duration.between(remTime, now).toHours() > 1 ? ReminderClockIcon.State.VERY_OVERDUE : ReminderClockIcon.State.OVERDUE)
                : (remTime.isBefore(now.plusMinutes(60)) ? ReminderClockIcon.State.DUE_SOON : ReminderClockIcon.State.FUTURE);

        javax.swing.Icon icon = IconCache.getReminderClockIcon(display.getHour(), display.getMinute(), state, false);
        String text = String.format("%04d-%02d-%02d %02d:%02d", display.getYear(), display.getMonth(), display.getDay(), display.getHour(), display.getMinute());

        javax.swing.JPanel small = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        small.setOpaque(false);
        javax.swing.JLabel iconLabel = new javax.swing.JLabel(icon);
        javax.swing.JLabel textLabel = new javax.swing.JLabel(text);
        textLabel.setFont(FontManager.getSmallMediumFont());
        // Tooltip to explain the reminder timestamp when hovering the icon or text
        String tooltip = "Reminder: " + text;
        String htmlTip = "<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>" + tooltip + "</p></html>";
        iconLabel.setToolTipText(htmlTip);
        textLabel.setToolTipText(htmlTip);
        small.add(iconLabel);
        small.add(textLabel);
        add(small, BorderLayout.WEST);
    }
}
