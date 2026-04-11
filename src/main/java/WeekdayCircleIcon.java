import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
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
        int circleSize = size;
        int ox = x;
        int oy = y;

        // fill circle
        g.setColor(bg);
        g.fillOval(ox, oy, circleSize, circleSize);

        // draw outer ring when selected
        if (selected) {
            g.setColor(java.awt.Color.BLACK);
            g.drawOval(ox - 2, oy - 2, circleSize + 4, circleSize + 4);
        }

        // draw text centered
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(java.awt.Color.WHITE);
        int tx = ox + (circleSize - fm.stringWidth(text)) / 2;
        int ty = oy + circleSize / 2 + (fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, tx, ty);
    }

    @Override
    public int getIconWidth() {
        return size + (selected ? 4 : 0);
    }

    @Override
    public int getIconHeight() {
        return size + (selected ? 4 : 0);
    }
}
