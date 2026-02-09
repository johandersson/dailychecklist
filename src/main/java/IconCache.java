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
    private static final javax.swing.Icon CHECKMARK_ICON;
    private static final javax.swing.Icon NOTE_ICON;

    // Key format: hour-minute-state-showTime
    private static final ConcurrentMap<String, Icon> reminderClockCache = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<String> reminderClockKeys = new ConcurrentLinkedQueue<>();
    private static final int MAX_REMINDER_ICONS = 200;
    // NOTE: application icon is provided by AppIcon.getAppIcon()

    public static Icon getChecklistDocumentIcon() {
        return CHECKLIST_DOC;
    }

    public static Image getAppIcon() {
        return AppIcon.getAppIcon();
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

    public static Icon getCheckmarkIcon() {
        return CHECKMARK_ICON;
    }

    public static Icon getNoteIcon() {
        return NOTE_ICON;
    }

    public static Icon getReminderClockIcon(int hour, int minute, ReminderClockIcon.State state, boolean showTimeText) {
        String key = hour + "-" + minute + "-" + (state == null ? "" : state.name()) + "-" + showTimeText;
        return reminderClockCache.computeIfAbsent(key, k -> {
            ReminderClockIcon tmp = new ReminderClockIcon(hour, minute, state, showTimeText);
            ImageIcon icon = renderToImageIcon(tmp);
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
        // Pre-render static icons via helper to reduce duplication
        ZzzIcon rawZzz = new ZzzIcon();
        ZZZ = renderToImageIcon(rawZzz);

        CHECKLIST_DOC = renderToImageIcon(CHECKLIST_DOC_RAW);

        NOTE_ICON = renderToImageIcon(new NoteIcon(18));

        ADD_SUBTASK_ICON = renderToImageIcon(new AddSubtaskIcon(16));

        CHECKMARK_ICON = renderToImageIcon(new CheckmarkIcon(16));
    }

    private static ImageIcon renderToImageIcon(javax.swing.Icon raw) {
        int w = Math.max(1, raw.getIconWidth());
        int h = Math.max(1, raw.getIconHeight());
        // Detect device scale (HiDPI) and render at device pixel size to avoid blurry scaling
        double scaleX = 1.0, scaleY = 1.0;
        try {
            java.awt.GraphicsDevice gd = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            java.awt.geom.AffineTransform tx = gd.getDefaultConfiguration().getDefaultTransform();
            scaleX = tx.getScaleX();
            scaleY = tx.getScaleY();
        } catch (Throwable ex) {
            // best-effort; fall back to 1.0
        }
        int sw = Math.max(1, (int) Math.ceil(w * scaleX));
        int sh = Math.max(1, (int) Math.ceil(h * scaleY));
        BufferedImage img = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            // If scaling is needed, scale the Graphics so the icon draws at the correct device pixel size
            if (scaleX != 1.0 || scaleY != 1.0) g.scale(scaleX, scaleY);
            raw.paintIcon(null, g, 0, 0);
        } finally {
            g.dispose();
        }
        return new ImageIcon(img);
    }

    
}
