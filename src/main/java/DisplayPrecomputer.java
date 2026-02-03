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

import java.awt.FontMetrics;
import java.util.List;

/**
 * Precomputes display name and cumulative char widths for tasks when the model changes.
 * This reduces per-paint string operations and repeated metrics queries.
 */
public final class DisplayPrecomputer {
    private DisplayPrecomputer() {}

    // Cache for character widths to avoid repeated FontMetrics.charWidth() calls
    private static final java.util.Map<java.awt.Font, int[]> charWidthCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Precompute for the provided list of tasks. If showChecklistInfo is true, the checklist
     * name is appended in parentheses where applicable (uses TaskManager to resolve names).
     * Only recomputes for tasks marked as dirty to avoid unnecessary work.
     */
    public static void precomputeForList(List<Task> tasks, TaskManager taskManager, boolean showChecklistInfo) {
        if (tasks == null || tasks.isEmpty()) return;
        java.awt.Font font = FontManager.getTaskListFont();
        FontMetrics fm = FontMetricsCache.get(font);

        // Get or create character width cache for this font
        int[] charWidths = charWidthCache.computeIfAbsent(font, f -> {
            int[] widths = new int[256]; // ASCII range
            for (int i = 0; i < 256; i++) {
                widths[i] = fm.charWidth((char) i);
            }
            return widths;
        });

        // Count tasks that need recomputation to optimize for small updates
        int dirtyCount = 0;
        for (Task t : tasks) {
            if (t.isDisplayDirty()) dirtyCount++;
        }
        
        // For large lists with many dirty tasks, process in batches to remain responsive
        final int BATCH_SIZE = 50;
        boolean shouldBatch = dirtyCount > BATCH_SIZE;
        int processed = 0;

        for (Task t : tasks) {
            // Skip if not dirty (already computed)
            if (!t.isDisplayDirty()) continue;
            
            String base = t.getName() != null ? t.getName() : "";
            if (showChecklistInfo && t.getChecklistId() != null && taskManager != null) {
                String cname = taskManager.getChecklistNameById(t.getChecklistId());
                if (cname != null && !cname.trim().isEmpty()) {
                    base = base + " (" + cname + ")";
                }
            }
            t.cachedDisplayFullName = base;

            // build cumulative char widths (so we can binary-search for truncation length)
            int n = base.length();
            int[] cum = new int[n];
            for (int i = 0; i < n; i++) {
                char c = base.charAt(i);
                int w = (c < 256) ? charWidths[c] : fm.charWidth(c); // Use cache for ASCII, fallback for others
                cum[i] = (i == 0) ? w : (cum[i - 1] + w);
            }
            t.cachedCumulativeCharWidthsMain = cum;
            
            // Mark as clean after computation
            t.markDisplayClean();
            
            // Yield to EDT periodically for large batch operations to keep UI responsive
            if (shouldBatch && ++processed % BATCH_SIZE == 0) {
                Thread.yield();
            }
        }
    }
}
