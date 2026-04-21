import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

public class XMLTaskRepositoryInMemoryTest {

    @Test
    public void testAddAndUpdateTaskInMemory() throws Exception {
        // Prepare temp dir and files
        File tmp = new File("build/tmp/xmlrepo");
        tmp.mkdirs();
        File tasksFile = new File(tmp, "tasks.xml");
        // create minimal xml root so parser can read if needed
        Files.write(tasksFile.toPath(), "<tasks></tasks>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        File remindersFile = new File(tmp, "reminders.properties");
        remindersFile.createNewFile();
        File checklistNames = new File(tmp, "checklist-names.properties");
        Files.write(checklistNames.toPath(), "chk.test=MyChecklist\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Point XMLTaskRepository static file constants to our temp files via reflection
        java.lang.reflect.Field f1 = XMLTaskRepository.class.getDeclaredField("FILE_NAME");
        f1.setAccessible(true);
        f1.set(null, tasksFile.getAbsolutePath());
        java.lang.reflect.Field f2 = XMLTaskRepository.class.getDeclaredField("REMINDER_FILE_NAME");
        f2.setAccessible(true);
        f2.set(null, remindersFile.getAbsolutePath());
        java.lang.reflect.Field f3 = XMLTaskRepository.class.getDeclaredField("CHECKLIST_NAMES_FILE_NAME");
        f3.setAccessible(true);
        f3.set(null, checklistNames.getAbsolutePath());

        // Create repository and initialize
        XMLTaskRepository repo = new XMLTaskRepository(null);
        repo.initialize();

        // Create and add a task (in-memory)
        Task t = new Task("Sample Task", TaskType.CUSTOM, null, "chk-1");
        repo.addTask(t);

        // In-memory lookup should return it
        Task got = repo.getTaskById(t.getId());
        assertNotNull(got);
        assertEquals("Sample Task", got.getName());

        // Update via updateTaskQuiet and verify in-memory update
        t.setName("Updated Task");
        boolean ok = repo.updateTaskQuiet(t);
        assertTrue(ok);
        Task up = repo.getTaskById(t.getId());
        assertNotNull(up);
        assertEquals("Updated Task", up.getName());

        // getTasks by type+checklist should include it when checklist id matches
        List<Task> customs = repo.getTasks(TaskType.CUSTOM, new Checklist("label", "chk-1"));
        assertTrue(customs.stream().anyMatch(x -> x.getId().equals(t.getId())));
    }
}
