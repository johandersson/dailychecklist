import org.junit.Test;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ReminderManagerUnitTest {

    @Test
    public void testDueRecurringReminderFound() throws Exception {
        // prepare temp files
        File tmpDir = new File("build/tmp/reminder-test");
        tmpDir.mkdirs();

        String remindersPath = new File(tmpDir, "reminders.properties").getAbsolutePath();

        // ensure checklist names file exists under ApplicationConfiguration.APPLICATION_DATA_DIR
        File cfgDir = new File(ApplicationConfiguration.APPLICATION_DATA_DIR);
        cfgDir.mkdirs();
        File checklistNames = new File(cfgDir, ApplicationConfiguration.CHECKLIST_NAMES_FILE_NAME);
        try (FileWriter w = new FileWriter(checklistNames, false)) {
            w.write("chk.test=MyChecklist\n");
        }

        // Build a recurring reminder for today + 1 minute
        LocalDateTime now = LocalDateTime.now();
        int dow = now.getDayOfWeek().getValue(); // 1=Mon..7=Sun
        int daysBitmask = 1 << (dow - 1);
        int hour = now.getHour();
        int minute = (now.getMinute() + 1) % 60;

        Reminder r = new Reminder("MyChecklist", daysBitmask, hour, minute, null);

        // Save the reminder to properties using ReminderStore
        java.util.ArrayList<Reminder> list = new java.util.ArrayList<>();
        list.add(r);
        ReminderStore.saveToProperties(list, remindersPath);

        // Construct manager pointing at our temp reminders file
        ReminderManager mgr = new ReminderManager(remindersPath, "build/tmp/empty-tasks.xml");

        // hasReminders should be true
        assertTrue(mgr.hasReminders("MyChecklist"));

        // getDueReminders should include our reminder within next 5 minutes
        List<Reminder> due = mgr.getDueReminders(5, Collections.emptySet());
        assertNotNull(due);
        assertFalse(due.isEmpty());
        boolean found = false;
        for (Reminder rr : due) {
            if ("MyChecklist".equals(rr.getChecklistName())) found = true;
        }
        assertTrue("Expected to find our recurring reminder in due list", found);
    }
}
