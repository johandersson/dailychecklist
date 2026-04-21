import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Calendar;

public class TaskStaxHandlerTest {

    private String makeTempPath(String name) {
        String tmp = System.getProperty("java.io.tmpdir");
        return new File(tmp, "stax_" + name + ".xml").getAbsolutePath();
    }

    @Test
    public void addParseRemoveAndEscaping() throws Exception {
        String path = makeTempPath("basic");
        new File(path).delete();
        TaskStaxHandler h = new TaskStaxHandler(path);
        h.ensureFileExists();
        assertTrue(new File(path).exists());

        Task t = new Task("sid1","Name & <x>", TaskType.CUSTOM, "Mon", true, null, "cid", null);
        // add
        h.addTask(t);
        List<Task> all = h.parseAllTasks();
        assertEquals(1, all.size());
        assertEquals("sid1", all.get(0).getId());
        assertEquals("Name & <x>", all.get(0).getName());

        // checkAndResetPastDoneDate: set done date to yesterday, then reset
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -2);
        t.setDoneDate(c.getTime());
        h.updateTasks(java.util.Arrays.asList(t));
        List<Task> after = h.parseAllTasks();
        assertEquals(1, after.size());

        // remove
        h.removeTask(t);
        List<Task> afterRem = h.parseAllTasks();
        assertTrue(afterRem.isEmpty());
    }

    @Test(expected=IllegalArgumentException.class)
    public void addInvalidTaskThrows() throws Exception {
        String path = makeTempPath("invalid");
        new File(path).delete();
        TaskStaxHandler h = new TaskStaxHandler(path);
        h.ensureFileExists();
        Task bad = null;
        h.addTask(bad);
    }
}
