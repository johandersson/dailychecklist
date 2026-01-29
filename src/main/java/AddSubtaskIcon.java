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
import javax.swing.Icon;

public class AddSubtaskIcon implements Icon {
    private final int size;

    public AddSubtaskIcon() {
        this(16);
    }

    public AddSubtaskIcon(int size) {
        this.size = size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            // circular background
            g2.setColor(new Color(66, 133, 244));
            g2.fillOval(x, y, size, size);
            // plus sign
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(Math.max(1f, size / 8f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = x + size / 2;
            int cy = y + size / 2;
            int len = Math.max(4, size * 2 / 5);
            g2.drawLine(cx - len/2, cy, cx + len/2, cy);
            g2.drawLine(cx, cy - len/2, cx, cy + len/2);
        } finally {
            g2.dispose();
        }
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
