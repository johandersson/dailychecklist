import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class CalendarEventLayoutTest {

    private static CalendarEvent timedEvent(String uid, int startHour, int startMinute, int endHour, int endMinute) {
        return new CalendarEvent(uid, uid, null, null, 2026, 8, 2, startHour, startMinute, 2026, 8, 2, endHour, endMinute, false);
    }

    @Test
    public void nonOverlappingEvents_eachGetsItsOwnSingleColumn() {
        List<CalendarEvent> events = Arrays.asList(
                timedEvent("a", 9, 0, 10, 0),
                timedEvent("b", 11, 0, 12, 0)
        );
        List<CalendarEventLayout.PositionedEvent> positioned = CalendarEventLayout.layout(events, LocalDate.of(2026, 8, 2));
        assertEquals(2, positioned.size());
        for (CalendarEventLayout.PositionedEvent pe : positioned) {
            assertEquals(0, pe.column);
            assertEquals(1, pe.columnCount);
        }
    }

    @Test
    public void overlappingEvents_getDistinctColumnsInSameCluster() {
        List<CalendarEvent> events = Arrays.asList(
                timedEvent("a", 9, 0, 10, 0),
                timedEvent("b", 9, 30, 10, 30)
        );
        List<CalendarEventLayout.PositionedEvent> positioned = CalendarEventLayout.layout(events, LocalDate.of(2026, 8, 2));
        assertEquals(2, positioned.size());
        assertEquals(2, positioned.get(0).columnCount);
        assertEquals(2, positioned.get(1).columnCount);
        assertNotEquals(positioned.get(0).column, positioned.get(1).column);
    }

    @Test
    public void threeWayOverlap_needsThreeColumns() {
        List<CalendarEvent> events = Arrays.asList(
                timedEvent("a", 9, 0, 11, 0),
                timedEvent("b", 9, 30, 10, 30),
                timedEvent("c", 10, 0, 10, 45)
        );
        List<CalendarEventLayout.PositionedEvent> positioned = CalendarEventLayout.layout(events, LocalDate.of(2026, 8, 2));
        int maxColumnCount = positioned.stream().mapToInt(pe -> pe.columnCount).max().orElse(0);
        assertEquals(3, maxColumnCount);
    }

    @Test
    public void sequentialNonOverlappingEvents_reuseSameColumn() {
        List<CalendarEvent> events = Arrays.asList(
                timedEvent("a", 9, 0, 10, 0),
                timedEvent("b", 10, 0, 11, 0),
                timedEvent("c", 11, 0, 12, 0)
        );
        List<CalendarEventLayout.PositionedEvent> positioned = CalendarEventLayout.layout(events, LocalDate.of(2026, 8, 2));
        for (CalendarEventLayout.PositionedEvent pe : positioned) {
            assertEquals(0, pe.column);
            assertEquals(1, pe.columnCount);
        }
    }

    @Test
    public void eventsNotOccurringToday_areExcluded() {
        CalendarEvent tomorrow = new CalendarEvent("x", "Tomorrow", null, null, 2026, 8, 3, 9, 0, 2026, 8, 3, 10, 0, false);
        List<CalendarEventLayout.PositionedEvent> positioned = CalendarEventLayout.layout(Arrays.asList(tomorrow), LocalDate.of(2026, 8, 2));
        assertTrue(positioned.isEmpty());
    }
}
