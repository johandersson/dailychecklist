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
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Panel that displays differences between current and backup task lists.
 */
public class TaskDiffPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public TaskDiffPanel(List<Task> currentTasks, List<Task> backupTasks) {
        setLayout(new BorderLayout());
        setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // Calculate differences
        TaskListDiff diff = new TaskListDiff(currentTasks, backupTasks);

        // Create HTML content
        String htmlContent = createHtmlContent(diff);

        // Create HTML editor pane
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setText(htmlContent);
        editorPane.setEditable(false);
        editorPane.setBackground(getBackground());

        // Add scroll pane
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    private String createHtmlContent(TaskListDiff diff) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif; font-size: 12px; margin: 10px;'>");

        // Summary section
        html.append("<div style='margin-bottom: 20px;'>");
        html.append("<h3 style='color: #2E86AB; margin-bottom: 10px; border-bottom: 1px solid #ddd; padding-bottom: 5px;'>📊 Summary of Changes</h3>");
        html.append("<div style='background: #f8f9fa; border: 1px solid #dee2e6; border-radius: 5px; padding: 15px;'>");

        String[] summaryLines = diff.getSummary().split("\n");
        for (String line : summaryLines) {
            if (line.contains("Current tasks:") || line.contains("Backup tasks:")) {
                html.append("<div style='margin-bottom: 5px;'><strong>").append(line).append("</strong></div>");
            } else {
                html.append("<div style='margin-bottom: 3px; color: #495057;'>").append(line).append("</div>");
            }
        }
        html.append("</div></div>");

        // Detailed changes section
        html.append("<div>");
        html.append("<h3 style='color: #2E86AB; margin-bottom: 10px; border-bottom: 1px solid #ddd; padding-bottom: 5px;'>🔍 Detailed Changes</h3>");
        html.append("<div style='background: #f8f9fa; border: 1px solid #dee2e6; border-radius: 5px; padding: 15px; font-family: \"Courier New\", monospace; font-size: 11px;'>");

        String details = diff.getDetailedChanges();
        if (details.trim().isEmpty()) {
            html.append("<div style='color: #6c757d; font-style: italic;'>No differences found - backup matches current data.</div>");
        } else {
            // Process each line and add appropriate styling
            String[] lines = details.split("\n");
            for (String line : lines) {
                if (line.startsWith("ADDED TASKS:")) {
                    html.append("<div style='color: #28a745; font-weight: bold; margin-top: 15px; margin-bottom: 5px;'>").append(line).append("</div>");
                } else if (line.startsWith("REMOVED TASKS:")) {
                    html.append("<div style='color: #dc3545; font-weight: bold; margin-top: 15px; margin-bottom: 5px;'>").append(line).append("</div>");
                } else if (line.startsWith("MODIFIED TASKS:")) {
                    html.append("<div style='color: #ffc107; font-weight: bold; margin-top: 15px; margin-bottom: 5px;'>").append(line).append("</div>");
                } else if (line.startsWith("+ ")) {
                    html.append("<div style='color: #28a745; margin-left: 10px; margin-bottom: 2px;'>").append(escapeHtml(line)).append("</div>");
                } else if (line.startsWith("- ")) {
                    html.append("<div style='color: #dc3545; margin-left: 10px; margin-bottom: 2px;'>").append(escapeHtml(line)).append("</div>");
                } else if (line.startsWith("~ ")) {
                    html.append("<div style='color: #ffc107; margin-left: 10px; margin-bottom: 2px;'>").append(escapeHtml(line)).append("</div>");
                } else if (!line.trim().isEmpty()) {
                    html.append("<div style='margin-left: 10px; margin-bottom: 2px;'>").append(escapeHtml(line)).append("</div>");
                }
            }
        }
        html.append("</div></div>");

        html.append("</body></html>");
        return html.toString();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}