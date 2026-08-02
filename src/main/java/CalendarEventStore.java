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
import java.awt.Component;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Loads and saves {@link CalendarEvent} instances from/to a dedicated properties file.
 * Kept separate from {@link ReminderStore} on purpose: calendar events are a distinct
 * domain concept (imported, not user-authored per checklist) so this store owns its
 * own file format extension instead of overloading the reminders file.
 */
public final class CalendarEventStore {
    private CalendarEventStore() {}

    public static List<CalendarEvent> loadFromProperties(String fileName, Component parentComponent) {
        List<CalendarEvent> events = new ArrayList<>();
        Properties props = new Properties();

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8)) {
            props.load(reader);

            int count = 0;
            for (String key : props.stringPropertyNames()) {
                if (!key.startsWith("event.")) continue;
                if (count >= MemorySafetyManager.MAX_CALENDAR_EVENTS) break;

                String value = props.getProperty(key);
                CalendarEvent event = parseEventLine(value);
                if (event != null) {
                    events.add(event);
                    count++;
                } else if (parentComponent != null) {
                    ErrorDialog.showError(parentComponent,
                            "Invalid calendar event data (key: " + key + ", value: " + value + "). This event will be skipped.",
                            new IllegalArgumentException(value));
                }
            }
        } catch (IOException e) {
            // File doesn't exist or can't be read: no events yet
        }

        return events;
    }

    private static CalendarEvent parseEventLine(String value) {
        if (value == null) return null;
        String[] parts = value.split("\\|");
        if (parts.length < 6) return null;

        try {
            String title = decode(parts[0]);
            int startYear = Integer.parseInt(parts[1].trim());
            int startMonth = Integer.parseInt(parts[2].trim());
            int startDay = Integer.parseInt(parts[3].trim());
            int startHour = Integer.parseInt(parts[4].trim());
            int startMinute = Integer.parseInt(parts[5].trim());

            String uid = null, location = null, description = null;
            int endYear = startYear, endMonth = startMonth, endDay = startDay;
            int endHour = startHour, endMinute = startMinute;
            boolean allDay = false;
            int daysBitmask = 0;

            for (int i = 6; i < parts.length; i++) {
                String token = parts[i];
                int eq = token.indexOf('=');
                if (eq < 0) continue;
                String tokenKey = token.substring(0, eq);
                String tokenVal = token.substring(eq + 1);
                switch (tokenKey) {
                    case "uid": uid = decode(tokenVal); break;
                    case "location": location = decode(tokenVal); break;
                    case "description": description = decode(tokenVal); break;
                    case "endYear": endYear = Integer.parseInt(tokenVal.trim()); break;
                    case "endMonth": endMonth = Integer.parseInt(tokenVal.trim()); break;
                    case "endDay": endDay = Integer.parseInt(tokenVal.trim()); break;
                    case "endHour": endHour = Integer.parseInt(tokenVal.trim()); break;
                    case "endMinute": endMinute = Integer.parseInt(tokenVal.trim()); break;
                    case "allDay": allDay = Boolean.parseBoolean(tokenVal.trim()); break;
                    case "days":
                        for (String d : tokenVal.split(",")) {
                            try {
                                int v = Integer.parseInt(d.trim());
                                if (v >= 1 && v <= 7) daysBitmask |= 1 << (v - 1);
                            } catch (NumberFormatException ignore) {}
                        }
                        break;
                    default: break; // forward-compatible: ignore unknown tokens
                }
            }

            if (daysBitmask != 0) {
                return new CalendarEvent(uid, title, location, description, daysBitmask, startHour, startMinute, endHour, endMinute);
            }
            return new CalendarEvent(uid, title, location, description,
                    startYear, startMonth, startDay, startHour, startMinute,
                    endYear, endMonth, endDay, endHour, endMinute, allDay);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void saveToProperties(List<CalendarEvent> events, String fileName) {
        Properties props = new Properties();
        for (int i = 0; i < events.size(); i++) {
            props.setProperty("event." + i, toLine(events.get(i)));
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8)) {
            props.store(writer, "Daily Checklist calendar events (imported from ICS files)");
        } catch (IOException e) {
            // Caller is responsible for surfacing persistence failures
        }
    }

    private static String toLine(CalendarEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append(encode(e.getTitle())).append('|')
                .append(e.getStartYear()).append('|').append(e.getStartMonth()).append('|').append(e.getStartDay()).append('|')
                .append(e.getStartHour()).append('|').append(e.getStartMinute());

        if (e.getUid() != null) sb.append('|').append("uid=").append(encode(e.getUid()));
        if (e.getLocation() != null) sb.append('|').append("location=").append(encode(e.getLocation()));
        if (e.getDescription() != null) sb.append('|').append("description=").append(encode(e.getDescription()));
        sb.append('|').append("endYear=").append(e.getEndYear());
        sb.append('|').append("endMonth=").append(e.getEndMonth());
        sb.append('|').append("endDay=").append(e.getEndDay());
        sb.append('|').append("endHour=").append(e.getEndHour());
        sb.append('|').append("endMinute=").append(e.getEndMinute());
        if (e.isAllDay()) sb.append('|').append("allDay=true");
        if (e.isRecurring()) {
            StringBuilder days = new StringBuilder();
            for (int d = 1; d <= 7; d++) {
                if ((e.getDaysBitmask() & (1 << (d - 1))) != 0) {
                    if (days.length() > 0) days.append(',');
                    days.append(d);
                }
            }
            if (days.length() > 0) sb.append('|').append("days=").append(days);
        }
        return sb.toString();
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s; // UTF-8 is always supported
        }
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return s;
        }
    }
}
