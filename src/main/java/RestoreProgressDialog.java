import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

/**
 * Simple non-blocking progress dialog for long-running restore operations.
 * Shows a green progress bar at the bottom, elapsed time and an estimating ETA.
 */
@SuppressWarnings("serial")
public class RestoreProgressDialog extends JDialog {
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel messageLabel = new JLabel("Loading file...");
    private final JLabel timeLabel = new JLabel("Elapsed: 00:00:00  Est: calculating...");
    private final Timer tick;
    private long startMillis;
    private volatile boolean done = false;

    public RestoreProgressDialog(Window owner, String title) {
        super(owner instanceof Frame ? (Frame) owner : null);
        setTitle(title == null ? "Progress" : title);
        setModal(false); // non-blocking
        initComponents();
        // Timer updates the progress animation and time label
        tick = new Timer(100, new ActionListener() {
            private int simulated = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                long elapsed = System.currentTimeMillis() - startMillis;
                // Simple simulated progress: ramp up to 95% while not done
                if (!done) {
                    simulated = Math.min(95, simulated + 5);
                    progressBar.setValue(simulated);
                }
                // If done, ensure completion
                if (done) {
                    progressBar.setValue(100);
                }
                timeLabel.setText("Elapsed: " + formatElapsed(elapsed) + (done ? "  Done" : "  Est: calculating..."));
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        messageLabel.setFont(FontManager.getTaskListFont());
        center.add(messageLabel, BorderLayout.NORTH);
        timeLabel.setFont(FontManager.getSmallFont());
        center.add(timeLabel, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // Bottom green bar
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(new Color(34, 139, 34)); // darker green background
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(76, 175, 80)); // bright green bar
        progressBar.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        progressBar.setValue(0);
        bottom.add(progressBar, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setSize(480, 140);
        setLocationRelativeTo(null);
    }

    private static String formatElapsed(long ms) {
        long s = ms / 1000;
        long hh = s / 3600;
        long mm = (s % 3600) / 60;
        long ss = s % 60;
        return String.format("%02d:%02d:%02d", hh, mm, ss);
    }

    /**
     * Run the provided background task while showing this dialog.
     * The dialog is modeless and will not block the EDT.
     */
    public void runTask(Runnable backgroundTask) {
        startMillis = System.currentTimeMillis();
        done = false;
        tick.start();
        setVisible(true);
        // Run background task off the EDT
        Thread worker = new Thread(() -> {
            try {
                backgroundTask.run();
            } finally {
                done = true;
                // Ensure dialog is visible for at least 500ms
                long elapsed = System.currentTimeMillis() - startMillis;
                long remaining = Math.max(0, 500 - elapsed);
                try { Thread.sleep(remaining); } catch (InterruptedException ignored) {}
                SwingUtilities.invokeLater(() -> {
                    tick.stop();
                    setVisible(false);
                    dispose();
                });
            }
        }, "RestoreProgressWorker");
        worker.setDaemon(true);
        worker.start();
    }
}
