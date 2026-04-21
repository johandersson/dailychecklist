import org.junit.Test;
import org.junit.Before;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.Assert.*;

public class RemindersCompatibilityTest {
    private File tasksFile;

    @Before
    public void setUp() throws Exception {
        tasksFile = File.createTempFile("tasks-test", ".xml");
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(tasksFile), StandardCharsets.UTF_8)) {
            w.write("<tasks></tasks>");
        }
    }

    @Test
    public void testNewFormatLoadedByFeatureBranch() throws Exception {
        // Ensure application data dir exists
        ApplicationConfiguration.ensureDataDirectoryExists();
        String path = ApplicationConfiguration.REMINDERS_FILE_PATH;
        java.util.Properties p = new java.util.Properties();
        p.setProperty("reminders.formatVersion", "2");
        // recurring entry with concrete next-occurrence date and days token
        String val = "CompatList|2026|04|21|09|30|taskId=|days=1,5";
        p.setProperty("reminder.0", val);
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8)) {
            p.store(w, "compat test");
        }

        ReminderManager mgr = new ReminderManager(path, tasksFile.getAbsolutePath());
        List<Reminder> loaded = mgr.getReminders();
        assertFalse("Should load reminders from sample file", loaded.isEmpty());
        // cleanup
        new File(path).delete();
    }
}
