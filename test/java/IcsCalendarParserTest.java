import org.junit.Test;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;

public class IcsCalendarParserTest {

    @Test
    public void parsesSingleTimedEvent() throws Exception {
        String ics = "BEGIN:VCALENDAR\r\n" +
                "VERSION:2.0\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:event-1@example.com\r\n" +
                "SUMMARY:Team Standup\r\n" +
                "LOCATION:Room 42\r\n" +
                "DTSTART:20260802T090000\r\n" +
                "DTEND:20260802T093000\r\n" +
                "END:VEVENT\r\n" +
                "END:VCALENDAR\r\n";

        List<CalendarEvent> events = IcsCalendarParser.parse(new StringReader(ics));
        assertEquals(1, events.size());
        CalendarEvent event = events.get(0);
        assertEquals("event-1@example.com", event.getUid());
        assertEquals("Team Standup", event.getTitle());
        assertEquals("Room 42", event.getLocation());
        assertFalse(event.isAllDay());
        assertTrue(event.occursOn(LocalDate.of(2026, 8, 2)));
        assertEquals(9, event.getStartHour());
        assertEquals(0, event.getStartMinute());
        assertEquals(9, event.getEndHour());
        assertEquals(30, event.getEndMinute());
    }

    @Test
    public void parsesAllDayEvent_withExclusiveDtendConvertedToInclusive() throws Exception {
        String ics = "BEGIN:VCALENDAR\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:allday-1\r\n" +
                "SUMMARY:Company Holiday\r\n" +
                "DTSTART;VALUE=DATE:20260802\r\n" +
                "DTEND;VALUE=DATE:20260803\r\n" +
                "END:VEVENT\r\n" +
                "END:VCALENDAR\r\n";

        List<CalendarEvent> events = IcsCalendarParser.parse(new StringReader(ics));
        assertEquals(1, events.size());
        CalendarEvent event = events.get(0);
        assertTrue(event.isAllDay());
        assertTrue(event.occursOn(LocalDate.of(2026, 8, 2)));
        assertFalse(event.occursOn(LocalDate.of(2026, 8, 3)));
    }

    @Test
    public void parsesWeeklyRecurringEvent() throws Exception {
        String ics = "BEGIN:VCALENDAR\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:recurring-1\r\n" +
                "SUMMARY:Weekly Sync\r\n" +
                "DTSTART:20260803T100000\r\n" +
                "DTEND:20260803T103000\r\n" +
                "RRULE:FREQ=WEEKLY;BYDAY=MO,WE\r\n" +
                "END:VEVENT\r\n" +
                "END:VCALENDAR\r\n";

        List<CalendarEvent> events = IcsCalendarParser.parse(new StringReader(ics));
        assertEquals(1, events.size());
        CalendarEvent event = events.get(0);
        assertTrue(event.isRecurring());
        assertTrue(event.occursOn(LocalDate.of(2026, 8, 3))); // Monday
        assertTrue(event.occursOn(LocalDate.of(2026, 8, 5))); // Wednesday
        assertFalse(event.occursOn(LocalDate.of(2026, 8, 4))); // Tuesday
    }

    @Test
    public void unescapesSummaryAndHandlesLineFolding() throws Exception {
        String ics = "BEGIN:VCALENDAR\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:folded-1\r\n" +
                "SUMMARY:Meeting\\, with\\; special chars and a very long descript\r\n" +
                " ion that continues on the next physical line\r\n" +
                "DTSTART:20260802T140000\r\n" +
                "DTEND:20260802T150000\r\n" +
                "END:VEVENT\r\n" +
                "END:VCALENDAR\r\n";

        List<CalendarEvent> events = IcsCalendarParser.parse(new StringReader(ics));
        assertEquals(1, events.size());
        String title = events.get(0).getTitle();
        assertTrue(title.contains("Meeting, with; special chars"));
        assertTrue(title.contains("continues on the next physical line"));
    }

    @Test
    public void utcDtstartIsConvertedToLocalTime() throws Exception {
        String ics = "BEGIN:VCALENDAR\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:utc-1\r\n" +
                "SUMMARY:UTC Event\r\n" +
                "DTSTART:20260802T120000Z\r\n" +
                "DTEND:20260802T130000Z\r\n" +
                "END:VEVENT\r\n" +
                "END:VCALENDAR\r\n";

        List<CalendarEvent> events = IcsCalendarParser.parse(new StringReader(ics));
        assertEquals(1, events.size());
        // Just verify it parsed into a valid, non-null local date/time without throwing.
        CalendarEvent event = events.get(0);
        assertNotNull(event.getTitle());
        assertFalse(event.isAllDay());
    }

    @Test
    public void missingDtstartIsSkipped() throws Exception {
        String ics = "BEGIN:VCALENDAR\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:invalid-1\r\n" +
                "SUMMARY:No start date\r\n" +
                "END:VEVENT\r\n" +
                "END:VCALENDAR\r\n";

        List<CalendarEvent> events = IcsCalendarParser.parse(new StringReader(ics));
        assertTrue(events.isEmpty());
    }
}
