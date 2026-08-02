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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Minimal RFC 5545 (iCalendar) VEVENT reader that produces {@link CalendarEvent} instances.
 * Only the subset of the spec needed by Daily Checklist's "today view" is supported:
 * <ul>
 *   <li>Timed events (DTSTART/DTEND with local or UTC "Z" date-times)</li>
 *   <li>All-day events (DTSTART/DTEND with VALUE=DATE)</li>
 *   <li>Simple weekly/daily recurrence (RRULE:FREQ=WEEKLY;BYDAY=... or FREQ=DAILY)</li>
 * </ul>
 * Timezone identifiers (TZID=...) other than UTC are treated as local wall-clock time;
 * more advanced recurrence rules (COUNT, UNTIL, MONTHLY, YEARLY, etc.) are intentionally
 * not expanded and are imported as single occurrences on their original date.
 */
public final class IcsCalendarParser {
    private IcsCalendarParser() {}

    private static final Map<String, Integer> WEEKDAY_BIT = new HashMap<>();
    static {
        WEEKDAY_BIT.put("MO", 0); WEEKDAY_BIT.put("TU", 1); WEEKDAY_BIT.put("WE", 2);
        WEEKDAY_BIT.put("TH", 3); WEEKDAY_BIT.put("FR", 4); WEEKDAY_BIT.put("SA", 5);
        WEEKDAY_BIT.put("SU", 6);
    }

