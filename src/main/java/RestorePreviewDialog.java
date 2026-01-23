import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 * Dialog that shows restore preview and diff; confirmation triggers restore action.
 */
public class RestorePreviewDialog {
    public static void showDialog(Component parent, List<Task> currentTasks, List<Task> backupTasks, File backupFile, Runnable onRestore, RestorePreview preview) {
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
            sb.append("<div style='margin-top:8px; font-size:12px; color:#333;'><b>Checklists in backup:</b><ul style='margin:6px 0 0 18px;'>");
            for (Map.Entry<String,String> e : preview.checklists.entrySet()) {
                String id = e.getKey();
                String name = e.getValue();
                int cnt = preview.checklistTaskCounts.getOrDefault(id, 0);
                sb.append("<li>").append(escapeHtml(name)).append(" — ").append(cnt).append(" task(s)</li>");
            }
            sb.append("</ul></div>");
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

        JButton restoreButton = new JButton("Restore from Backup");
        restoreButton.setFont(restoreButton.getFont().deriveFont(java.awt.Font.BOLD, 12.0f));
        restoreButton.setBackground(new java.awt.Color(200, 50, 50));
        restoreButton.setForeground(java.awt.Color.BLACK);
        restoreButton.setFocusPainted(false);
        restoreButton.setPreferredSize(new java.awt.Dimension(160, 35));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(cancelButton.getFont().deriveFont(12.0f));
        cancelButton.setFocusPainted(false);
        cancelButton.setPreferredSize(new java.awt.Dimension(100, 35));

        restoreButton.addActionListener(e -> {
            dialog.dispose();
            onRestore.run();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(restoreButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
