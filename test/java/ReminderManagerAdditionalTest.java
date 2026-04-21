import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ReminderManagerAdditionalTest {

    @Test
    public void loadFromProperties_and_add_remove_getDueReminders() throws Exception {
        Path tmp = Files.createTempDirectory("rm-test");
        String remindersFile = tmp.resolve("reminders.properties").toString();
        String tasksFile = tmp.resolve("tasks.xml").toString();

        // Create an initial reminders properties file with a single reminder for now+1min
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();
        int minute = (now.getMinute() + 1) % 60;

        String value = "MYCHECK|" + year + "|" + month + "|" + day + "|" + hour + "|" + minute;
        java.util.Properties props = new java.util.Properties();
        props.setProperty("reminder.0", value);
        try (java.io.OutputStream os = Files.newOutputStream(Path.of(remindersFile))) {
            props.store(os, "test");
        }

        ReminderManager mgr = new ReminderManager(remindersFile, tasksFile);
        List<Reminder> all = mgr.getReminders();
        assertFalse(all.isEmpty());

        // Should be due within 5 minutes
        List<Reminder> due = mgr.getDueReminders(5, Collections.emptySet());
        assertFalse(due.isEmpty());

        // Add a new reminder and ensure it's present
        Reminder r = new Reminder("NEWCHK", year, month, day, hour, minute, null);
        mgr.addReminder(r);
        List<Reminder> after = mgr.getReminders();
        boolean found = after.stream().anyMatch(x -> "NEWCHK".equals(x.getChecklistName()));
        assertTrue(found);

        // Remove it and ensure gone
        mgr.removeReminder(r);
        List<Reminder> afterRemove = mgr.getReminders();
        boolean still = afterRemove.stream().anyMatch(x -> "NEWCHK".equals(x.getChecklistName()));
        assertFalse(still);
    }
}
