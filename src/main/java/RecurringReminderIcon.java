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
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import javax.swing.Icon;

/**
 * Small recurring badge icon used beside reminder clocks.
 * Draws two thin circular arrow arcs forming a "repeat/cycle" symbol.
 */
public class RecurringReminderIcon implements Icon {
    private static final int ICON_SIZE = 14;
    private static final Color BACKGROUND = new Color(46, 134, 171);
    private static final Color BORDER = new Color(30, 92, 117);
    private static final Color FOREGROUND = Color.WHITE;

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int pad = 1;
            int diameter = ICON_SIZE - (pad * 2);
            int left = x + pad;
            int top = y + pad;

            // Background circle
            g2.setColor(BACKGROUND);
            g2.fillOval(left, top, diameter, diameter);

            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawOval(left, top, diameter, diameter);

            // Arrow arcs: thin strokes for clarity at small sizes
            g2.setColor(FOREGROUND);
            g2.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            double inset = 3.0;
            double arcLeft = left + inset;
            double arcTop = top + inset;
            double arcSize = diameter - (inset * 2);
            double cx = arcLeft + arcSize / 2.0;
            double cy = arcTop + arcSize / 2.0;
            double radius = arcSize / 2.0;

            // Two arcs each spanning 140 degrees, separated by 40-degree gaps
            // Top arc: starts at 20 degrees, sweeps 140 degrees counter-clockwise (in Java2D angles)
            Arc2D topArc = new Arc2D.Double(arcLeft, arcTop, arcSize, arcSize, 20, 140, Arc2D.OPEN);
            g2.draw(topArc);

            // Bottom arc: starts at 200 degrees, sweeps 140 degrees
            Arc2D bottomArc = new Arc2D.Double(arcLeft, arcTop, arcSize, arcSize, 200, 140, Arc2D.OPEN);
            g2.draw(bottomArc);

            // Arrowheads at the end of each arc, drawn tangent to the arc direction
            // Top arc ends at 20+140=160 degrees; arrowhead points in arc travel direction
            drawTangentArrowHead(g2, cx, cy, radius, 160, true);
            // Bottom arc ends at 200+140=340 degrees; arrowhead points in arc travel direction
            drawTangentArrowHead(g2, cx, cy, radius, 340, true);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Draws a small filled triangular arrowhead at the given angle on the circle,
     * oriented tangent to the arc (i.e. pointing in the direction of travel).
     */
    private void drawTangentArrowHead(Graphics2D g2, double cx, double cy, double radius,
                                       double angleDeg, boolean counterClockwise) {
        double angleRad = Math.toRadians(angleDeg);
        // Tip of arrow sits on the circle
        double tipX = cx + Math.cos(angleRad) * radius;
        double tipY = cy - Math.sin(angleRad) * radius;

        // Tangent direction (perpendicular to radius): for CCW travel, tangent points "ahead"
        double tangentAngle = counterClockwise ? angleRad + Math.PI / 2.0 : angleRad - Math.PI / 2.0;

        // Arrow tip points along the tangent; two base vertices spread behind
        double arrowLen = 3.2;
        double halfBase = 1.6;

        // Direction from base to tip is along tangent
        double dx = Math.cos(tangentAngle);
        double dy = -Math.sin(tangentAngle);
        // Perpendicular to tangent for spreading the base
        double px = -dy;
        double py = dx;

        // Base center is behind the tip
        double baseX = tipX - dx * arrowLen;
        double baseY = tipY - dy * arrowLen;

        Path2D arrow = new Path2D.Double();
        arrow.moveTo(tipX, tipY);
        arrow.lineTo(baseX + px * halfBase, baseY + py * halfBase);
        arrow.lineTo(baseX - px * halfBase, baseY - py * halfBase);
        arrow.closePath();
        g2.fill(arrow);
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