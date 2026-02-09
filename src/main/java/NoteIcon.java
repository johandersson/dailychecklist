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
import java.awt.geom.Path2D;
import javax.swing.Icon;

/**
 * Icon representing a note indicator.
 * Draws a document/note shape with a folded corner.
 */
public class NoteIcon implements Icon {
    private final int size;
    private static final Color NOTE_COLOR = new Color(255, 215, 0); // Golden yellow
    private static final Color NOTE_BORDER = new Color(200, 160, 0); // Darker yellow-gold
    private static final Color NOTE_FOLD = new Color(230, 190, 50); // Medium yellow for fold

    public NoteIcon(int size) {
        this.size = size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate dimensions
        int foldSize = (int)(size * 0.25); // Size of the folded corner
        
        // Create the main note shape (rectangle with folded corner)
        Path2D.Float notePath = new Path2D.Float();
        notePath.moveTo(x, y);
        notePath.lineTo(x + size - foldSize, y);
        notePath.lineTo(x + size, y + foldSize);
        notePath.lineTo(x + size, y + size);
        notePath.lineTo(x, y + size);
        notePath.closePath();
        
        // Fill the note
        g2.setColor(NOTE_COLOR);
        g2.fill(notePath);
        
        // Draw the folded corner
        Path2D.Float foldPath = new Path2D.Float();
        foldPath.moveTo(x + size - foldSize, y);
        foldPath.lineTo(x + size - foldSize, y + foldSize);
        foldPath.lineTo(x + size, y + foldSize);
        foldPath.closePath();
        
        g2.setColor(NOTE_FOLD);
        g2.fill(foldPath);
        
        // Draw border around the main note
        g2.setColor(NOTE_BORDER);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(notePath);
        
        // Draw fold lines
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawLine(x + size - foldSize, y, x + size - foldSize, y + foldSize);
        g2.drawLine(x + size - foldSize, y + foldSize, x + size, y + foldSize);
        
        // Draw horizontal lines on the note to make it look like text
        g2.setColor(NOTE_BORDER);
        g2.setStroke(new BasicStroke(0.8f));
        int lineCount = 3;
        int lineSpacing = (size - foldSize - 4) / (lineCount + 1);
        int lineStartY = y + lineSpacing + 2;
        int lineMargin = (int)(size * 0.15);
        
        for (int i = 0; i < lineCount; i++) {
            int ly = lineStartY + i * lineSpacing;
            // Make lines shorter as they approach the fold
            int lineEndX = x + size - lineMargin;
            if (ly < y + foldSize) {
                lineEndX = Math.min(lineEndX, x + size - foldSize - 2);
            }
            g2.drawLine(x + lineMargin, ly, lineEndX, ly);
        }

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
