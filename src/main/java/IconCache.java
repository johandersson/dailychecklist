import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Simple icon cache to avoid recreating lightweight icon instances repeatedly.
 */
public final class IconCache {
    private IconCache() {}

    private static final ChecklistDocumentIcon CHECKLIST_DOC = new ChecklistDocumentIcon();
    private static final ZzzIcon ZZZ = new ZzzIcon();

    // Key format: hour-minute-state-showTime
    private static final ConcurrentMap<String, Icon> reminderClockCache = new ConcurrentHashMap<>();

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
            return new ImageIcon(img);
        });
    }
}
