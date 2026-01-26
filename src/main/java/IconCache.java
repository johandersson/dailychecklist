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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Simple icon cache to avoid recreating lightweight icon instances repeatedly.
 */
public final class IconCache {
    private IconCache() {}

    private static final ChecklistDocumentIcon CHECKLIST_DOC_RAW = new ChecklistDocumentIcon();
    private static final ZzzIcon ZZZ = new ZzzIcon();
    private static final javax.swing.Icon CHECKLIST_DOC;

    // Key format: hour-minute-state-showTime
    private static final ConcurrentMap<String, Icon> reminderClockCache = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<String> reminderClockKeys = new ConcurrentLinkedQueue<>();
    private static final int MAX_REMINDER_ICONS = 200;

    public static Icon getChecklistDocumentIcon() {
        return CHECKLIST_DOC;
    }

    public static Icon getZzzIcon() {
        return ZZZ;
    }

    public static Icon getReminderClockIcon(int hour, int minute, ReminderClockIcon.State state) {
        return getReminderClockIcon(hour, minute, state, false);
    }

    public static Icon getReminderClockIcon(int hour, int minute, ReminderClockIcon.State state, boolean showTimeText) {
        String key = hour + "-" + minute + "-" + (state == null ? "" : state.name()) + "-" + showTimeText;
        return reminderClockCache.computeIfAbsent(key, k -> {
            // Pre-render the ReminderClockIcon into a BufferedImage and return an ImageIcon.
            ReminderClockIcon tmp = new ReminderClockIcon(hour, minute, state, showTimeText);
            int w = tmp.getIconWidth();
            int h = tmp.getIconHeight();
            BufferedImage img = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            try {
                // Paint into the buffered image once
                tmp.paintIcon(null, g, 0, 0);
            } finally {
                g.dispose();
            }
            ImageIcon icon = new ImageIcon(img);
            // Track insertion order and prune if necessary
            reminderClockKeys.add(k);
            while (reminderClockKeys.size() > MAX_REMINDER_ICONS) {
                String old = reminderClockKeys.poll();
                if (old != null) reminderClockCache.remove(old);
            }
            return icon;
        });
    }

    static {
        // Pre-render the checklist document icon into an ImageIcon for consistent painting
        int w = CHECKLIST_DOC_RAW.getIconWidth();
        int h = CHECKLIST_DOC_RAW.getIconHeight();
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(Math.max(1, w), Math.max(1, h), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        try {
            CHECKLIST_DOC_RAW.paintIcon(null, g, 0, 0);
        } finally {
            g.dispose();
        }
        CHECKLIST_DOC = new javax.swing.ImageIcon(img);
    }
}
