/*
 * Daily Checklist
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
