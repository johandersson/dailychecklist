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
 * Icon representing an information/note indicator.
 * Draws a blue circle with an "i" character inside.
 */
public class InfoIcon implements Icon {
    private final int size;
    private static final Color INFO_COLOR = new Color(30, 120, 220); // Brighter, more vibrant blue

    public InfoIcon(int size) {
        this.size = size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw circle background
        g2.setColor(INFO_COLOR);
        g2.fillOval(x, y, size, size);

        // Draw circle border
        g2.setColor(INFO_COLOR.darker());
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(x, y, size, size);

        // Draw "i" character in white (smaller and better centered)
        g2.setColor(Color.WHITE);
        Font font = new Font(FontManager.FONT_NAME, Font.BOLD, (int)(size * 0.55));
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        String text = "i";
        int textWidth = fm.stringWidth(text);
        // Better vertical centering calculation
        int textX = x + (size - textWidth) / 2;
        int textY = y + ((size - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(text, textX, textY);

        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
