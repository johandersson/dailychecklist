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
 */import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

@SuppressWarnings("serial")
public class CheckboxListCellRenderer extends JPanel implements ListCellRenderer<Task> {
    private static final long serialVersionUID = 1L;
    private boolean isChecked;
    private String taskName;
    private String weekdayAbbreviation;
    private Color weekdayColor;
    private boolean isWeekdayTask;
    private Font circleFont; // Font for the text inside the circle
    private String doneDate; // Timestamp when task was completed
    private boolean isSelected; // Whether this item is currently selected
    
    // Cached checkmark image for performance
    private static final BufferedImage checkmarkImage;

    // Mapping of weekdays to abbreviations and WCAG-compliant colors
    private static final Map<String, String> WEEKDAY_ABBREVIATIONS = new HashMap<>();
    private static final Map<String, Color> WEEKDAY_COLORS = new HashMap<>();

    static {
        WEEKDAY_ABBREVIATIONS.put("monday", "Mo");
        WEEKDAY_ABBREVIATIONS.put("tuesday", "Tu");
        WEEKDAY_ABBREVIATIONS.put("wednesday", "We");
        WEEKDAY_ABBREVIATIONS.put("thursday", "Th");
        WEEKDAY_ABBREVIATIONS.put("friday", "Fr");
        WEEKDAY_ABBREVIATIONS.put("saturday", "Sa");
        WEEKDAY_ABBREVIATIONS.put("sunday", "Su");

        // WCAG 2.1 compliant colors
        WEEKDAY_COLORS.put("monday", new Color(165, 42, 42));   // Brown
        WEEKDAY_COLORS.put("tuesday", new Color(0, 90, 156));   // Dark Blue
        WEEKDAY_COLORS.put("wednesday", new Color(139, 128, 0)); // Dark Yellow
        WEEKDAY_COLORS.put("thursday", new Color(34, 139, 34));  // Forest Green
        WEEKDAY_COLORS.put("friday", new Color(139, 69, 19));    // Saddle Brown
        WEEKDAY_COLORS.put("saturday", new Color(255, 69, 0));   // Dark Orange
        WEEKDAY_COLORS.put("sunday", new Color(199, 21, 133));   // Dark Pink
        
        // Pre-render checkmark image for performance
        checkmarkImage = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = checkmarkImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(76, 175, 80)); // Material green checkmark
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(5, 10, 10, 15);
        g2.drawLine(10, 15, 17, 5);
        g2.dispose();
    }

    @SuppressWarnings("this-escape")
    public CheckboxListCellRenderer() {
        setPreferredSize(new Dimension(200, 50)); // Increased height for timestamp display
        this.circleFont = getAvailableFont("Yu Gothic UI", Font.BOLD, 12); // Font for circle text
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Task> list, Task task, int index, boolean isSelected, boolean cellHasFocus) {
        this.isChecked = task.isDone();
        this.taskName = task.getName();
        String weekdayKey = task.getWeekday() != null ? task.getWeekday().toLowerCase() : null;
        this.weekdayAbbreviation = WEEKDAY_ABBREVIATIONS.get(weekdayKey);
        this.weekdayColor = WEEKDAY_COLORS.get(weekdayKey);
        this.isWeekdayTask = task.getWeekday() != null && WEEKDAY_ABBREVIATIONS.containsKey(weekdayKey);
        this.doneDate = task.getDoneDate();
        this.isSelected = isSelected;

        setFont(FontManager.getTaskListFont()); // Use consistent font for all task lists
        setOpaque(true); // Ensure background is painted
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
        }

        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Enable anti-aliasing for smoother rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int textStartX = 40; // Adjusted text position to make space for checkbox

        // Define checkbox dimensions
        int checkboxX = 10, checkboxY = getHeight() / 2 - 10, checkboxSize = 20;

        // Draw subtle shadow behind checkbox
        g2.setColor(new Color(200, 200, 200, 100)); // Light gray shadow with transparency
        g2.fillRoundRect(checkboxX + 2, checkboxY + 2, checkboxSize, checkboxSize, 8, 8);

        // Draw checkbox outline
        g2.setColor(new Color(120, 120, 120)); // Softer gray outline
        g2.drawRoundRect(checkboxX, checkboxY, checkboxSize, checkboxSize, 8, 8);

        // Fill checkbox with white
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(checkboxX + 1, checkboxY + 1, checkboxSize - 2, checkboxSize - 2, 8, 8);

        // Draw checkmark if selected
        if (isChecked) {
            g2.drawImage(checkmarkImage, checkboxX, checkboxY, null);
        }

        // Draw the weekday circle **only if it's a weekday task**
        if (isWeekdayTask) {
            int circleX = textStartX, circleY = getHeight() / 2 - 15, circleSize = 30;
            g2.setColor(weekdayColor);
            g2.fillOval(circleX, circleY, circleSize, circleSize);

            // Draw the weekday abbreviation inside the circle (centered)
            g2.setColor(Color.WHITE);
            g2.setFont(circleFont);
            FontMetrics fm = g2.getFontMetrics();
            int textX = circleX + (circleSize - fm.stringWidth(weekdayAbbreviation)) / 2;
            int textY = circleY + circleSize / 2 + (fm.getAscent() - fm.getDescent()) / 2;

            g2.drawString(weekdayAbbreviation, textX, textY);

            textStartX = circleX + circleSize + 10; // Adjust task text position
        }

        // Draw the task text next to the checkbox and circle
        g2.setColor(getForeground());
        g2.setFont(getFont());
        g2.drawString(taskName, textStartX, getHeight() / 2 + 5);
        
        // Draw timestamp if task is checked - always in black
        if (isChecked && doneDate != null && !doneDate.isEmpty()) {
            g2.setColor(Color.BLACK);
            g2.setFont(getFont().deriveFont(Font.PLAIN, FontManager.SIZE_SMALL));
            g2.drawString("✓ " + doneDate, textStartX, getHeight() / 2 + 20);
        }
    }


    /**
     * Checks if a font is available on the system. If not, falls back to Arial.
     */
    private Font getAvailableFont(String preferredFontName, int style, int size) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();

        for (String font : availableFonts) {
            if (font.equalsIgnoreCase(preferredFontName)) {
                return new Font(preferredFontName, style, size);
            }
        }
        return new Font(FontManager.FONT_NAME, style, size); // Fallback to Yu Gothic UI
    }
}


