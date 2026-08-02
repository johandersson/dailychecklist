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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure (no AWT dependency) algorithm that assigns non-overlapping column slots to
 * a day's worth of timed calendar events, so a renderer can lay them out side by
 * side without clipping. Kept separate from {@link TodayPanel} so the placement
 * logic can be unit tested without a graphics environment.
 */
public final class CalendarEventLayout {
    private CalendarEventLayout() {}

    /** A calendar event positioned within its overlap cluster for a given day. */
    public static final class PositionedEvent {
        public final CalendarEvent event;
        public final LocalTime start;
        public final LocalTime end;
        public final int column;
        public final int columnCount;

        PositionedEvent(CalendarEvent event, LocalTime start, LocalTime end, int column, int columnCount) {
            this.event = event;
            this.start = start;
            this.end = end;
            this.column = column;
            this.columnCount = columnCount;
        }
    }

    /**
     * Lays out the given events for the given day, assigning each a column index
     * and the total column count of its overlap cluster so equal-width columns
     * can be computed by the caller.
     */
    public static List<PositionedEvent> layout(List<CalendarEvent> events, LocalDate day) {
        List<TimedEvent> timed = new ArrayList<>();
        for (CalendarEvent event : events) {
            if (!event.occursOn(day)) continue;
            LocalTime start = event.effectiveStartTime(day);
            LocalTime end = event.effectiveEndTime(day);
            if (!end.isAfter(start)) end = start.plusMinutes(30);
            timed.add(new TimedEvent(event, start, end));
        }
        timed.sort((a, b) -> {
            int cmp = a.start.compareTo(b.start);
            return cmp != 0 ? cmp : a.end.compareTo(b.end);
        });

        List<PositionedEvent> result = new ArrayList<>();
        List<TimedEvent> cluster = new ArrayList<>();
        LocalTime clusterEndMax = null;

        for (TimedEvent te : timed) {
            if (cluster.isEmpty() || te.start.isBefore(clusterEndMax)) {
                cluster.add(te);
                clusterEndMax = (clusterEndMax == null || te.end.isAfter(clusterEndMax)) ? te.end : clusterEndMax;
            } else {
                assignColumns(cluster, result);
                cluster = new ArrayList<>();
                cluster.add(te);
                clusterEndMax = te.end;
            }
        }
        if (!cluster.isEmpty()) assignColumns(cluster, result);

        return result;
    }

    private static void assignColumns(List<TimedEvent> cluster, List<PositionedEvent> result) {
        List<LocalTime> columnEndTimes = new ArrayList<>();
        int[] assignedColumn = new int[cluster.size()];

        for (int i = 0; i < cluster.size(); i++) {
            TimedEvent te = cluster.get(i);
            int col = -1;
            for (int c = 0; c < columnEndTimes.size(); c++) {
                if (!columnEndTimes.get(c).isAfter(te.start)) {
                    col = c;
                    break;
                }
            }
            if (col == -1) {
                columnEndTimes.add(te.end);
                col = columnEndTimes.size() - 1;
            } else {
                columnEndTimes.set(col, te.end);
            }
            assignedColumn[i] = col;
        }

        int columnCount = columnEndTimes.size();
        for (int i = 0; i < cluster.size(); i++) {
            TimedEvent te = cluster.get(i);
            result.add(new PositionedEvent(te.event, te.start, te.end, assignedColumn[i], columnCount));
        }
    }

    private static final class TimedEvent {
        final CalendarEvent event;
        final LocalTime start;
        final LocalTime end;

        TimedEvent(CalendarEvent event, LocalTime start, LocalTime end) {
            this.event = event;
            this.start = start;
            this.end = end;
        }
    }
}
