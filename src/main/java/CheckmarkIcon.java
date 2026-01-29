/*
 * Daily Checklist
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.Icon;

public class CheckmarkIcon implements Icon {
    private final int size;

    public CheckmarkIcon() { this(16); }
    public CheckmarkIcon(int size) { this.size = size; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(76, 175, 80));
            g2.setStroke(new BasicStroke(Math.max(1f, size / 8f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Draw checkmark: from lower-left -> mid -> upper-right
            int sx = x + (int) Math.round(size * 0.20);
            int sy = y + (int) Math.round(size * 0.60);
            int mx = x + (int) Math.round(size * 0.45);
            int my = y + (int) Math.round(size * 0.80);
            int ex = x + (int) Math.round(size * 0.85);
            int ey = y + (int) Math.round(size * 0.25);
            g2.drawLine(sx, sy, mx, my);
            g2.drawLine(mx, my, ex, ey);
        } finally {
            g2.dispose();
        }
    }

    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
}
