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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

/**
 * Icon that displays a clock symbol for checklists with reminders.
 */
public class ReminderClockIcon implements Icon {
    private static final int ICON_SIZE = 16;
    private static final Color CLOCK_COLOR = new Color(46, 134, 171); // #2E86AB - same as dialog header

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();

        // Enable anti-aliasing for smoother rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate center
        int centerX = x + ICON_SIZE / 2;
        int centerY = y + ICON_SIZE / 2;

        // Draw clock outline (clear black outline)
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(x + 1, y + 1, ICON_SIZE - 2, ICON_SIZE - 2);

        // Draw clock face background
        g2.setColor(CLOCK_COLOR);
        g2.fillOval(x + 2, y + 2, ICON_SIZE - 4, ICON_SIZE - 4);

        // Draw inner circle for more detail
        g2.setColor(CLOCK_COLOR.darker());
        g2.fillOval(centerX - 1, centerY - 1, 3, 3);

        // Draw clock hands with better contrast
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Hour hand (shorter) - pointing to 10 o'clock
        g2.drawLine(centerX, centerY, centerX - 2, centerY - 4);

        // Minute hand (longer) - pointing to 2 o'clock
        g2.drawLine(centerX, centerY, centerX + 4, centerY - 2);

        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return ICON_SIZE;
    }

    @Override
    public int getIconHeight() {
        return ICON_SIZE;
    }
}