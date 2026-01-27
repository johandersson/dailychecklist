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
import java.awt.geom.QuadCurve2D;
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
        // move the whole checklist slightly up to avoid the lower checkmark touching the rounded bottom
        final int startY = y + 4;
        final int checkboxSize = 6; // slightly smaller boxes to avoid spillover
        // reduce spacing so icons and grey lines sit a bit closer together
        final int rowSpacing = 9;
        final int rows = 2; // keep two rows

        for (int row = 0; row < rows; row++) {
            int rowTop = startY + row * rowSpacing;
            drawCheckboxWithShadow(g2, startX, rowTop, checkboxSize);
            drawLineForRow(g2, x, y, startX, checkboxSize, rowTop, row);
        }
    }

    private void drawCheckboxWithShadow(Graphics2D g2, int bx, int by, int size) {
        // subtle shadow
        g2.setColor(new Color(0, 0, 0, 18));
        g2.fillRoundRect(bx + 1, by + 1, size, size, 3, 3);

        // box background
        g2.setColor(DOC_BG);
        g2.fillRoundRect(bx, by, size, size, 3, 3);

        // box border (thinner)
        g2.setColor(CHECKBOX_BORDER);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(bx, by, size, size, 3, 3);

        // checkmark - reuse `AppIcon` proportions scaled to checkbox size, with round caps
            // checkmark - reuse the same style as CheckboxListCellRenderer but scaled to this checkbox
            Object prevAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CHECKMARK_COLOR);
            // original template was a 16x16 image with stroke 2.5f
            double templateSize = 16.0;
            double scale = (double) size / templateSize;
            float strokeWidth = (float) Math.max(1.0, 2.5 * scale);
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // center the scaled 16x16 template inside the checkbox
            int imgW = Math.max(1, (int) Math.round(templateSize * scale));
            int imgH = Math.max(1, (int) Math.round(templateSize * scale));
            int imgX = bx + (size - imgW) / 2;
            int imgY = by + (size - imgH) / 2;

            // template check coordinates
            int t1x = 3, t1y = 9;
            int t2x = 7, t2y = 13;
            int t3x = 13, t3y = 5;

            int x1 = imgX + (int) Math.round(t1x * scale);
            int y1 = imgY + (int) Math.round(t1y * scale);
            int x2 = imgX + (int) Math.round(t2x * scale);
            int y2 = imgY + (int) Math.round(t2y * scale);
            int x3 = imgX + (int) Math.round(t3x * scale);
            int y3 = imgY + (int) Math.round(t3y * scale);

            // allow the checkmark to be slightly bigger (extend a couple px outside the checkbox)
            // by scaling the template a little and permitting a small overflow when clamping
            double markExtraScale = 1.12; // make the mark a bit larger than the template
            // recompute center with extra scale
            int imgW2 = Math.max(1, (int) Math.round(templateSize * scale * markExtraScale));
            int imgH2 = imgW2;
            int imgX2 = bx + (size - imgW2) / 2;
            int imgY2 = by + (size - imgH2) / 2;

            x1 = imgX2 + (int) Math.round(t1x * scale * markExtraScale);
            y1 = imgY2 + (int) Math.round(t1y * scale * markExtraScale);
            x2 = imgX2 + (int) Math.round(t2x * scale * markExtraScale);
            y2 = imgY2 + (int) Math.round(t2y * scale * markExtraScale);
            x3 = imgX2 + (int) Math.round(t3x * scale * markExtraScale);
            y3 = imgY2 + (int) Math.round(t3y * scale * markExtraScale);

            // move the whole mark a bit upward so it sits higher in the checkbox
            y1 -= 2;
            y2 -= 2;
            y3 -= 2;

            // permit a small overflow beyond the checkbox bounds so the mark visibly protrudes
            int clampMinX = bx - 2;
            int clampMinY = by - 2;
            int clampMaxX = bx + size + 2;
            int clampMaxY = by + size + 2;

            x1 = Math.min(clampMaxX, Math.max(clampMinX, x1));
            y1 = Math.min(clampMaxY, Math.max(clampMinY, y1));
            x2 = Math.min(clampMaxX, Math.max(clampMinX, x2));
            y2 = Math.min(clampMaxY, Math.max(clampMinY, y2));
            x3 = Math.min(clampMaxX, Math.max(clampMinX, x3));
            y3 = Math.min(clampMaxY, Math.max(clampMinY, y3));

            // draw the check as two gentle quad curves to create a slightly wavy look
            QuadCurve2D.Float q1 = new QuadCurve2D.Float();
            QuadCurve2D.Float q2 = new QuadCurve2D.Float();

            // control points: nudge them to create a soft wave
            int c1x = (x1 + x2) / 2 - 1;
            // increase control offsets to produce a more noticeable wave
            int c1y = (y1 + y2) / 2 + 4;
            int c2x = (x2 + x3) / 2 + 1;
            int c2y = (y2 + y3) / 2 - 4;

            q1.setCurve(x1, y1, c1x, c1y, x2, y2);
            q2.setCurve(x2, y2, c2x, c2y, x3, y3);

            g2.draw(q1);
            g2.draw(q2);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA);
    }

    private void drawLineForRow(Graphics2D g2, int docX, int docY, int startX, int checkboxSize, int rowTop, int row) {
        // draw a straight grey line and keep it inside the rounded document
        g2.setColor(LINE_COLOR);
        g2.setStroke(new BasicStroke(1.2f));
        int tx = startX + checkboxSize + 3;
        int ty = rowTop + checkboxSize / 2;
        int txEnd = docX + ICON_WIDTH - 3; // extend a little further but stay within border

        // save and apply a gentle clip so strokes do not draw outside rounded corners
        java.awt.Shape prevClip = g2.getClip();
        int clipInset = 1; // 1px inset keeps content inside rounded border without clipping strokes
        g2.setClip(docX + clipInset, docY + clipInset, ICON_WIDTH - clipInset * 2, ICON_HEIGHT - clipInset * 2);
        g2.drawLine(tx, ty, txEnd, ty);
        g2.setClip(prevClip);
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

