import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight non-blocking metrics collector for write-thread metrics.
 * Producers call {@link #record(String)} which is lock-free. A background
 * flusher periodically drains the queue and emits logs from its own thread.
 */
public final class MetricsCollector {
    private static final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private static final ScheduledExecutorService flusher = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "metrics-flusher");
        t.setDaemon(true);
        return t;
    });

    static {
        flusher.scheduleAtFixedRate(MetricsCollector::flush, 1, 1, TimeUnit.SECONDS);
    }

    public static void record(String metric) {
        if (metric == null) return;
        queue.offer(metric);
    }

    private static void flush() {
        StringBuilder batch = new StringBuilder();
        String s;
        boolean any = false;
        while ((s = queue.poll()) != null) {
            any = true;
            batch.append(s).append('\n');
            if (batch.length() > 8_192) break; // flush in chunks
        }
        if (any) {
            java.util.logging.Logger.getLogger(MetricsCollector.class.getName()).fine("Metrics:\n" + batch.toString());
        }
    }

    private MetricsCollector() {}
}
