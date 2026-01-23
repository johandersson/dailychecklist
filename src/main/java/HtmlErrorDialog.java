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
            trace.setText(sw.toString());
        }

        JScrollPane scroll = new JScrollPane(trace);
        scroll.setPreferredSize(new Dimension(600, 240));
        scroll.setVisible(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
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
