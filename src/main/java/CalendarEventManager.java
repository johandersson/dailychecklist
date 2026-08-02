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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages calendar event data persistence and lookup, mirroring the
 * responsibilities of {@link ReminderManager} for the calendar-event domain.
 */
public class CalendarEventManager {
    private final String eventFileName;
    private List<CalendarEvent> cachedEvents;
    private boolean eventsDirty = true;
    private Component parentComponent;

    public CalendarEventManager(String eventFileName) {
        this.eventFileName = eventFileName;
    }

    public void setParentComponent(Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    /**
     * Gets all imported calendar events, using the in-memory cache when possible.
     */
    public List<CalendarEvent> getEvents() {
        if (cachedEvents != null && !eventsDirty) {
            return new ArrayList<>(cachedEvents);
        }
        List<CalendarEvent> events = CalendarEventStore.loadFromProperties(eventFileName, parentComponent);
        cachedEvents = new ArrayList<>(events);
        eventsDirty = false;
        return events;
    }

    /**
     * Returns the events that occur on the given date.
     */
    public List<CalendarEvent> getEventsOn(LocalDate date) {
        List<CalendarEvent> result = new ArrayList<>();
        for (CalendarEvent event : getEvents()) {
            if (event.occursOn(date)) {
                result.add(event);
            }
        }
        return result;
    }

    public void addEvent(CalendarEvent event) {
        addEvents(java.util.Collections.singletonList(event));
    }

    /**
     * Adds multiple events in a single persistence call (used by calendar import,
     * which may bring in dozens of events at once).
     */
    public void addEvents(List<CalendarEvent> newEvents) {
        if (newEvents == null || newEvents.isEmpty()) return;
        List<CalendarEvent> events = getEvents();
        events.addAll(newEvents);
        CalendarEventStore.saveToProperties(events, eventFileName);
        cachedEvents = events;
        eventsDirty = false;
    }

    public void removeEvent(CalendarEvent event) {
        List<CalendarEvent> events = getEvents();
        events.remove(event);
        CalendarEventStore.saveToProperties(events, eventFileName);
        cachedEvents = events;
        eventsDirty = false;
    }
}
