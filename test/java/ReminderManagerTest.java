import org.junit.Test;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.*;

public class ReminderManagerTest {

    // Helper to inject cached reminders into ReminderManager via reflection
    private void setCachedReminders(ReminderManager mgr, List<Reminder> reminders) throws Exception {
        Field cachedField = ReminderManager.class.getDeclaredField("cachedReminders");
        cachedField.setAccessible(true);
        cachedField.set(mgr, new ArrayList<>(reminders));

        Field dirtyField = ReminderManager.class.getDeclaredField("remindersDirty");
        dirtyField.setAccessible(true);
        dirtyField.setBoolean(mgr, false);
    }

    @Test
    public void getDueReminders_includesUpcomingRecurring() throws Exception {
        ReminderManager mgr = new ReminderManager("does-not-exist","does-not-exist");
        // recurring reminder for today, one minute in the future
        LocalDateTime now = LocalDateTime.now();
        int dow = now.getDayOfWeek().getValue(); // 1..7
        int daysBitmask = 1 << (dow - 1);
        int hour = now.getHour();
        int minute = now.getMinute() + 1;
        if (minute >= 60) { minute = 0; hour = (hour + 1) % 24; }

        Reminder r = new Reminder("MORNING", daysBitmask, hour, minute, "task-1");
        setCachedReminders(mgr, Collections.singletonList(r));

        List<Reminder> due = mgr.getDueReminders(2, Collections.emptySet());
        assertFalse("Expected at least one due reminder", due.isEmpty());
        assertTrue(due.contains(r));
    }

    @Test
    public void getNextReminderTime_skipsOpenedChecklists() throws Exception {
        ReminderManager mgr = new ReminderManager("a","b");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime t1 = now.plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime t2 = now.plusDays(2).withHour(8).withMinute(30).withSecond(0).withNano(0);

        Reminder r1 = new Reminder("ListA", t1.getYear(), t1.getMonthValue(), t1.getDayOfMonth(), t1.getHour(), t1.getMinute(), "id1");
        Reminder r2 = new Reminder("ListB", t2.getYear(), t2.getMonthValue(), t2.getDayOfMonth(), t2.getHour(), t2.getMinute(), "id2");

        setCachedReminders(mgr, Arrays.asList(r1, r2));

        // open ListA so next should be ListB's reminder
        LocalDateTime next = mgr.getNextReminderTime(new HashSet<>(Collections.singletonList("ListA")));
        assertNotNull(next);
        assertEquals(t2.withSecond(0).withNano(0), next.withSecond(0).withNano(0));
    }

    @Test
    public void hasReminders_countsTaskLevelOnlyForBuiltIn() throws Exception {
        ReminderManager mgr = new ReminderManager("x","y");

        Reminder checklistOnly = new Reminder("MORNING", 2025, 1, 1, 9, 0);
        Reminder taskLevel = new Reminder("MORNING", 2025, 1, 2, 9, 0, "task-1");

        // Only checklist-level -> should be false
        setCachedReminders(mgr, Collections.singletonList(checklistOnly));
        assertFalse(mgr.hasReminders("MORNING"));

        // Add task-level -> should be true
        setCachedReminders(mgr, Arrays.asList(checklistOnly, taskLevel));
        assertTrue(mgr.hasReminders("MORNING"));
    }

    @Test
    public void testAddGetNextAndRemoveReminder() throws Exception {
        Path tmp = Files.createTempDirectory("rem-test");
        String remindersFile = tmp.resolve("reminders.properties").toString();
        String tasksFile = tmp.resolve("tasks.xml").toString();

        ReminderManager mgr = new ReminderManager(remindersFile, tasksFile);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime later = now.plusMinutes(1);

        Reminder r = new Reminder("MORNING",
                later.getYear(), later.getMonthValue(), later.getDayOfMonth(), later.getHour(), later.getMinute(), "task-1");

        mgr.addReminder(r);

        List<Reminder> due = mgr.getDueReminders(2, Collections.emptySet());
        assertTrue("Expected reminder to be due", due.contains(r));

        LocalDateTime next = mgr.getNextReminderTime(Collections.emptySet());
        assertNotNull("Next reminder time should not be null", next);
        assertEquals(later.withSecond(0).withNano(0), next.withSecond(0).withNano(0));

        mgr.removeReminder(r);
        List<Reminder> afterRemove = mgr.getDueReminders(2, Collections.emptySet());
        assertFalse("Reminder should have been removed", afterRemove.contains(r));
    }
}
