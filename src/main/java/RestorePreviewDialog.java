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
import java.awt.Component;
import java.io.File;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 * Dialog that shows restore preview and diff; confirmation triggers restore action.
 */
public class RestorePreviewDialog {
    public static void showDialog(Component parent, List<Task> currentTasks, List<Task> backupTasks, File backupFile, Runnable onMerge, Runnable onReplace, RestorePreview preview) {
        JDialog dialog = new JDialog((java.awt.Frame) parent, "Restore from Backup", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(900, 700);
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Header panel with warning
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new java.awt.Color(255, 255, 255));

        // Build header HTML with stats and per-checklist task counts. Use app font and neutral styling.
        String appFont = FontManager.FONT_NAME != null ? FontManager.FONT_NAME : javax.swing.UIManager.getFont("Label.font").getFamily();
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: ").append(escapeHtml(appFont)).append("; font-size: 13px; color:#222;'>");
        sb.append("<div style='text-align: left; margin-bottom: 10px;'>");
        sb.append("<h2 style='color: #333; margin: 0 0 6px 0; font-size: 16px;'>Restore from Backup</h2>");
        sb.append("<div style='color: #666; font-size: 12px; margin-bottom: 6px;'>Backup file: <b>").append(escapeHtml(backupFile.getName())).append("</b></div>");
        sb.append("</div>");

        // Warning line (neutral)
        sb.append("<div style='background: transparent; padding: 6px 0 6px 0; margin-bottom:6px; color:#663; font-size:12px;'><b>Warning:</b> This will overwrite your current data. Review the differences below and confirm to proceed.</div>");

        // Stats summary including morning/evening counts
        int totalCustom = 0;
        for (Integer c : preview.checklistTaskCounts.values()) totalCustom += c == null ? 0 : c;
        sb.append("<div style='margin-top:6px; font-size:13px; color:#222;'>");
        sb.append("This import will add <b>").append(preview.newChecklistCount).append("</b> custom checklist(s), import <b>").append(totalCustom).append("</b> custom task(s), <b>").append(preview.morningCount).append("</b> morning task(s) and <b>").append(preview.eveningCount).append("</b> evening task(s).");
        sb.append("</div>");

        if (!preview.checklists.isEmpty()) {
            // Show a concise summary instead of listing every checklist name.
            sb.append("<div style='margin-top:8px; font-size:12px; color:#333;'><b>Checklists in backup:</b> ");
            sb.append("<b>").append(preview.checklists.size()).append("</b> checklist(s)");
            sb.append(" — total tasks: <b>");
            int totalInBackup = 0;
            for (Integer c : preview.checklistTaskCounts.values()) totalInBackup += c == null ? 0 : c;
            sb.append(totalInBackup).append("</b>");
            sb.append("</div>");
        }

        sb.append("</body></html>");
        String headerHtml = sb.toString();

        javax.swing.JLabel headerLabel = new javax.swing.JLabel(headerHtml);
        headerPanel.add(headerLabel, BorderLayout.CENTER);
        dialog.add(headerPanel, BorderLayout.NORTH);

        // Diff panel
        TaskDiffPanel diffPanel = new TaskDiffPanel(currentTasks, backupTasks);
        dialog.add(new javax.swing.JScrollPane(diffPanel), BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 20, 10));
        buttonPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 8, 8));

        JButton restoreButton = new JButton("Merge from Backup");
        restoreButton.setFont(restoreButton.getFont().deriveFont(java.awt.Font.BOLD, 12.0f));
        restoreButton.setBackground(new java.awt.Color(200, 50, 50));
        restoreButton.setForeground(java.awt.Color.BLACK);
        restoreButton.setFocusPainted(false);
        restoreButton.setPreferredSize(new java.awt.Dimension(160, 35));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(cancelButton.getFont().deriveFont(12.0f));
        cancelButton.setFocusPainted(false);
        cancelButton.setPreferredSize(new java.awt.Dimension(100, 35));

        JButton replaceButton = new JButton("Replace with Backup");
        replaceButton.setFont(replaceButton.getFont().deriveFont(java.awt.Font.BOLD, 12.0f));
        replaceButton.setBackground(new java.awt.Color(200, 50, 50));
        replaceButton.setForeground(java.awt.Color.BLACK);
        replaceButton.setFocusPainted(false);
        replaceButton.setPreferredSize(new java.awt.Dimension(160, 35));

        restoreButton.addActionListener(e -> { dialog.dispose(); onMerge.run(); });
        replaceButton.addActionListener(e -> { dialog.dispose(); int ok = javax.swing.JOptionPane.showConfirmDialog(parent, "This will completely replace your current tasks with the backup. Continue?", "Confirm Replace", javax.swing.JOptionPane.YES_NO_OPTION); if (ok == javax.swing.JOptionPane.YES_OPTION) onReplace.run(); });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(restoreButton);
        buttonPanel.add(replaceButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