    public static List<CalendarEvent> parse(File icsFile) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(icsFile), StandardCharsets.UTF_8)) {
            return parse(isr);
        }
    }

    public static List<CalendarEvent> parse(java.io.Reader reader) throws IOException {
        List<String> unfolded = unfoldLines(reader);
        List<CalendarEvent> events = new ArrayList<>();

        Map<String, IcsProperty> current = null;
        for (String line : unfolded) {
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase("BEGIN:VEVENT")) {
                current = new HashMap<>();
            } else if (trimmed.equalsIgnoreCase("END:VEVENT")) {
                if (current != null) {
                    CalendarEvent event = toCalendarEvent(current);
                    if (event != null) events.add(event);
                }
                current = null;
            } else if (current != null && !trimmed.isEmpty()) {
                IcsProperty prop = IcsProperty.parseLine(trimmed);
                if (prop != null) current.put(prop.name, prop);
            }
        }
        return events;
    }

    /**
     * Unfolds continuation lines: per RFC 5545 a line starting with a single
     * space or tab is a continuation of the previous physical line.
     */
    private static List<String> unfoldLines(java.io.Reader reader) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            StringBuilder currentLine = null;
            while ((line = br.readLine()) != null) {
                if ((line.startsWith(" ") || line.startsWith("\t")) && currentLine != null) {
                    currentLine.append(line.substring(1));
                } else {
                    if (currentLine != null) result.add(currentLine.toString());
                    currentLine = new StringBuilder(line);
                }
            }
            if (currentLine != null) result.add(currentLine.toString());
        }
        return result;
    }

    private static CalendarEvent toCalendarEvent(Map<String, IcsProperty> props) {
        IcsProperty dtStart = props.get("DTSTART");
        if (dtStart == null) return null; // DTSTART is mandatory

        IcsDateTime start = IcsDateTime.parse(dtStart);
        if (start == null) return null;

        IcsProperty dtEnd = props.get("DTEND");
        IcsDateTime end = dtEnd != null ? IcsDateTime.parse(dtEnd) : null;
        if (end == null) {
            end = start.allDay ? start : new IcsDateTime(start.date, start.time.plusHours(1), false);
        } else if (end.allDay) {
            // DTEND for all-day events is exclusive per RFC 5545; convert to an inclusive last day.
            LocalDate inclusiveEnd = end.date.minusDays(1);
            if (inclusiveEnd.isBefore(start.date)) inclusiveEnd = start.date;
            end = new IcsDateTime(inclusiveEnd, end.time, true);
        }

        String uid = valueOf(props.get("UID"));
        if (uid == null) uid = "ics-" + UUID.randomUUID();
        String title = unescape(valueOf(props.get("SUMMARY")));
        String location = unescape(valueOf(props.get("LOCATION")));
        String description = unescape(valueOf(props.get("DESCRIPTION")));

        int daysBitmask = parseRecurrenceBitmask(props.get("RRULE"));
        boolean allDay = start.allDay;

        if (daysBitmask != 0) {
            return new CalendarEvent(uid, title, location, description, daysBitmask,
                    start.time.getHour(), start.time.getMinute(), end.time.getHour(), end.time.getMinute());
        }

        return new CalendarEvent(uid, title, location, description,
                start.date.getYear(), start.date.getMonthValue(), start.date.getDayOfMonth(),
                start.time.getHour(), start.time.getMinute(),
                end.date.getYear(), end.date.getMonthValue(), end.date.getDayOfMonth(),
                end.time.getHour(), end.time.getMinute(), allDay);
    }

    /**
     * Converts a simple RRULE into a weekday bitmask. Only FREQ=WEEKLY (with optional
     * BYDAY) and FREQ=DAILY are recognized; anything else returns 0 (no recurrence expansion).
     */
    private static int parseRecurrenceBitmask(IcsProperty rrule) {
        if (rrule == null) return 0;
        Map<String, String> parts = new HashMap<>();
        for (String token : rrule.value.split(";")) {
            int eq = token.indexOf('=');
            if (eq > 0) parts.put(token.substring(0, eq).toUpperCase(), token.substring(eq + 1));
        }
        String freq = parts.get("FREQ");
        if (freq == null) return 0;
        if (freq.equalsIgnoreCase("DAILY")) return 0b1111111;
        if (!freq.equalsIgnoreCase("WEEKLY")) return 0;

        String byDay = parts.get("BYDAY");
        if (byDay == null || byDay.trim().isEmpty()) return 0;
        int mask = 0;
        for (String day : byDay.split(",")) {
            Integer bit = WEEKDAY_BIT.get(day.trim().toUpperCase());
            if (bit != null) mask |= 1 << bit;
        }
        return mask;
    }

    private static String valueOf(IcsProperty p) {
        return p == null ? null : p.value;
    }

    /**
     * Reverses the ICS TEXT escaping rules (backslash-escaped comma/semicolon/backslash,
     * and literal newline escapes).
     */
    private static String unescape(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n': case 'N': sb.append('\n'); i++; break;
                    case ',': sb.append(','); i++; break;
                    case ';': sb.append(';'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    default: sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** A single unparsed "NAME;PARAM=VAL:VALUE" ICS content line. */
    private static final class IcsProperty {
        final String name;
        final String value;
        final Map<String, String> params;

        private IcsProperty(String name, String value, Map<String, String> params) {
            this.name = name;
            this.value = value;
            this.params = params;
        }

        static IcsProperty parseLine(String line) {
            int colon = line.indexOf(':');
            if (colon < 0) return null;
            String head = line.substring(0, colon);
            String value = line.substring(colon + 1);

            String[] headParts = head.split(";");
            String name = headParts[0].trim().toUpperCase();
            Map<String, String> params = new HashMap<>();
            for (int i = 1; i < headParts.length; i++) {
                int eq = headParts[i].indexOf('=');
                if (eq > 0) {
                    params.put(headParts[i].substring(0, eq).toUpperCase(), headParts[i].substring(eq + 1));
                }
            }
            return new IcsProperty(name, value, params);
        }
    }

    /** A resolved date (and, unless all-day) time-of-day value for DTSTART/DTEND. */
    private static final class IcsDateTime {
        final LocalDate date;
        final java.time.LocalTime time;
        final boolean allDay;

        IcsDateTime(LocalDate date, java.time.LocalTime time, boolean allDay) {
            this.date = date;
            this.time = time;
            this.allDay = allDay;
        }

        static IcsDateTime parse(IcsProperty prop) {
            String value = prop.value.trim();
            boolean isDateOnly = "DATE".equalsIgnoreCase(prop.params.get("VALUE")) || (value.length() == 8 && !value.contains("T"));
            try {
                if (isDateOnly) {
                    LocalDate date = LocalDate.parse(value, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                    return new IcsDateTime(date, java.time.LocalTime.MIDNIGHT, true);
                }

                boolean utc = value.endsWith("Z");
                String core = utc ? value.substring(0, value.length() - 1) : value;
                LocalDate date = LocalDate.parse(core.substring(0, 8), java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                int hour = Integer.parseInt(core.substring(9, 11));
                int minute = Integer.parseInt(core.substring(11, 13));
                LocalDateTime local = LocalDateTime.of(date, java.time.LocalTime.of(hour, minute));

                if (utc) {
                    ZonedDateTime utcZdt = local.atZone(ZoneOffset.UTC);
                    local = utcZdt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                }
                return new IcsDateTime(local.toLocalDate(), local.toLocalTime(), false);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
