import org.junit.Test;
import org.junit.Before;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.Assert.*;

public class ReminderRecurringTest {
    private File remindersFile;
    private File tasksFile;

    @Before
    public void setUp() throws Exception {
        remindersFile = File.createTempFile("reminders-test", ".properties");
        tasksFile = File.createTempFile("tasks-test", ".xml");
        // ensure empty files
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(remindersFile), StandardCharsets.UTF_8)) {
            w.write("");
        }
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(tasksFile), StandardCharsets.UTF_8)) {
            w.write("<tasks></tasks>");
        }
    }

    @Test
    public void testAddAndLoadRecurringReminder() throws Exception {
        ReminderManager mgr = new ReminderManager(remindersFile.getAbsolutePath(), tasksFile.getAbsolutePath());
        String list = "MyList";
        // Monday and Friday -> bits 0 and 4 -> mask = (1<<0) | (1<<4) = 1 + 16 = 17
        int mask = (1 << 0) | (1 << 4);
        Reminder r = new Reminder(list, mask, 9, 30, null);
        mgr.addReminder(r);

        // New manager instance should load from properties
        ReminderManager loader = new ReminderManager(remindersFile.getAbsolutePath(), tasksFile.getAbsolutePath());
        List<Reminder> loaded = loader.getReminders();
        assertFalse("Should have loaded reminders", loaded.isEmpty());
        boolean found = false;
        for (Reminder lr : loaded) {
            if (lr.isRecurring() && lr.getChecklistName().equals(list) && lr.getHour() == 9 && lr.getMinute() == 30 && lr.getDaysBitmask() == mask) {
                found = true; break;
            }
        }
        assertTrue("Recurring reminder should be found after save/load", found);
    }

    @Test
    public void testLoadOldCommaFormatIsHandled() throws Exception {
        // write legacy comma separated value
        String key = "reminder.0";
        String val = "LegacyList,2025,12,25,9,30";
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(remindersFile), StandardCharsets.UTF_8)) {
            java.util.Properties p = new java.util.Properties();
            p.setProperty(key, val);
            p.store(w, "test");
        }
        ReminderManager loader = new ReminderManager(remindersFile.getAbsolutePath(), tasksFile.getAbsolutePath());
        java.util.List<Reminder> loaded = loader.getReminders();
        assertFalse("Should load legacy reminder", loaded.isEmpty());
        Reminder lr = loaded.get(0);
        assertEquals("LegacyList", lr.getChecklistName());
        assertEquals(2025, lr.getYear());
        assertEquals(12, lr.getMonth());
        assertEquals(25, lr.getDay());
        assertEquals(9, lr.getHour());
        assertEquals(30, lr.getMinute());
    }
}
