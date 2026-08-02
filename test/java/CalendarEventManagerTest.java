import org.junit.Test;
import org.junit.Before;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class CalendarEventManagerTest {
    private File eventsFile;

    @Before
    public void setUp() throws Exception {
        eventsFile = File.createTempFile("calendar-events-test", ".properties");
        eventsFile.delete(); // start from a clean, non-existent file like a fresh install
    }

    @Test
    public void addEvents_persistsAndReloadsAcrossInstances() throws Exception {
        CalendarEventManager mgr = new CalendarEventManager(eventsFile.getAbsolutePath());
        CalendarEvent event = new CalendarEvent("uid-1", "Dentist", "Clinic", "Bring insurance card",
                2026, 8, 2, 9, 0, 2026, 8, 2, 9, 30, false);

        mgr.addEvent(event);

        CalendarEventManager reloaded = new CalendarEventManager(eventsFile.getAbsolutePath());
        List<CalendarEvent> loaded = reloaded.getEvents();
        assertEquals(1, loaded.size());
        CalendarEvent roundTripped = loaded.get(0);
        assertEquals("uid-1", roundTripped.getUid());
        assertEquals("Dentist", roundTripped.getTitle());
        assertEquals("Clinic", roundTripped.getLocation());
        assertEquals("Bring insurance card", roundTripped.getDescription());
    }

    @Test
    public void addEvents_bulkImportPersistsAllInOneCall() throws Exception {
        CalendarEventManager mgr = new CalendarEventManager(eventsFile.getAbsolutePath());
        CalendarEvent a = new CalendarEvent("a", "Event A", null, null, 2026, 8, 2, 9, 0, 2026, 8, 2, 10, 0, false);
        CalendarEvent b = new CalendarEvent("b", "Event B", null, null, 2026, 8, 2, 11, 0, 2026, 8, 2, 12, 0, false);

        mgr.addEvents(Arrays.asList(a, b));

        assertEquals(2, mgr.getEvents().size());
    }

    @Test
    public void getEventsOn_filtersToRequestedDate() throws Exception {
        CalendarEventManager mgr = new CalendarEventManager(eventsFile.getAbsolutePath());
        CalendarEvent today = new CalendarEvent("t", "Today Event", null, null, 2026, 8, 2, 9, 0, 2026, 8, 2, 10, 0, false);
        CalendarEvent otherDay = new CalendarEvent("o", "Other Event", null, null, 2026, 8, 5, 9, 0, 2026, 8, 5, 10, 0, false);
        mgr.addEvents(Arrays.asList(today, otherDay));

        List<CalendarEvent> onTargetDate = mgr.getEventsOn(LocalDate.of(2026, 8, 2));
        assertEquals(1, onTargetDate.size());
        assertEquals("Today Event", onTargetDate.get(0).getTitle());
    }

    @Test
    public void removeEvent_deletesIt() throws Exception {
        CalendarEventManager mgr = new CalendarEventManager(eventsFile.getAbsolutePath());
        CalendarEvent event = new CalendarEvent("r", "Removable", null, null, 2026, 8, 2, 9, 0, 2026, 8, 2, 10, 0, false);
        mgr.addEvent(event);
        assertEquals(1, mgr.getEvents().size());

        mgr.removeEvent(event);
        assertTrue(mgr.getEvents().isEmpty());
    }

    @Test
    public void specialCharactersInTitleSurviveRoundTrip() throws Exception {
        CalendarEventManager mgr = new CalendarEventManager(eventsFile.getAbsolutePath());
        CalendarEvent event = new CalendarEvent(null, "Pipe|Delimiter & \"quotes\" test", null, null,
                2026, 8, 2, 9, 0, 2026, 8, 2, 10, 0, false);
        mgr.addEvent(event);

        CalendarEventManager reloaded = new CalendarEventManager(eventsFile.getAbsolutePath());
        assertEquals("Pipe|Delimiter & \"quotes\" test", reloaded.getEvents().get(0).getTitle());
    }
}
