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

import java.util.List;
import javax.swing.DefaultListModel;

/**
 * Encapsulates search/query logic used by the search dialog.
 * Extracted to separate class for separation of concerns and testability.
 */
public class SearchController {
    private final TaskManager taskManager;
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "search-debounce");
        t.setDaemon(true);
        return t;
    });
    private volatile java.util.concurrent.ScheduledFuture<?> scheduled = null;
    private final Object lock = new Object();
    private final long DEBOUNCE_MS = 150;

    public SearchController(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    /**
     * Request an update for the unified model; filtering is performed off-EDT
     * with a short debounce window to keep typing snappy.
     */
    public void requestUpdateModel(String query, boolean includeAllWeekday, DefaultListModel<Object> unifiedModel) {
        final String q = query == null ? "" : query.toLowerCase();
        synchronized (lock) {
            if (scheduled != null && !scheduled.isDone()) scheduled.cancel(false);
            scheduled = scheduler.schedule(() -> doSearch(q, includeAllWeekday, unifiedModel), DEBOUNCE_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    private void doSearch(String q, boolean includeAllWeekday, DefaultListModel<Object> unifiedModel) {
        final String currentWeekday = java.time.LocalDateTime.now().getDayOfWeek().toString().toLowerCase();
        List<Task> allTasks = taskManager.getAllTasks();
        List<Task> results = new java.util.ArrayList<>();
        for (Task task : allTasks) {
            String name = task.getName();
            if (name == null) continue;
            if (!name.toLowerCase().contains(q)) continue;
            if (task.getWeekday() == null || includeAllWeekday || task.getWeekday().toLowerCase().equals(currentWeekday)) {
                results.add(task);
            }
        }

        List<Checklist> allLists = new java.util.ArrayList<>();
        for (Checklist c : taskManager.getCustomChecklists()) {
            if (c.getName() != null && c.getName().toLowerCase().contains(q)) allLists.add(c);
        }

        javax.swing.SwingUtilities.invokeLater(() -> {
            unifiedModel.clear();
            for (Checklist c : allLists) unifiedModel.addElement(c);
            for (Task t : results) unifiedModel.addElement(t);
        });
    }
}
