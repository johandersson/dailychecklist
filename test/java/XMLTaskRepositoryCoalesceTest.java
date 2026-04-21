import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class XMLTaskRepositoryCoalesceTest {

    private static class RecordingHandler extends TaskStaxHandler {
        volatile List<Task> lastPersisted = null;
        RecordingHandler(String f) { super(f); }
        @Override public void setAllTasks(List<Task> tasks) {
            this.lastPersisted = tasks;
        }
    }

    @Test
    public void coalescedFlushPersistsSnapshot() throws Exception {
        XMLTaskRepository repo = new XMLTaskRepository(null);

        // Install recording handler
        RecordingHandler rh = new RecordingHandler("unused");
        Field fh = XMLTaskRepository.class.getDeclaredField("taskXmlHandler");
        fh.setAccessible(true);
        fh.set(repo, rh);

        // Add task, update it, then remove — all should be coalesced
        Task t1 = new Task("T one", TaskType.CUSTOM, null, "chk-1");
        repo.addTask(t1);

        // update
        t1.setName("T one updated");
        repo.updateTaskQuiet(t1);

        // remove
        repo.removeTask(t1);

        // Invoke private flushPendingWrites to force immediate coalesced flush
        Method m = XMLTaskRepository.class.getDeclaredMethod("flushPendingWrites");
        m.setAccessible(true);
        m.invoke(repo);

        // Wait for the write executor to finish submitted tasks
        Field wf = XMLTaskRepository.class.getDeclaredField("writeExecutor");
        wf.setAccessible(true);
        java.util.concurrent.ExecutorService es = (java.util.concurrent.ExecutorService) wf.get(repo);
        es.shutdown();
        es.awaitTermination(2, TimeUnit.SECONDS);

        // lastPersisted should be non-null and should NOT contain the removed task
        assertNotNull("persist should have been invoked", rh.lastPersisted);
        boolean contains = rh.lastPersisted.stream().anyMatch(x -> x.getId().equals(t1.getId()));
        assertFalse("Removed task should not be persisted", contains);
    }
}
