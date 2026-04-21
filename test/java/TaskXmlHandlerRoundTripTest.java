import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TaskXmlHandlerRoundTripTest {

    @Test
    public void roundTripSpecialCharsAndNote() throws Exception {
        String tmp = System.getProperty("java.io.tmpdir");
        File f = new File(tmp, "taskxmlhandler-roundtrip.xml");
        if (f.exists()) f.delete();

        TaskXmlHandler h = new TaskXmlHandler(f.getAbsolutePath());

        Task t = new Task("Name & <test>", TaskType.CUSTOM, null, "chk-rt");
        t.setNote("Line1 &amp; Line2 <tag> \"quotes\"");
        t.setDone(true);
        t.setDoneDate(new Date());

        List<Task> tasks = new ArrayList<>();
        tasks.add(t);

        h.setAllTasks(tasks);

        List<Task> parsed = h.parseAllTasks();
        assertEquals(1, parsed.size());
        Task p = parsed.get(0);
        assertEquals(t.getId(), p.getId());
        assertEquals(t.getName(), p.getName());
        assertEquals(t.getChecklistId(), p.getChecklistId());
        assertEquals(t.getNote(), p.getNote());
        assertEquals(t.isDone(), p.isDone());
        assertNotNull(p.getDoneDate());

        // cleanup
        if (f.exists()) f.delete();
    }

    @Test
    public void checkAndResetPastDoneDate_resetsIfPast() throws Exception {
        TaskXmlHandler h = new TaskXmlHandler(System.getProperty("java.io.tmpdir") + File.separator + "taskxmlhandler-dummy.xml");

        Task t = new Task("old", TaskType.CUSTOM, null, "chk-x");
        // mark done yesterday
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -1);
        t.setDone(true);
        t.setDoneDate(c.getTime());

        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());
        h.checkAndResetPastDoneDate(t, today);
        assertFalse(t.isDone());
        assertNull(t.getDoneDate());
    }
}
