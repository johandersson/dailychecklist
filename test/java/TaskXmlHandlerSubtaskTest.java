import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class TaskXmlHandlerSubtaskTest {

    @Test
    public void parsesSubtaskParentId() throws Exception {
        String tmp = System.getProperty("java.io.tmpdir");
        File f = new File(tmp, "tc_subtasks.xml");
        String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<tasks>\n" +
            "  <task id=\"p1\">\n" +
            "    <name>Parent</name>\n" +
            "    <type>CUSTOM</type>\n" +
            "  </task>\n" +
            "  <task id=\"c1\">\n" +
            "    <name>Child</name>\n" +
            "    <type>CUSTOM</type>\n" +
            "    <parentId>p1</parentId>\n" +
            "  </task>\n" +
            "</tasks>\n";
        Files.write(f.toPath(), xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        TaskXmlHandler h = new TaskXmlHandler(f.getAbsolutePath());
        List<Task> tasks = h.parseAllTasks();
        assertEquals(2, tasks.size());
        Task parent = tasks.stream().filter(t->"p1".equals(t.getId())).findFirst().orElse(null);
        Task child = tasks.stream().filter(t->"c1".equals(t.getId())).findFirst().orElse(null);
        assertNotNull(parent);
        assertNotNull(child);
        assertEquals("p1", child.getParentId());
    }
}
