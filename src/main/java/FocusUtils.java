import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Small helper to restore focus to components with a slight delay when needed.
 */
public class FocusUtils {
    /**
     * Request focus on the component after a short delay to avoid focus being stolen by dialogs.
     */
    public static void restoreFocusLater(final JComponent comp) {
        if (comp == null) return;
        // Use a short timer so that dialogs/dispose sequences finish first
        ActionListener al = e -> {
            if (comp.isShowing()) comp.requestFocusInWindow();
        };
        // 100ms delay should be enough
        Timer t = new Timer(100, al);
        t.setRepeats(false);
        SwingUtilities.invokeLater(t::start);
    }
}
