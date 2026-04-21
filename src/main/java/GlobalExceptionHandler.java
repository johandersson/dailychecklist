import java.awt.AWTEvent;
import java.awt.EventQueue;
import javax.swing.SwingUtilities;

/**
 * Installs global exception handlers for the application so any uncaught
 * exceptions (including EDT/boot-time exceptions) are shown in the HTML error dialog
 * with full stack traces and written to the debug log.
 */
public final class GlobalExceptionHandler {
    private GlobalExceptionHandler() { }

    public static void install() {
        // Handler for non-EDT threads
        Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
            DebugLog.d("Uncaught exception in thread %s: %s", thread.getName(), t.toString());
            // Ensure dialog is shown on EDT
            try {
                SwingUtilities.invokeLater(() -> {
                    HtmlErrorDialog.showHtmlError(null, buildStartupMessage(thread.getName()), t);
                });
            } catch (Throwable showEx) {
                // As a last resort, print to stderr
                System.err.println("Failed to show error dialog: " + showEx.getMessage());
                t.printStackTrace();
            }
        });

        // Handler for EDT: push an EventQueue that catches exceptions during dispatch
        try {
            java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {
                @Override
                protected void dispatchEvent(AWTEvent event) {
                    try {
                        super.dispatchEvent(event);
                    } catch (Throwable t) {
                        DebugLog.d("Uncaught EDT exception: %s", t.toString());
                        try {
                            HtmlErrorDialog.showHtmlError(null, buildStartupMessage("EDT"), t);
                        } catch (Throwable showEx) {
                            System.err.println("Failed to show EDT error dialog: " + showEx.getMessage());
                            t.printStackTrace();
                        }
                    }
                }
            });
        } catch (Exception e) {
            // If installing the EventQueue wrapper fails, at least log it
            DebugLog.d("Could not install EDT exception handler: %s", e.toString());
        }
    }

    private static String buildStartupMessage(String threadName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Unexpected Error</h3>");
        sb.append("<p>An unexpected error occurred while the application was starting or running.</p>");
        sb.append("<p><b>Thread:</b> ").append(threadName).append("</p>");
        sb.append("<p>You can copy the details, report the issue, or try restarting the application.</p>");
        return sb.toString();
    }
}
