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

        // Draw alarm bells on top
        g2.setColor(CLOCK_COLOR.darker());
        int bellRadius = 3;
        int bellY = y + 2;
        // Left bell
        g2.fillOval(x + 4, bellY, bellRadius * 2, bellRadius * 2);
        // Right bell
        g2.fillOval(x + ICON_SIZE - 4 - bellRadius * 2, bellY, bellRadius * 2, bellRadius * 2);
        // Bell hammer
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(x + ICON_SIZE / 2 - 2, bellY + bellRadius, x + ICON_SIZE / 2 + 2, bellY + bellRadius);

        // Draw clock face (circle) - slightly smaller to make room for bells
        int clockSize = ICON_SIZE - 6;
        int clockX = x + 3;
        int clockY = y + 6;
        g2.setColor(CLOCK_COLOR);
        g2.fillOval(clockX, clockY, clockSize, clockSize);

        // Draw clock outline
        g2.setColor(CLOCK_COLOR.darker());
        g2.setStroke(new BasicStroke(1));
        g2.drawOval(clockX, clockY, clockSize, clockSize);

        // Draw clock hands
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1));

        // Calculate center
        int centerX = clockX + clockSize / 2;
        int centerY = clockY + clockSize / 2;

        // Hour hand (shorter, thicker) - pointing to 10 o'clock
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(centerX, centerY, centerX - 3, centerY - 4);

        // Minute hand (longer, thinner) - pointing to 12 o'clock
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(centerX, centerY, centerX, centerY - 5);

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