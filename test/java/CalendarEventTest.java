import org.junit.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.Assert.*;

public class CalendarEventTest {

    @Test
    public void singleDayEvent_occursOnlyOnItsDate() {
        CalendarEvent event = new CalendarEvent(null, "Standup", null, null,
                2026, 8, 2, 9, 0,
                2026, 8, 2, 9, 30, false);

        assertTrue(event.occursOn(LocalDate.of(2026, 8, 2)));
        assertFalse(event.occursOn(LocalDate.of(2026, 8, 3)));
        assertEquals(LocalTime.of(9, 0), event.effectiveStartTime(LocalDate.of(2026, 8, 2)));
        assertEquals(LocalTime.of(9, 30), event.effectiveEndTime(LocalDate.of(2026, 8, 2)));
    }

    @Test
    public void multiDayEvent_clipsTimesToDayBoundaries() {
        CalendarEvent event = new CalendarEvent(null, "Conference", null, null,
                2026, 8, 1, 14, 0,
                2026, 8, 3, 11, 0, false);

        assertTrue(event.occursOn(LocalDate.of(2026, 8, 2)));
        // Middle day: starts at midnight, ends just before midnight
        assertEquals(LocalTime.MIDNIGHT, event.effectiveStartTime(LocalDate.of(2026, 8, 2)));
        assertEquals(LocalTime.of(23, 59), event.effectiveEndTime(LocalDate.of(2026, 8, 2)));
        // First day: real start time, clipped end
        assertEquals(LocalTime.of(14, 0), event.effectiveStartTime(LocalDate.of(2026, 8, 1)));
        // Last day: clipped start, real end time
        assertEquals(LocalTime.of(11, 0), event.effectiveEndTime(LocalDate.of(2026, 8, 3)));
    }

    @Test
    public void allDayEvent_spansEntireDay() {
        CalendarEvent event = new CalendarEvent(null, "Holiday", null, null,
                2026, 8, 2, 0, 0,
                2026, 8, 2, 0, 0, true);

        assertTrue(event.isAllDay());
        assertEquals(LocalTime.MIDNIGHT, event.effectiveStartTime(LocalDate.of(2026, 8, 2)));
        assertEquals(LocalTime.of(23, 59), event.effectiveEndTime(LocalDate.of(2026, 8, 2)));
    }

    @Test
    public void recurringWeeklyEvent_occursOnMatchingWeekdaysOnly() {
        // Monday (bit 0) and Wednesday (bit 2)
        int mask = (1 << 0) | (1 << 2);
        CalendarEvent event = new CalendarEvent(null, "Weekly Sync", null, null, mask, 10, 0, 10, 30);

        assertTrue(event.isRecurring());
        assertTrue(event.occursOn(LocalDate.of(2026, 8, 3))); // Monday
        assertTrue(event.occursOn(LocalDate.of(2026, 8, 5))); // Wednesday
        assertFalse(event.occursOn(LocalDate.of(2026, 8, 4))); // Tuesday
    }

    @Test
    public void equality_prefersUidWhenPresent() {
        CalendarEvent a = new CalendarEvent("uid-1", "Title A", null, null, 2026, 1, 1, 9, 0, 2026, 1, 1, 10, 0, false);
        CalendarEvent b = new CalendarEvent("uid-1", "Title B", null, null, 2027, 2, 2, 11, 0, 2027, 2, 2, 12, 0, false);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
