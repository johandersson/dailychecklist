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
import java.awt.Image;
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
    private static final javax.swing.Icon ZZZ;
    private static final javax.swing.Icon CHECKLIST_DOC;
    private static final javax.swing.Icon ADD_SUBTASK_ICON;

    // Key format: hour-minute-state-showTime
    private static final ConcurrentMap<String, Icon> reminderClockCache = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<String> reminderClockKeys = new ConcurrentLinkedQueue<>();
    private static final int MAX_REMINDER_ICONS = 200;
    // Cached application icon (32x32)
    private static volatile Image APP_ICON;

    public static Icon getChecklistDocumentIcon() {
        return CHECKLIST_DOC;
    }

    public static Image getAppIcon() {
        Image img = APP_ICON;
        if (img != null) return img;
        synchronized (IconCache.class) {
            if (APP_ICON != null) return APP_ICON;
            APP_ICON = createAppIcon();
            return APP_ICON;
        }
    }

    public static Icon getZzzIcon() {
        return ZZZ;
    }

    public static Icon getReminderClockIcon(int hour, int minute, ReminderClockIcon.State state) {
        return getReminderClockIcon(hour, minute, state, false);
    }

    public static Icon getAddSubtaskIcon() {
        return ADD_SUBTASK_ICON;
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
        // Pre-render ZzzIcon into an ImageIcon to avoid repeated painting work
        ZzzIcon rawZzz = new ZzzIcon();
        int zw = rawZzz.getIconWidth();
        int zh = rawZzz.getIconHeight();
        java.awt.image.BufferedImage zimg = new java.awt.image.BufferedImage(Math.max(1, zw), Math.max(1, zh), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D zg = zimg.createGraphics();
        try {
            zg.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            rawZzz.paintIcon(null, zg, 0, 0);
        } finally {
            zg.dispose();
        }
        ZZZ = new javax.swing.ImageIcon(zimg);

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

        // Use dedicated AddSubtaskIcon class and pre-render into an ImageIcon
        AddSubtaskIcon rawAdd = new AddSubtaskIcon(16);
        int aw = rawAdd.getIconWidth();
        int ah = rawAdd.getIconHeight();
        java.awt.image.BufferedImage aimg = new java.awt.image.BufferedImage(Math.max(1, aw), Math.max(1, ah), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D ag = aimg.createGraphics();
        try {
            ag.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            rawAdd.paintIcon(null, ag, 0, 0);
        } finally {
            ag.dispose();
        }
        ADD_SUBTASK_ICON = new javax.swing.ImageIcon(aimg);
    }

    private static Image createAppIcon() {
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
        g2.setColor(new java.awt.Color(200, 200, 200, 100));
        g2.fillRoundRect(checkboxX + 1, checkboxY + 1, checkboxSize, checkboxSize, 6, 6);

        // Draw checkbox outline
        g2.setColor(new java.awt.Color(120, 120, 120));
        g2.drawRoundRect(checkboxX, checkboxY, checkboxSize, checkboxSize, 6, 6);

        // Fill checkbox with white
        g2.setColor(java.awt.Color.WHITE);
        g2.fillRoundRect(checkboxX + 1, checkboxY + 1, checkboxSize - 2, checkboxSize - 2, 6, 6);

        // Draw checkmark
        g2.setColor(new java.awt.Color(76, 175, 80)); // Material green
        g2.setStroke(new java.awt.BasicStroke(2, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        int offsetX = checkboxX + 3;
        int offsetY = checkboxY + 6;
        g2.drawLine(offsetX + 2, offsetY + 6, offsetX + 7, offsetY + 11);
        g2.drawLine(offsetX + 7, offsetY + 11, offsetX + 15, offsetY + 1);

        g2.dispose();
        return image;
    }
}
