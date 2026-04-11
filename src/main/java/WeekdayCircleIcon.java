import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 * Icon that draws a colored circular badge with a two-letter weekday abbreviation.
 * Supports an outer ring when "selected" is true.
 */
@SuppressWarnings("serial")
public class WeekdayCircleIcon implements Icon {
    private final String text;
    private final Color bg;
    private final boolean selected;
    private final int size;
    private final Font font;

    public WeekdayCircleIcon(String text, Color bg, boolean selected, int size) {
        this.text = text == null ? "" : text;
        this.bg = bg == null ? new Color(120, 120, 120) : bg;
        this.selected = selected;
        this.size = size <= 0 ? 28 : size;
        this.font = new Font("Yu Gothic UI", Font.BOLD, Math.max(10, size / 3));
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int pad = 3; // padding for outer ring
            int circleSize = size;
            int ox = x + pad;
            int oy = y + pad;

            // fill circle
            g2.setColor(bg);
            g2.fillOval(ox, oy, circleSize, circleSize);

            // draw outer ring when selected
            if (selected) {
                g2.setColor(Color.black);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(ox - 2, oy - 2, circleSize + 4, circleSize + 4);
            }

            // draw text centered
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(java.awt.Color.WHITE);
            int tx = ox + (circleSize - fm.stringWidth(text)) / 2;
            int ty = oy + circleSize / 2 + (fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(text, tx, ty);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        // include padding for outer ring
        return size + 6;
    }

    @Override
    public int getIconHeight() {
        return size + 6;
    }
}
