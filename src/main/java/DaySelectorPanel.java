import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

/**
 * Small reusable day-of-week selector that renders 7 small colored circles
 * with two-letter abbreviations and supports toggling selection by clicking.
 * Reuses the same visuals as the weekday circle in the checklist renderer.
 */
@SuppressWarnings("serial")
public class DaySelectorPanel extends JPanel {
    // bit0 = Monday ... bit6 = Sunday
    private int daysBitmask = 0;
    private Font abbrevFont = new Font("Yu Gothic UI", Font.BOLD, 10);
    private static final String[] ABBR = {"Mo","Tu","We","Th","Fr","Sa","Su"};
    private static final Color[] COLORS = {
        new Color(165,42,42), new Color(0,90,156), new Color(139,128,0),
        new Color(34,139,34), new Color(139,69,19), new Color(255,69,0), new Color(199,21,133)
    };

    public DaySelectorPanel() {
        setPreferredSize(new Dimension(260, 36));
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = indexAt(e.getX(), e.getY());
                if (idx >= 0) {
                    toggle(idx);
                    repaint();
                }
            }
        });
    }

    private int indexAt(int x, int y) {
        int w = getWidth();
        int preferred = Math.min(32, Math.max(20, w / 9));
        // determine spacing based on preferred icon size
        int spacing = (w - preferred * 7) / 8;
        if (spacing < 4) spacing = 4;
        int cx = spacing;
        for (int i = 0; i < 7; i++) {
            int left = cx;
            int right = cx + preferred;
            if (x >= left && x <= right) return i;
            cx += preferred + spacing;
        }
        return -1;
    }

    public void setSelectedDaysBitmask(int bitmask) {
        this.daysBitmask = bitmask;
        repaint();
    }

    public int getSelectedDaysBitmask() {
        return daysBitmask;
    }

    public void toggle(int index) {
        int bit = 1 << index;
        daysBitmask = (daysBitmask & bit) == 0 ? (daysBitmask | bit) : (daysBitmask & ~bit);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int preferred = Math.min(28, Math.max(20, h - 6));
        // compute actual icon widths and spacing using the icon's reported size to avoid clipping
        int[] iconWidths = new int[7];
        for (int i = 0; i < 7; i++) {
            javax.swing.Icon tmp = IconCache.getWeekdayIcon(ABBR[i], COLORS[i % COLORS.length], false, preferred);
            iconWidths[i] = tmp.getIconWidth();
        }
        int totalIconsWidth = 0; for (int iw : iconWidths) totalIconsWidth += iw;
        int spacing = (w - totalIconsWidth) / 8;
        if (spacing < 4) spacing = 4;
        int x = spacing;

        for (int i = 0; i < 7; i++) {
            boolean selected = (daysBitmask & (1 << i)) != 0;
            java.awt.Color bg = COLORS[i % COLORS.length];
            javax.swing.Icon icon = IconCache.getWeekdayIcon(ABBR[i], bg, selected, preferred);
            int y = (h - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2, x, y);
            x += icon.getIconWidth() + spacing;
        }

        g2.dispose();
    }
}
