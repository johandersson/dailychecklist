import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class MenuBarHyperlinkListener implements HyperlinkListener {
    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                java.awt.Desktop.getDesktop().browse(e.getURL().toURI());
            } catch (Exception ex) {
                if (!java.awt.GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(null, "Unable to open link: " + e.getURL(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}