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
                System.out.println("[DEBUG] FocusUtils initial timer fired (attempt " + attempts + "), current focus owner: " + (current == null ? "null" : current.getClass().getName()));
                if (comp.isShowing() && comp.requestFocusInWindow()) {
                    System.out.println("[DEBUG] FocusUtils: requestFocusInWindow() returned true on attempt " + attempts);
                    ((Timer) e.getSource()).stop();
                    return;
                }
                // Retry once after another delay if the first attempt didn't get focus
                if (attempts >= 1) {
                    // schedule one more try
                    Timer retry = new Timer(200, evt -> {
                        java.awt.Component currentRetry = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                        System.out.println("[DEBUG] FocusUtils retry timer fired, current focus owner: " + (currentRetry == null ? "null" : currentRetry.getClass().getName()));
                        if (comp.isShowing()) comp.requestFocusInWindow();
                    });
                    retry.setRepeats(false);
                    retry.start();
                }
            }
        };
        // 200ms delay to give windowing system more time to complete focus transfers
        Timer t = new Timer(200, al);
        t.setRepeats(false);
        SwingUtilities.invokeLater(t::start);
    }
}
