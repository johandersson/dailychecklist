import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Small helper to restore focus to components.
 */
public class FocusUtils {
    public static void restoreFocusLater(final JComponent comp) {
        if (comp == null) return;
        SwingUtilities.invokeLater(() -> {
            if (comp.isShowing()) {
                comp.requestFocusInWindow();
            }
        });
    }
}
