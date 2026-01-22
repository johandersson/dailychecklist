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
 * Icon that displays a clock symbol for checklists with reminders.
 * It can render the reminder time inside the clock and use different
 * colors for overdue, due (now), and future reminders.
 */
public class ReminderClockIcon implements Icon {
    private static final int ICON_SIZE = 20;

    public enum State {
        OVERDUE, DUE_SOON, FUTURE, VERY_OVERDUE
    }

    private final int hour;
    private final int minute;
    private final State state;
    private final boolean showTimeText;

    public ReminderClockIcon(int hour, int minute, State state) {
        this(hour, minute, state, true);
    }

    public ReminderClockIcon(int hour, int minute, State state, boolean showTimeText) {
        this.hour = hour;
        this.minute = minute;
        this.state = state;
        this.showTimeText = showTimeText;
    }

    private Color colorForState() {
        return switch (state) {
            case OVERDUE -> new Color(192, 57, 43); // red-ish
            case DUE_SOON -> new Color(204, 102, 0); // darker orange (readable)
            case FUTURE -> new Color(46, 134, 171); // blue-ish
            case VERY_OVERDUE -> new Color(139, 0, 0); // dark red
        };
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = x + ICON_SIZE / 2;
        int centerY = y + ICON_SIZE / 2;

        // Outline
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawOval(x + 1, y + 1, ICON_SIZE - 2, ICON_SIZE - 2);

        // Face filled with state color
        Color face = colorForState();
        g2.setColor(face);
        g2.fillOval(x + 2, y + 2, ICON_SIZE - 4, ICON_SIZE - 4);

        // Center dot
        g2.setColor(face.darker());
        g2.fillOval(centerX - 1, centerY - 1, 3, 3);

        // Clock hands based on hour/minute
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Compute simple hand positions (not accurate angles, but readable)
        int hourHandX = centerX + (int) (Math.cos(Math.toRadians((hour % 12) * 30 - 90)) * 4);
        int hourHandY = centerY + (int) (Math.sin(Math.toRadians((hour % 12) * 30 - 90)) * 4);
        int minuteHandX = centerX + (int) (Math.cos(Math.toRadians(minute * 6 - 90)) * 6);
        int minuteHandY = centerY + (int) (Math.sin(Math.toRadians(minute * 6 - 90)) * 6);

        g2.drawLine(centerX, centerY, hourHandX, hourHandY);
        g2.drawLine(centerX, centerY, minuteHandX, minuteHandY);

        // Draw Zzz for very overdue
        if (state == State.VERY_OVERDUE) {
            g2.setColor(Color.BLACK);
            Font orig = g2.getFont();

            // First Z - largest and skewed
            Font f1 = orig.deriveFont(10f);
            g2.setFont(f1);
            java.awt.geom.AffineTransform origTransform = g2.getTransform();
            g2.rotate(Math.toRadians(15), x + ICON_SIZE + 5, y + 10);
            g2.drawString("Z", x + ICON_SIZE + 2, y + 10);
            g2.setTransform(origTransform);

            // Second Z - medium size
            Font f2 = orig.deriveFont(8f);
            g2.setFont(f2);
            g2.rotate(Math.toRadians(10), x + ICON_SIZE + 12, y + 5);
            g2.drawString("Z", x + ICON_SIZE + 9, y + 5);
            g2.setTransform(origTransform);

            // Third Z - small size
            Font f3 = orig.deriveFont(6f);
            g2.setFont(f3);
            g2.rotate(Math.toRadians(5), x + ICON_SIZE + 18, y + 2);
            g2.drawString("Z", x + ICON_SIZE + 15, y + 2);
            g2.setTransform(origTransform);

            g2.setFont(orig);
        }

        // Optionally draw time text (e.g., "9:30") to the right of the icon
        if (showTimeText) {
            String timeText = String.format("%d:%02d", hour, minute);
            Font orig = g2.getFont();
            Font f = orig.deriveFont(11f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int th = fm.getAscent();
            int textX = x + ICON_SIZE + 4;
            int textY = y + (ICON_SIZE + th) / 2 - 1;

            boolean selected = false;
            if (c != null) {
                Object sel = c instanceof javax.swing.JComponent ? ((javax.swing.JComponent) c).getClientProperty("selected") : null;
                if (sel instanceof Boolean) selected = (Boolean) sel;
            }
            g2.setColor(selected ? Color.BLACK : colorForState().darker());
            g2.drawString(timeText, textX, textY);
            g2.setFont(orig);
        }
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        if (showTimeText) {
            return ICON_SIZE + 28;
        } else if (state == State.VERY_OVERDUE) {
            return ICON_SIZE + 25; // Extra space for the three Zzz
        } else {
            return ICON_SIZE + 6;
        }
    }

    @Override
    public int getIconHeight() {
        return ICON_SIZE;
    }
}