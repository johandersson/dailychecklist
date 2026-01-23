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
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

@SuppressWarnings("serial")
public class ChecklistWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    private CustomChecklistPanel checklistPanel;
    private final transient TaskManager taskManager;
    private final Checklist checklist;
    private final transient Runnable updateTasks;

    @SuppressWarnings("this-escape")
    public ChecklistWindow(TaskManager taskManager, Runnable updateTasks, Checklist checklist) {
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        this.checklist = checklist;
        initialize();
    }

    private void initialize() {
        setTitle("Checklist: " + checklist.getName());
        setIconImage(createAppIcon());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 700); // Increased height for split
        setLocationRelativeTo(null);

        checklistPanel = new CustomChecklistPanel(taskManager, checklist);
        JScrollPane checklistScroll = new JScrollPane(checklistPanel);
        checklistScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));

        CustomAddTaskPanel addPanel = new CustomAddTaskPanel(taskManager, () -> {
            checklistPanel.updateTasks();
            updateTasks.run();
        }, checklist.getName());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, checklistScroll, addPanel);
        splitPane.setDividerLocation(400); // Adjust as needed
        splitPane.setResizeWeight(0.7); // More space for checklist
        splitPane.setDividerSize(4);
        splitPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Make add panel flatter as well
        if (addPanel != null) addPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);

        checklistPanel.updateTasks();
    }

    /**
     * Creates a programmatic icon that looks like the checked checkbox from the app.
     */
    private java.awt.Image createAppIcon() {
        int size = 32;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = image.createGraphics();

        // Enable anti-aliasing
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // Clear background to transparent
        g2.setComposite(java.awt.AlphaComposite.Clear);
        g2.fillRect(0, 0, size, size);
        g2.setComposite(java.awt.AlphaComposite.SrcOver);

        // Define checkbox dimensions (centered)
        int checkboxSize = 24;
        int checkboxX = (size - checkboxSize) / 2;
        int checkboxY = (size - checkboxSize) / 2;

        // Draw subtle shadow
        g2.setColor(new java.awt.Color(200, 200, 200, 100));
        g2.fillRoundRect(checkboxX + 1, checkboxY + 1, checkboxSize, checkboxSize, 6, 6);

        // Draw checkbox outline
        g2.setColor(new java.awt.Color(120, 120, 120));
        g2.drawRoundRect(checkboxX, checkboxY, checkboxSize, checkboxSize, 6, 6);

        // Fill checkbox with white
        g2.setColor(java.awt.Color.WHITE);
        g2.fillRoundRect(checkboxX + 1, checkboxY + 1, checkboxSize - 2, checkboxSize - 2, 6, 6);

        // Draw checkmark
        g2.setColor(new java.awt.Color(76, 175, 80)); // Material green
        g2.setStroke(new java.awt.BasicStroke(3, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        int offsetX = checkboxX + 3;
        int offsetY = checkboxY + 6;
        g2.drawLine(offsetX + 2, offsetY + 6, offsetX + 7, offsetY + 11);
        g2.drawLine(offsetX + 7, offsetY + 11, offsetX + 15, offsetY + 1);

        g2.dispose();
        return image;
    }
}