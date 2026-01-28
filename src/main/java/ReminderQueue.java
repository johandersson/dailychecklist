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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A thread-safe queue system for managing reminder notifications.
 * Ensures reminders are shown sequentially without overwhelming the user.
 */
public class ReminderQueue {
    private final Queue<Reminder> queue = new ConcurrentLinkedQueue<>();
    private final ReminderDisplayCallback callback;
    private volatile boolean dialogShowing = false;
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "reminder-queue-scheduler");
        t.setDaemon(true);
        return t;
    });

    /**
     * Callback interface for displaying reminders.
     */
    public interface ReminderDisplayCallback {
        void displayReminder(Reminder reminder);
    }

    public ReminderQueue(ReminderDisplayCallback callback) {
        this.callback = callback;
    }

    /**
     * Adds a reminder to the queue and attempts to show it if no dialog is currently displayed.
     */
    public void addReminder(Reminder reminder) {
        if (!queue.contains(reminder)) {
            queue.add(reminder);
            showNextReminder();
        }
    }

    /**
     * Called when a reminder dialog is dismissed to show the next reminder in queue.
     */
    public void onReminderDismissed() {
        dialogShowing = false;
        // Schedule showing next reminder after a short delay to prevent rapid dialogs
        scheduler.schedule(() -> {
            try {
                showNextReminder();
            } catch (Exception e) {
                // ignore - safe to swallow here
            }
        }, 500, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Shows the next reminder in the queue if no dialog is currently showing.
     */
    private void showNextReminder() {
        if (dialogShowing || queue.isEmpty()) {
            return;
        }

        Reminder reminder = queue.poll();
        if (reminder != null) {
            dialogShowing = true;
            callback.displayReminder(reminder);
        }
    }

    /**
     * Returns the current number of reminders in the queue.
     */
    public int size() {
        return queue.size();
    }

    /**
     * Checks if the queue is empty.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Clears all reminders from the queue.
     */
    public void clear() {
        queue.clear();
    }

    /**
     * Shutdown the internal scheduler used for delayed showing.
     */
    public void shutdown() {
        try {
            scheduler.shutdownNow();
            scheduler.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}