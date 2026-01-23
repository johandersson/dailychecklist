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

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Subtle drop shadow for the document
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillRoundRect(x + 2, y + 2, WIDTH, HEIGHT, 6, 6);

            // Draw document background
            g2.setColor(new Color(250, 250, 250));
            g2.fillRoundRect(x, y, WIDTH, HEIGHT, 6, 6);

            // Subtle border
            g2.setColor(new Color(180, 180, 180));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x, y, WIDTH - 1, HEIGHT - 1, 6, 6);

            // Draw three checklist rows
            int bx = x + 4;
            int by = y + 5;
            int boxSize = 6;
            int vStep = 7;
            for (int i = 0; i < 3; i++) {
                int rowY = by + i * vStep;

                // subtle shadow behind checkbox
                g2.setColor(new Color(0, 0, 0, 28));
                g2.fillRoundRect(bx + 1, rowY + 1, boxSize, boxSize, 3, 3);

                // checkbox background and border
                g2.setColor(new Color(245, 245, 245));
                g2.fillRoundRect(bx, rowY, boxSize, boxSize, 3, 3);
                g2.setColor(new Color(140, 140, 140));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(bx, rowY, boxSize, boxSize, 3, 3);

                // checkmark
                g2.setColor(new Color(76, 175, 80));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = bx + 1;
                int cy = rowY + 1;
                g2.drawLine(cx, cy + boxSize / 2, cx + boxSize / 2, cy + boxSize - 1);
                g2.drawLine(cx + boxSize / 2, cy + boxSize - 1, cx + boxSize - 1, cy + 2);

                // text line
                g2.setColor(lineColor);
                g2.setStroke(new BasicStroke(1f));
                int tx = bx + boxSize + 6;
                int ty = rowY + boxSize / 2;
                g2.drawLine(tx, ty, x + WIDTH - 6, ty);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return WIDTH;
    }

    @Override
    public int getIconHeight() {
        return HEIGHT;
    }
}

