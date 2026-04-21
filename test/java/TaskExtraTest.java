import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class TaskExtraTest {

    @Test
    public void subtasksBehaviorAndEquals() {
        Task parent = new Task("pid","Parent", TaskType.CUSTOM, null, false, null, null, null);
        Task s1 = new Task("s1","sub1", TaskType.CUSTOM, null, false, null, null, "pid");
        Task s2 = new Task("s2","sub2", TaskType.CUSTOM, null, false, null, null, "pid");
        ArrayList<Task> subs = new ArrayList<>();
        subs.add(s1); subs.add(s2);
        parent.setSubtasks(subs);

        assertTrue(parent.hasSubtasks());
        assertFalse(parent.areAllSubtasksDone());

        parent.markAllSubtasksDone(true);
        assertTrue(parent.areAllSubtasksDone());

        // equals/hashCode by id
        Task same = new Task("pid","Other", TaskType.CUSTOM, null, false, null, null, null);
        assertEquals(parent, same);
        assertEquals(parent.hashCode(), same.hashCode());
    }

    @Test
    public void doneDateFormattingAndParsing() {
        Task t = new Task("name", TaskType.CUSTOM, null);
        Calendar cal = Calendar.getInstance();
        cal.set(2020, Calendar.JANUARY, 2, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date d = cal.getTime();
        t.setDoneDate(d);
        String s = t.getDoneDate();
        assertNotNull(s);
        assertTrue(s.startsWith("2020-01-02"));

        // parsed date should not be null and should match year
        Date parsed = t.getParsedDoneDate();
        assertNotNull(parsed);
        Calendar pc = Calendar.getInstance(); pc.setTime(parsed);
        assertEquals(2020, pc.get(Calendar.YEAR));
    }

    @Test
    public void noteAndDisplayDirty() {
        Task t = new Task("name2", TaskType.CUSTOM, null);
        assertTrue(t.isDisplayDirty());
        t.markDisplayClean();
        assertFalse(t.isDisplayDirty());
        t.setNote("hello");
        assertTrue(t.hasNote());
        assertTrue(t.isDisplayDirty());
        t.setName("newname");
        assertTrue(t.isDisplayDirty());
    }
}
