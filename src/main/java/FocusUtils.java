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
        ActionListener al = new ActionListener() {
            private int attempts = 0;
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                attempts++;
                java.awt.Component current = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                System.out.println("[DEBUG] FocusUtils timer fired (attempt " + attempts + "), current focus owner: " + (current == null ? "null" : current.getClass().getName()));
                if (comp.isShowing() && comp.requestFocusInWindow()) {
                    System.out.println("[DEBUG] FocusUtils: requestFocusInWindow() returned true on attempt " + attempts);
                    ((Timer) e.getSource()).stop();
                    return;
                }
                // If not succeeded and attempts < 3, schedule another try
                if (attempts < 3) {
                    Timer retry = new Timer(300, this);
                    retry.setRepeats(false);
                    retry.start();
                }
            }
        };
        // 300ms initial delay to give windowing system more time to complete focus transfers
        Timer t = new Timer(300, al);
        t.setRepeats(false);
        SwingUtilities.invokeLater(t::start);
    }
}
