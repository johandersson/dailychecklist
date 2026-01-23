import java.awt.BorderLayout;
import java.awt.Color;
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
 * Reusable error dialog with a short message and a collapsible stack trace area.
 */
@SuppressWarnings("serial")
public class ErrorDialog extends JDialog {
    private ErrorDialog(java.awt.Frame owner, String title, String message, Throwable t) {
        super(owner, title, true);
        initUI(message, t);
    }

    private void initUI(String message, Throwable t) {
        setLayout(new BorderLayout());
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(new Color(0, 123, 255));
        JLabel titleLabel = new JLabel("Error");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getHeader1Font());
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel msg = new JLabel("<html>" + escapeHtml(message).replaceAll("\n", "<br>") + "</html>");
        msg.setFont(FontManager.getTaskListFont());
        content.add(msg, BorderLayout.NORTH);

        // Collapsible stack trace area
        JTextArea trace = new JTextArea();
        trace.setEditable(false);
        trace.setFont(FontManager.getSmallFont());
        trace.setLineWrap(true);
        trace.setWrapStyleWord(true);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            trace.setText(sw.toString());
            trace.setCaretPosition(0);
        } else {
            trace.setText("");
        }
        trace.setVisible(false);

        JScrollPane scroll = new JScrollPane(trace);
        scroll.setPreferredSize(new Dimension(600, 240));
        scroll.setVisible(false);
        content.add(scroll, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton details = new JButton("Show details");
        details.setFont(FontManager.getButtonFont());
        details.addActionListener((ActionEvent e) -> {
            boolean showing = scroll.isVisible();
            scroll.setVisible(!showing);
            trace.setVisible(!showing);
            details.setText(showing ? "Show details" : "Hide details");
            pack();
        });
        JButton close = new JButton("Close");
        close.setFont(FontManager.getButtonFont());
        close.addActionListener(e -> dispose());
        btns.add(details);
        btns.add(close);

        add(content, BorderLayout.CENTER);
        add(btns, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static void showError(java.awt.Component parent, String message, Throwable t) {
        java.awt.Frame owner = parent instanceof java.awt.Frame frame ? frame : null;
        ErrorDialog d = new ErrorDialog(owner, "Error - Daily Checklist", message, t);
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    public static void showError(java.awt.Component parent, String message) {
        showError(parent, message, null);
    }

    // HTML-specific error dialog extracted to HtmlErrorDialog
}
