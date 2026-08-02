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
import java.util.Objects;

/**
 * An event imported from an external calendar (ICS file).
 * Mirrors the shape of {@link Reminder} (recurring via a weekday bitmask,
 * or a concrete date range) but represents an independent domain concept:
 * calendar events are not tied to a checklist or task.
 */
public class CalendarEvent {
    private final String uid;
    private final String title;
    private final String location; // nullable
    private final String description; // nullable
    private final int startYear, startMonth, startDay, startHour, startMinute;
    private final int endYear, endMonth, endDay, endHour, endMinute;
    private final boolean allDay;
    private final boolean recurring;
    // bit 0 = Monday .. bit 6 = Sunday (same convention as Reminder.getDaysBitmask())
    private final int daysBitmask;

    /**
     * Creates a single-occurrence event spanning a concrete date/time range.
     */
    public CalendarEvent(String uid, String title, String location, String description,
                          int startYear, int startMonth, int startDay, int startHour, int startMinute,
                          int endYear, int endMonth, int endDay, int endHour, int endMinute,
                          boolean allDay) {
        this.uid = normalize(uid);
        this.title = (title == null || title.trim().isEmpty()) ? "Untitled event" : title;
        this.location = normalize(location);
        this.description = normalize(description);
        this.startYear = startYear; this.startMonth = startMonth; this.startDay = startDay;
        this.startHour = startHour; this.startMinute = startMinute;
        this.endYear = endYear; this.endMonth = endMonth; this.endDay = endDay;
        this.endHour = endHour; this.endMinute = endMinute;
        this.allDay = allDay;
        this.recurring = false;
        this.daysBitmask = 0;
    }

    /**
     * Creates a weekly-recurring event (e.g. imported from RRULE:FREQ=WEEKLY/DAILY).
     * Only the time-of-day matters; start/end date fields are unused (stored as 0),
     * matching the convention used by recurring {@link Reminder} instances.
     */
    public CalendarEvent(String uid, String title, String location, String description,
                          int daysBitmask, int startHour, int startMinute, int endHour, int endMinute) {
        this.uid = normalize(uid);
        this.title = (title == null || title.trim().isEmpty()) ? "Untitled event" : title;
        this.location = normalize(location);
        this.description = normalize(description);
        this.startYear = 0; this.startMonth = 0; this.startDay = 0;
        this.startHour = startHour; this.startMinute = startMinute;
        this.endYear = 0; this.endMonth = 0; this.endDay = 0;
        this.endHour = endHour; this.endMinute = endMinute;
        this.allDay = false;
        this.recurring = daysBitmask != 0;
        this.daysBitmask = daysBitmask;
    }

    private static String normalize(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s;
    }

    public String getUid() { return uid; }
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public int getStartYear() { return startYear; }
    public int getStartMonth() { return startMonth; }
    public int getStartDay() { return startDay; }
    public int getStartHour() { return startHour; }
    public int getStartMinute() { return startMinute; }
    public int getEndYear() { return endYear; }
    public int getEndMonth() { return endMonth; }
    public int getEndDay() { return endDay; }
    public int getEndHour() { return endHour; }
    public int getEndMinute() { return endMinute; }
    public boolean isAllDay() { return allDay; }
    public boolean isRecurring() { return recurring; }
    public int getDaysBitmask() { return daysBitmask; }

    /**
     * Returns true if this event occurs on the given date.
     */
    public boolean occursOn(LocalDate date) {
        if (date == null) return false;
        if (recurring) {
            int dowBit = 1 << (date.getDayOfWeek().getValue() - 1);
            return (daysBitmask & dowBit) != 0;
        }
        try {
            LocalDate start = LocalDate.of(startYear, startMonth, startDay);
            LocalDate end = LocalDate.of(endYear, endMonth, endDay);
            if (end.isBefore(start)) end = start;
            return !date.isBefore(start) && !date.isAfter(end);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * The time this event effectively starts on the given day, clipped to
     * midnight when the event began on a previous day (multi-day span).
     */
    public LocalTime effectiveStartTime(LocalDate date) {
        if (allDay) return LocalTime.MIDNIGHT;
        if (recurring) return LocalTime.of(startHour, startMinute);
        LocalDate start = LocalDate.of(startYear, startMonth, startDay);
        return date.isEqual(start) ? LocalTime.of(startHour, startMinute) : LocalTime.MIDNIGHT;
    }

    /**
     * The time this event effectively ends on the given day, clipped to just
     * before midnight when the event continues into a following day.
     */
    public LocalTime effectiveEndTime(LocalDate date) {
        if (allDay) return LocalTime.of(23, 59);
        if (recurring) {
            LocalTime end = LocalTime.of(endHour, endMinute);
            LocalTime start = LocalTime.of(startHour, startMinute);
            return end.isAfter(start) ? end : LocalTime.of(23, 59);
        }
        LocalDate end = LocalDate.of(endYear, endMonth, endDay);
        return date.isEqual(end) ? LocalTime.of(endHour, endMinute) : LocalTime.of(23, 59);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CalendarEvent)) return false;
        CalendarEvent other = (CalendarEvent) obj;
        if (uid != null && other.uid != null) {
            return uid.equals(other.uid);
        }
        return startYear == other.startYear && startMonth == other.startMonth && startDay == other.startDay &&
                startHour == other.startHour && startMinute == other.startMinute &&
                endYear == other.endYear && endMonth == other.endMonth && endDay == other.endDay &&
                endHour == other.endHour && endMinute == other.endMinute &&
                allDay == other.allDay && recurring == other.recurring && daysBitmask == other.daysBitmask &&
                Objects.equals(title, other.title);
    }

    @Override
    public int hashCode() {
        if (uid != null) return uid.hashCode();
        return Objects.hash(title, startYear, startMonth, startDay, startHour, startMinute,
                endYear, endMonth, endDay, endHour, endMinute, allDay, recurring, daysBitmask);
    }
}
