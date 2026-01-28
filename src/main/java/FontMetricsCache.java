import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache of FontMetrics per Font. If a FontMetrics is not present,
 * it is created using a temporary BufferedImage Graphics context.
 */
public final class FontMetricsCache {
    private static final ConcurrentHashMap<Font, FontMetrics> CACHE = new ConcurrentHashMap<>();

    private FontMetricsCache() {}

    public static FontMetrics get(Font font) {
        FontMetrics fm = CACHE.get(font);
        if (fm != null) return fm;
        // Create a temporary graphics context to obtain FontMetrics
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setFont(font);
            FontMetrics created = g2.getFontMetrics(font);
            FontMetrics prev = CACHE.putIfAbsent(font, created);
            return prev != null ? prev : created;
        } finally {
            g2.dispose();
        }
    }
}
