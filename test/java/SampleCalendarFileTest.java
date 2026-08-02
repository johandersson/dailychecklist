import org.junit.Test;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Verifies the bundled sample .ics file (samples/sample-calendar.ics) parses
 * correctly and produces the events documented in its comments.
 */
public class SampleCalendarFileTest {

    @Test
    public void sampleFileParsesIntoExpectedEvents() throws Exception {
        File sampleFile = new File("samples/sample-calendar.ics");
        assertTrue("Sample .ics file should exist at " + sampleFile.getAbsolutePath(), sampleFile.exists());

        List<CalendarEvent> events = IcsCalendarParser.parse(sampleFile);
        assertEquals(6, events.size());

        LocalDate today = LocalDate.of(2026, 8, 2);
        long occurringToday = events.stream().filter(e -> e.occursOn(today)).count();
        assertEquals("5 of the 6 sample events should occur on 2026-08-02", 5, occurringToday);

        boolean hasAllDay = events.stream().anyMatch(CalendarEvent::isAllDay);
        assertTrue("Sample should include an all-day event", hasAllDay);

        boolean hasRecurring = events.stream().anyMatch(CalendarEvent::isRecurring);
        assertTrue("Sample should include a recurring event", hasRecurring);

        boolean notTodayExcluded = events.stream()
                .filter(e -> "Future Planning Session".equals(e.getTitle()))
                .noneMatch(e -> e.occursOn(today));
        assertTrue("The 'Future Planning Session' event should not occur today", notTodayExcluded);
    }
}
