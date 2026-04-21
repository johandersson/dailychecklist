import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class TaskXmlHandlerTest {

    private String makeTempPath(String name) {
        String tmp = System.getProperty("java.io.tmpdir");
        return new File(tmp, "tc_" + name + ".xml").getAbsolutePath();
    }

    @Test
    public void ensureAndAddUpdateRemove() throws Exception {
        String path = makeTempPath("xmlhandler");
        // cleanup
        new File(path).delete();

        TaskXmlHandler h = new TaskXmlHandler(path);
        h.ensureFileExists();
        assertTrue(new File(path).exists());

        Task t = new Task("tid1","T1", TaskType.CUSTOM, null, false, null, null, null);
        h.addTask(t);

        List<Task> tasks = h.parseAllTasks();
        assertEquals(1, tasks.size());
        assertEquals("tid1", tasks.get(0).getId());

        // update note
        t.setNote("hello & <xml>");
        h.updateTask(t);
        tasks = h.parseAllTasks();
        assertEquals(1, tasks.size());
        assertEquals("hello & <xml>", tasks.get(0).getNote());

        // remove
        h.removeTask(t);
        tasks = h.parseAllTasks();
        assertEquals(0, tasks.size());
    }

    @Test(expected=IllegalArgumentException.class)
    public void parseMissingIdThrows() throws Exception {
        String path = makeTempPath("bad1");
        // write a minimal bad file
        String xml = "<?xml version='1.0' encoding='UTF-8'?><tasks><task><name>n</name><type>CUSTOM</type></task></tasks>";
        java.nio.file.Files.write(java.nio.file.Paths.get(path), xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        TaskXmlHandler h = new TaskXmlHandler(path);
        // parseAllTasks will attempt to parse and should skip/throw when encountering invalid element; parseTaskFromElement throws
        h.parseAllTasks();
    }
}
