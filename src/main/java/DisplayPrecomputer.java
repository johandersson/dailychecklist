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

    /**
     * Precompute for the provided list of tasks. If showChecklistInfo is true, the checklist
     * name is appended in parentheses where applicable (uses TaskManager to resolve names).
     */
    public static void precomputeForList(List<Task> tasks, TaskManager taskManager, boolean showChecklistInfo) {
        if (tasks == null || tasks.isEmpty()) return;
        java.awt.Font font = FontManager.getTaskListFont();
        FontMetrics fm = FontMetricsCache.get(font);

        for (Task t : tasks) {
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
                int w = fm.charWidth(base.charAt(i));
                cum[i] = (i == 0) ? w : (cum[i - 1] + w);
            }
            t.cachedCumulativeCharWidthsMain = cum;
        }
    }
}
