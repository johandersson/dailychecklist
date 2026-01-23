import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 * Simple icon that resembles a document with a checklist.
 */
public class ChecklistDocumentIcon implements Icon {
    private static final int WIDTH = 24;
    private static final int HEIGHT = 24;

    private final Color lineColor = new Color(120, 120, 120);
    private final Color boxColor = new Color(76, 175, 80);

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw document background (slightly inset to create visual padding)
        g2.setColor(new Color(250, 250, 250));
        g2.fillRect(x, y, WIDTH, HEIGHT);

        // Subtle border
        g2.setColor(new Color(180, 180, 180));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(x, y, WIDTH - 1, HEIGHT - 1);

        // Draw three checklist lines with compact checkboxes
        int bx = x + 3;
        int by = y + 4;
        int boxSize = 6; // slightly smaller to make checkmark thinner
        for (int i = 0; i < 3; i++) {
            // checkbox (outline + fill)
            g2.setColor(new java.awt.Color(240, 240, 240));
            g2.fillRect(bx, by + i * 7, boxSize, boxSize);
            g2.setColor(new java.awt.Color(140, 140, 140));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRect(bx, by + i * 7, boxSize, boxSize);

            // slightly smaller, thinner checkmark for visual balance
            g2.setColor(new java.awt.Color(76, 175, 80));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = bx + 1;
            int cy = by + i * 7 + 1;
            g2.drawLine(cx, cy + 2, cx + 3, cy + 5);
            g2.drawLine(cx + 3, cy + 5, cx + 6, cy + 1);

            // text line
            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(1f));
            int tx = bx + boxSize + 6;
            int ty = by + i * 7 + boxSize - 1;
            g2.drawLine(tx, ty, x + WIDTH - 4, ty);
        }

        g2.dispose();
    }

    @Override
    public int getIconWidth() { return WIDTH; }

    @Override
    public int getIconHeight() { return HEIGHT; }
}
