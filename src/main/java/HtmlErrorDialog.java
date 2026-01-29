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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Dialog that shows preformatted HTML content and an optional collapsible stack trace.
 */
@SuppressWarnings("serial")
public class HtmlErrorDialog extends JDialog {
    private HtmlErrorDialog(java.awt.Frame owner, String title) {
        super(owner, title, true);
    }

    public static void showHtmlError(java.awt.Component parent, String htmlMessage, Throwable t) {
        java.awt.Frame owner = parent instanceof java.awt.Frame frame ? frame : null;
        HtmlErrorDialog d = new HtmlErrorDialog(owner, "Error - Daily Checklist");

        JLabel html = new JLabel(htmlMessage);
        html.setFont(FontManager.getTaskListFont());

        JTextArea trace = new JTextArea();
        trace.setEditable(false);
        trace.setFont(FontManager.getSmallFont());
        trace.setLineWrap(true);
        trace.setWrapStyleWord(true);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String stack = sw.toString();
            trace.setText(stack);
            trace.setCaretPosition(0);
            // Also write the stack trace to the debug log file for offline diagnosis
            DebugLog.d("Exception shown to user: %s", stack);
        }

        JScrollPane scroll = new JScrollPane(trace);
        scroll.setPreferredSize(new Dimension(600, 240));
        scroll.setVisible(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(html, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton details = new JButton("Show details");
        details.setFont(FontManager.getButtonFont());
        details.addActionListener((ActionEvent e) -> {
            boolean showing = scroll.isVisible();
            scroll.setVisible(!showing);
            details.setText(showing ? "Show details" : "Hide details");
            d.pack();
        });
        JButton close = new JButton("Close");
        close.setFont(FontManager.getButtonFont());
        close.addActionListener(e -> d.dispose());
        btns.add(details);
        btns.add(close);

        d.add(content, BorderLayout.CENTER);
        d.add(btns, BorderLayout.SOUTH);
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }
}
