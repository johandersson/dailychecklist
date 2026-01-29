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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

/**
 * Base class for list cell renderers that display an icon and text.
 * Provides common functionality for rendering list items with icons.
 */
@SuppressWarnings("serial")
public abstract class IconListCellRenderer<T> extends JPanel implements ListCellRenderer<T> {
    private static final long serialVersionUID = 1L;

    protected boolean isSelected;
    protected T value;
    protected Icon icon;
    protected Icon rightIcon;
    // Reserve a fixed space on the right so rows align whether or not a right-side icon is present
    protected static final int RIGHT_ICON_SPACE = UiLayout.RIGHT_ICON_SPACE;

    @SuppressWarnings("this-escape")
    public IconListCellRenderer() {
        setPreferredSize(new Dimension(200, 36));
        setFont(FontManager.getTaskListFont());
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends T> list, T value, int index,
            boolean isSelected, boolean cellHasFocus) {
        this.isSelected = isSelected;
        this.value = value;

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            // expose selection state for icons rendered within this component
            putClientProperty("selected", Boolean.TRUE);
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            putClientProperty("selected", Boolean.FALSE);
        }

        // Update icon based on the value
        this.icon = getIconForValue(value);
        this.rightIcon = getRightIconForValue(value);

        return this;
    }

    /**
     * Subclasses may override to provide an icon drawn at the right end of the row.
     */
    protected Icon getRightIconForValue(T value) {
        return null;
    }

    /**
     * Subclasses must implement this to provide the appropriate icon for the given value.
     */
    protected abstract Icon getIconForValue(T value);

    /**
     * Subclasses must implement this to provide the text to display for the given value.
     */
    protected abstract String getTextForValue(T value);

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Enable anti-aliasing for smoother rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int iconX = 5;
        int textX = 42; // Default space for icon (align with task lists)

        // Draw icon if present
        if (icon != null) {
            icon.paintIcon(this, g2, iconX, getHeight() / 2 - icon.getIconHeight() / 2);
            textX = iconX + icon.getIconWidth() + 10;
        }

        // Draw text, truncating if needed to avoid overlapping the reserved right icon space
        g2.setColor(getForeground());
        g2.setFont(getFont());
        String text = getTextForValue(value);
        if (text != null) {
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int textY = getHeight() / 2 + (fm.getAscent() - fm.getDescent()) / 2;
            int availableWidth = getWidth() - textX - RIGHT_ICON_SPACE - 6;
            String drawText = text;
            if (fm.stringWidth(drawText) > availableWidth && availableWidth > 12) {
                // Truncate with ellipsis
                while (fm.stringWidth(drawText + "…") > availableWidth && drawText.length() > 0) {
                    drawText = drawText.substring(0, drawText.length() - 1);
                }
                drawText = drawText + "…";
            }
            g2.drawString(drawText, textX, textY);
        }

        // Draw right-side icon (aligned across rows)
        if (rightIcon != null) {
            int iconXRight = getWidth() - rightIcon.getIconWidth() - 6;
            int iconYRight = getHeight() / 2 - rightIcon.getIconHeight() / 2;
            rightIcon.paintIcon(this, g2, iconXRight, iconYRight);
        }
    }
}