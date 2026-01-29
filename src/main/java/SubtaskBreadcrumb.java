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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

@SuppressWarnings("serial")
public class SubtaskBreadcrumb extends JComponent {
    private String text;
    private Font font;

    public SubtaskBreadcrumb() {
        this.text = null;
        this.font = FontManager.getTaskListFont().deriveFont(Font.PLAIN, FontManager.SIZE_SMALL);
    }

    public void setText(String text) {
        this.text = text;
        repaint();
    }

    public String getText() {
        return text;
    }

    public void setFontToUse(Font f) {
        if (f != null) this.font = f;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (text == null || text.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int avail = getWidth() - 6;
        String draw = text;
        if (avail > 8 && fm.stringWidth(draw) > avail) {
            while (fm.stringWidth(draw + "…") > avail && draw.length() > 0) {
                draw = draw.substring(0, draw.length() - 1);
            }
            draw = draw + "…";
        }
        g2.setColor(new Color(120, 120, 120));
        int y = getHeight() / 2 + (fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(draw, 3, y);
    }
}
