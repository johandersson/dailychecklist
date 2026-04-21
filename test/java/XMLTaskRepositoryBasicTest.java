import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class XMLTaskRepositoryBasicTest {

    private Path tmp;

    @Before
    public void setup() throws Exception {
        tmp = Files.createTempDirectory("xmlrepo");
        System.setProperty("dailychecklist.data.dir", tmp.toString());
    }

    @Test
    public void getCachedTasks_and_map_behavior() throws Exception {
        // Ensure data dir exists and create an empty tasks.xml via TaskStaxHandler
        XMLTaskRepository repo = new XMLTaskRepository();
        repo.initialize();

        // Initially empty
        assertNotNull(repo.getAllTasks());
        assertTrue(repo.getAllTasks().isEmpty());

        // Create a sample Task and add to repo (in-memory)
        Task t = new Task("Sample", TaskType.CUSTOM, null, "chk-1");
        repo.addTask(t);

        // In-memory lookup should return it by generated id
        Task got = repo.getTaskById(t.getId());
        assertNotNull(got);
        assertEquals("Sample", got.getName());

        // getTasks by type should include it (filter by checklist id)
        List<Task> customs = repo.getTasks(TaskType.CUSTOM, new Checklist("x", "chk-1"));
        assertFalse(customs.isEmpty());

        // hasUndoneTasks should be true since default done=false
        assertTrue(repo.hasUndoneTasks());

        // Update task and verify update path (in-memory)
        t.setName("Updated");
        repo.updateTaskQuiet(t);
        Task up = repo.getTaskById(t.getId());
        assertEquals("Updated", up.getName());

        // Remove and verify it's gone in-memory
        repo.removeTask(t);
        assertNull(repo.getTaskById("t-1"));
    }
}
