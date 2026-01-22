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
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 * Icon that displays Zzz bubbles for very overdue reminders.
 */
public class ZzzIcon implements Icon {
    private static final int ICON_SIZE = 20;

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.BLACK);
        Font orig = g2.getFont();

        // First Z - largest and skewed, positioned above the clock face
        Font f1 = orig.deriveFont(8f);
        g2.setFont(f1);
        java.awt.geom.AffineTransform origTransform = g2.getTransform();
        g2.rotate(Math.toRadians(25), x + ICON_SIZE/2, y + 8);
        g2.drawString("Z", x + ICON_SIZE/2 - 4, y + 8);
        g2.setTransform(origTransform);

        // Second Z - medium size, higher up
        Font f2 = orig.deriveFont(6f);
        g2.setFont(f2);
        g2.rotate(Math.toRadians(20), x + ICON_SIZE/2 + 2, y + 4);
        g2.drawString("Z", x + ICON_SIZE/2 - 1, y + 4);
        g2.setTransform(origTransform);

        // Third Z - small size, highest
        Font f3 = orig.deriveFont(4f);
        g2.setFont(f3);
        g2.rotate(Math.toRadians(15), x + ICON_SIZE/2 + 4, y + 0);
        g2.drawString("Z", x + ICON_SIZE/2 + 1, y + 0);
        g2.setTransform(origTransform);

        g2.setFont(orig);
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