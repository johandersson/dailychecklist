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
    private static final Color CLOCK_COLOR = new Color(255, 87, 34); // Material Design orange

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();

        // Enable anti-aliasing for smoother rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw clock face (circle)
        g2.setColor(CLOCK_COLOR);
        g2.fillOval(x, y, ICON_SIZE, ICON_SIZE);

        // Draw clock outline
        g2.setColor(CLOCK_COLOR.darker());
        g2.setStroke(new BasicStroke(1));
        g2.drawOval(x, y, ICON_SIZE, ICON_SIZE);

        // Draw clock hands
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1));

        // Hour hand (shorter, thicker)
        g2.setStroke(new BasicStroke(2));
        int centerX = x + ICON_SIZE / 2;
        int centerY = y + ICON_SIZE / 2;
        g2.drawLine(centerX, centerY, centerX + 3, centerY - 2);

        // Minute hand (longer, thinner)
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(centerX, centerY, centerX + 5, centerY + 1);

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