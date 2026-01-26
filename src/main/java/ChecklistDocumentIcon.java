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
 * Simple icon that resembles a document with a checklist.
 */
public class ChecklistDocumentIcon implements Icon {
    private static final int ICON_WIDTH = 24;
    private static final int ICON_HEIGHT = 24;

    private static final Color LINE_COLOR = new Color(200, 200, 200);
    private static final Color DOC_BG = new Color(250, 250, 250);
    private static final Color DOC_BORDER = new Color(180, 180, 180);
    private static final Color CHECKBOX_BORDER = new Color(140, 140, 140);
    private static final Color CHECKMARK_COLOR = new Color(76, 175, 80);

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawDocumentBackground(g2, x, y);
            drawChecklistRows(g2, x, y);
        } finally {
            g2.dispose();
        }
    }

    private void drawDocumentBackground(Graphics2D g2, int x, int y) {
        // drop shadow
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillRoundRect(x + 2, y + 2, ICON_WIDTH, ICON_HEIGHT, 6, 6);

        // background
        g2.setColor(DOC_BG);
        g2.fillRoundRect(x, y, ICON_WIDTH, ICON_HEIGHT, 6, 6);

        // border
        g2.setColor(DOC_BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, ICON_WIDTH - 1, ICON_HEIGHT - 1, 6, 6);
    }

    private void drawChecklistRows(Graphics2D g2, int x, int y) {
        // increase internal padding so contents don't touch the rounded corners
        final int startX = x + 4;
        final int startY = y + 6;
        final int checkboxSize = 7; // slightly smaller boxes to avoid spillover
        final int rowSpacing = 12;
        final int rows = 2; // keep two rows

        for (int row = 0; row < rows; row++) {
            int rowTop = startY + row * rowSpacing;
            drawCheckboxWithShadow(g2, startX, rowTop, checkboxSize);
            drawLineForRow(g2, x, startX, checkboxSize, rowTop);
        }
    }

    private void drawCheckboxWithShadow(Graphics2D g2, int bx, int by, int size) {
        // subtle shadow (slightly larger roundness for bigger box)
        g2.setColor(new Color(0, 0, 0, 20));
        g2.fillRoundRect(bx + 1, by + 1, size, size, 4, 4);

        // box background
        g2.setColor(DOC_BG);
        g2.fillRoundRect(bx, by, size, size, 4, 4);

        // box border
        g2.setColor(CHECKBOX_BORDER);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(bx, by, size, size, 4, 4);

        // checkmark - keep it nicely inside the box and avoid overlap between rows
        Object prevAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(CHECKMARK_COLOR);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = bx; // center relative to box
        int cy = by;
        int x1 = cx + 2;
        int y1 = cy + size / 2;
        int x2 = cx + size / 2;
        int y2 = cy + size - 3;
        int x3 = cx + size - 2;
        int y3 = cy + 3;
        g2.drawLine(x1, y1, x2, y2);
        g2.drawLine(x2, y2, x3, y3);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA);
    }

    private void drawLineForRow(Graphics2D g2, int docX, int startX, int checkboxSize, int rowTop) {
        g2.setColor(LINE_COLOR);
        g2.setStroke(new BasicStroke(1.2f));
        int tx = startX + checkboxSize + 6;
        int ty = rowTop + checkboxSize / 2;
        // Keep the line inside the rounded document border by adding a small inset
        int txEnd = docX + ICON_WIDTH - 6;
        g2.drawLine(tx, ty, txEnd, ty);
    }

    @Override
    public int getIconWidth() {
        return ICON_WIDTH;
    }

    @Override
    public int getIconHeight() {
        return ICON_HEIGHT;
    }
}

