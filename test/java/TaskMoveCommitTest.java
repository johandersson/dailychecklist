/*
 * Daily Checklist
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

public class TaskMoveCommitTest {
    private final List<String> previousPaths = new ArrayList<>();
    private static final String[] PATH_FIELDS = {"FILE_NAME", "REMINDER_FILE_NAME", "CHECKLIST_NAMES_FILE_NAME"};
    private Path directory;
    private Path tasksFile;
    private XMLTaskRepository repository;
    private TaskManager manager;
    private BlockingHandler blocker;

    private static Field field(String name) throws Exception {
        Field field = XMLTaskRepository.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @Before public void setup() throws Exception {
        directory = Path.of("build", "move-commit-tests", UUID.randomUUID().toString());
        Files.createDirectories(directory);
        String[] names = {"tasks.xml", "reminders.properties", "checklists.properties"};
        for (int i = 0; i < PATH_FIELDS.length; i++) {
            previousPaths.add((String) field(PATH_FIELDS[i]).get(null));
            field(PATH_FIELDS[i]).set(null, directory.resolve(names[i]).toString());
        }
        tasksFile = directory.resolve("tasks.xml");
        new TaskStaxHandler(tasksFile.toString()).setAllTasks(List.of(
            TaskMovePlannerTest.task("a", TaskType.MORNING, null),
            TaskMovePlannerTest.task("b", TaskType.MORNING, null),
            TaskMovePlannerTest.task("c", TaskType.EVENING, null),
            TaskMovePlannerTest.task("d", TaskType.EVENING, null)));
        repository = new TestRepository();
        manager = new TaskManager(repository);
        assertEquals(4, manager.getAllTasks().size());
    }

    @After public void cleanup() throws Exception {
        if (blocker != null) blocker.release.countDown();
        if (repository != null) {
            drainWriter();
            repository.shutdown();
        }
        for (int i = 0; i < previousPaths.size(); i++) field(PATH_FIELDS[i]).set(null, previousPaths.get(i));
        if (directory != null) {
            try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private void drainWriter() throws Exception {
        Method flush = XMLTaskRepository.class.getDeclaredMethod("flushPendingWrites");
        flush.setAccessible(true);
        flush.invoke(repository);
        ExecutorService executor = (ExecutorService) field("writeExecutor").get(repository);
        if (!executor.isShutdown()) executor.submit(() -> {}).get(10, TimeUnit.SECONDS);
    }

    private List<Task> disk() throws Exception {
        return new TaskStaxHandler(tasksFile.toString()).parseAllTasks();
    }

    private TaskMovePlanner.Intent move(String id, TaskMovePlanner.Destination destination, String before, String parent) {
        return new TaskMovePlanner.Intent(List.of(id), destination, before, parent);
    }

    private static class BlockingHandler extends TaskStaxHandler {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger writes = new AtomicInteger();
        BlockingHandler(String path) { super(path); }
        void beforeWrite() throws Exception {
            if (writes.incrementAndGet() == 1) {
                entered.countDown();
                if (!release.await(10, TimeUnit.SECONDS)) throw new IOException("Test write release timed out");
            }
        }
        @Override public void setAllTasks(List<Task> tasks) throws Exception {
            beforeWrite();
            super.setAllTasks(tasks);
        }
    }

    private static class TestRepository extends XMLTaskRepository {
        volatile BlockingHandler moveBlocker;
        volatile boolean failMoves;
        volatile int failMoveWrite = -1;
        final AtomicInteger moveWrites = new AtomicInteger();
        TestRepository() { super(null); }
        @Override protected void persistMoveSnapshot(List<Task> tasks, Path stagingFile) throws Exception {
            int write = moveWrites.incrementAndGet();
            if (moveBlocker != null) moveBlocker.beforeWrite();
            if (failMoves || failMoveWrite == write) throw new IOException("injected disk failure");
            super.persistMoveSnapshot(tasks, stagingFile);
        }
    }

    private BlockingHandler blockFirstWrite() throws Exception {
        blocker = new BlockingHandler(tasksFile.toString());
        field("taskXmlHandler").set(repository, blocker);
        ((TestRepository) repository).moveBlocker = blocker;
        return blocker;
    }

    @Test public void futureAndNotificationsWaitForPersistenceWithoutBlockingEdt() throws Exception {
        BlockingHandler handler = blockFirstWrite();
        AtomicInteger notifications = new AtomicInteger();
        manager.addTaskChangeListener(notifications::incrementAndGet);
        CompletableFuture<List<String>> result = manager.moveTasks(move("a", TaskMovePlannerTest.EVENING, "c", null));
        assertTrue(handler.entered.await(5, TimeUnit.SECONDS));
        assertFalse(result.isDone());
        assertEquals(TaskType.MORNING, manager.getTaskById("a").getType());
        assertEquals(0, notifications.get());
        Future<?> edt = CompletableFuture.runAsync(() -> {
            try { SwingUtilities.invokeAndWait(() -> assertEquals(4, manager.getAllTasks().size())); }
            catch (Exception exception) { throw new RuntimeException(exception); }
        });
        edt.get(2, TimeUnit.SECONDS);
        handler.release.countDown();
        assertEquals(List.of("a"), result.get(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(1, notifications.get());
        assertEquals(TaskType.EVENING, manager.getTaskById("a").getType());
        assertEquals(TaskType.EVENING, disk().stream().filter(t -> t.getId().equals("a")).findFirst().get().getType());
    }

    @Test public void queuedIntentsRecomputeAgainstPreviousCommittedMove() throws Exception {
        BlockingHandler handler = blockFirstWrite();
        CompletableFuture<List<String>> first = manager.moveTasks(move("a", TaskMovePlannerTest.EVENING, "c", null));
        assertTrue(handler.entered.await(5, TimeUnit.SECONDS));
        CompletableFuture<List<String>> second = manager.moveTasks(move("b", TaskMovePlannerTest.EVENING, "a", null));
        assertFalse(second.isDone());
        handler.release.countDown();
        first.get(5, TimeUnit.SECONDS);
        second.get(5, TimeUnit.SECONDS);
        assertEquals(List.of("b", "a", "c", "d"), TaskMovePlannerTest.ids(manager.getAllTasks()));
        assertEquals(TaskMovePlanner.snapshot(manager.getAllTasks()), TaskMovePlanner.snapshot(disk()));
    }

    @Test public void failureDoesNotPublishOrNotifyAndNextIntentStillWorks() throws Exception {
        AtomicInteger notifications = new AtomicInteger();
        manager.addTaskChangeListener(notifications::incrementAndGet);
        TaskMovePlanner.Snapshot before = TaskMovePlanner.snapshot(manager.getAllTasks());
        ((TestRepository) repository).failMoves = true;
        CompletableFuture<List<String>> result = manager.moveTasks(move("a", TaskMovePlannerTest.EVENING, "c", null));
        java.util.concurrent.ExecutionException failure = assertThrows(java.util.concurrent.ExecutionException.class,
            () -> result.get(5, TimeUnit.SECONDS));
        assertTrue(failure.getCause() instanceof IOException);
        assertEquals(before, TaskMovePlanner.snapshot(manager.getAllTasks()));
        assertEquals(before, TaskMovePlanner.snapshot(disk()));
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(0, notifications.get());
        ((TestRepository) repository).failMoves = false;
        manager.moveTasks(move("a", TaskMovePlannerTest.EVENING, "c", null)).get(5, TimeUnit.SECONDS);
        assertEquals(TaskType.EVENING, manager.getTaskById("a").getType());
    }

    @Test public void editsAdditionsAndRemovalsDuringDiskIoAreNotDropped() throws Exception {
        BlockingHandler handler = blockFirstWrite();
        CompletableFuture<List<String>> result = manager.moveTasks(move("a", TaskMovePlannerTest.EVENING, "c", null));
        assertTrue(handler.entered.await(5, TimeUnit.SECONDS));
        CompletableFuture.runAsync(() -> {
            Task edited = TaskManager.copyTask(manager.getTaskById("b"));
            edited.setName("edited while saving");
            edited.setNote("new note");
            manager.updateTask(edited);
            manager.addTask(TaskMovePlannerTest.task("new", TaskType.MORNING, null));
            manager.removeTask(manager.getTaskById("d"));
        }).get(2, TimeUnit.SECONDS);
        handler.release.countDown();
        result.get(5, TimeUnit.SECONDS);
        drainWriter();
        assertTrue("a conflicting write must be recomputed", handler.writes.get() >= 2);
        assertEquals("edited while saving", manager.getTaskById("b").getName());
        assertEquals("new note", manager.getTaskById("b").getNote());
        assertNotNull(manager.getTaskById("new"));
        assertNull(manager.getTaskById("d"));
        assertEquals(TaskType.EVENING, manager.getTaskById("a").getType());
        assertEquals(TaskMovePlanner.snapshot(manager.getAllTasks()), TaskMovePlanner.snapshot(disk()));
    }

    @Test public void deletionOfAnchorDuringWriteRejectsAndRestoresCanonicalDiskState() throws Exception {
        BlockingHandler handler = blockFirstWrite();
        CompletableFuture<List<String>> result = manager.moveTasks(move("a", TaskMovePlannerTest.EVENING, "c", null));
        assertTrue(handler.entered.await(5, TimeUnit.SECONDS));
        manager.removeTask(manager.getTaskById("c"));
        handler.release.countDown();
        assertThrows(java.util.concurrent.ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
        drainWriter();
        assertEquals(TaskType.MORNING, manager.getTaskById("a").getType());
        assertNull(manager.getTaskById("c"));
        assertEquals(TaskMovePlanner.snapshot(manager.getAllTasks()), TaskMovePlanner.snapshot(disk()));
    }

    @Test public void failedRecomputedWriteNeverPromotesEarlierAttemptToCanonicalFile() throws Exception {
        BlockingHandler handler = blockFirstWrite();
        ((TestRepository) repository).failMoveWrite = 2;
        TaskMovePlanner.Snapshot originalDisk = TaskMovePlanner.snapshot(disk());
        CompletableFuture<List<String>> result = manager.moveTasks(move("a", TaskMovePlannerTest.EVENING, "c", null));
        assertTrue(handler.entered.await(5, TimeUnit.SECONDS));
        Task edited = TaskManager.copyTask(manager.getTaskById("b"));
        edited.setNote("concurrent note");
        manager.updateTask(edited);
        handler.release.countDown();
        assertThrows(java.util.concurrent.ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
        // Ordinary coalescing may already have saved the note, but never the failed move.
        assertEquals(TaskType.MORNING, disk().stream().filter(t -> t.getId().equals("a")).findFirst().get().getType());
        assertEquals(TaskType.MORNING, manager.getTaskById("a").getType());
        assertEquals(originalDisk.tasks().get(0), TaskMovePlanner.TaskValue.of(manager.getTaskById("a")));
        drainWriter();
        assertEquals("concurrent note", manager.getTaskById("b").getNote());
        assertEquals(TaskMovePlanner.snapshot(manager.getAllTasks()), TaskMovePlanner.snapshot(disk()));
    }

    @Test public void concurrentDestinationChecklistRemovalIsRevalidatedBeforePromotion() throws Exception {
        Checklist custom = new Checklist("Stable custom", "custom-id");
        manager.addChecklist(custom);
        BlockingHandler handler = blockFirstWrite();
        CompletableFuture<List<String>> result = manager.moveTasks(move("a", TaskMovePlannerTest.CUSTOM, null, null));
        assertTrue(handler.entered.await(5, TimeUnit.SECONDS));
        manager.removeChecklist(custom);
        handler.release.countDown();
        assertThrows(java.util.concurrent.ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
        assertEquals(TaskType.MORNING, manager.getTaskById("a").getType());
        assertEquals(TaskMovePlanner.snapshot(manager.getAllTasks()), TaskMovePlanner.snapshot(disk()));
    }

    @Test public void shutdownDoesNotDiscardAnAlreadyAcceptedDurableCommit() throws Exception {
        BlockingHandler handler = blockFirstWrite();
        CompletableFuture<List<String>> result = manager.moveTasks(move("a", TaskMovePlannerTest.EVENING, "c", null));
        assertTrue(handler.entered.await(5, TimeUnit.SECONDS));
        CompletableFuture.runAsync(repository::shutdown).get(2, TimeUnit.SECONDS);
        handler.release.countDown();
        assertEquals(List.of("a"), result.get(5, TimeUnit.SECONDS));
        assertTrue(((ExecutorService) field("writeExecutor").get(repository)).awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(TaskType.EVENING, disk().stream().filter(t -> t.getId().equals("a")).findFirst().get().getType());
    }

    @Test public void queuedNormalWriterCannotOverwriteNewMoveWithCapturedOldState() throws Exception {
        BlockingHandler handler = blockFirstWrite();
        manager.updateTaskImmediate(TaskManager.copyTask(manager.getTaskById("b")));
        assertTrue(handler.entered.await(5, TimeUnit.SECONDS));
        CompletableFuture<List<String>> move = manager.moveTasks(move("a", TaskMovePlannerTest.EVENING, "c", null));
        // The coordinator is FIFO; its repository submission may race this ordinary writer.
        Task edited = TaskManager.copyTask(manager.getTaskById("b"));
        edited.setName("new name");
        manager.updateTaskImmediate(edited);
        handler.release.countDown();
        move.get(5, TimeUnit.SECONDS);
        drainWriter();
        assertEquals("new name", manager.getTaskById("b").getName());
        assertEquals(TaskType.EVENING, manager.getTaskById("a").getType());
        assertEquals(TaskMovePlanner.snapshot(manager.getAllTasks()), TaskMovePlanner.snapshot(disk()));
    }

    @Test public void xmlRoundTripRetainsBlocksIdsNotesDatesAndChecklistIdentity() throws Exception {
        Checklist custom = new Checklist("Stable custom", "custom-id");
        manager.addChecklist(custom);
        manager.addTask(TaskMovePlannerTest.task("parent", TaskType.CUSTOM, null));
        Task heading = TaskMovePlannerTest.task("heading", TaskType.HEADING, "parent");
        heading.setChecklistId("custom-id");
        manager.addTask(heading);
        manager.addTask(TaskMovePlannerTest.task("hidden child", TaskType.CUSTOM, "parent"));
        manager.moveTasks(move("parent", TaskMovePlannerTest.MORNING, "a", null)).get(5, TimeUnit.SECONDS);
        manager.moveTasks(move("parent", TaskMovePlannerTest.CUSTOM, null, null)).get(5, TimeUnit.SECONDS);
        drainWriter();
        List<Task> persisted = disk();
        assertEquals(TaskMovePlanner.snapshot(manager.getAllTasks()), TaskMovePlanner.snapshot(persisted));
        assertEquals(7, persisted.size());
        Task parent = persisted.stream().filter(t -> t.getId().equals("parent")).findFirst().get();
        assertEquals("custom-id", parent.getChecklistId());
        assertEquals("2099-12-31", parent.getDoneDate());
        assertTrue(parent.isDone());
        assertEquals("note & <parent>", parent.getNote());
        Task restoredHeading = persisted.stream().filter(t -> t.getId().equals("heading")).findFirst().get();
        assertEquals(TaskType.HEADING, restoredHeading.getType());
        assertEquals("custom-id", restoredHeading.getChecklistId());
        assertEquals("parent", restoredHeading.getParentId());
        assertEquals("Stable custom", repository.getChecklists().iterator().next().getName());
    }

    @Test public void staleMissingChecklistAndAnchorAreExceptionalNotSuccessfulNoOps() throws Exception {
        CompletableFuture<List<String>> missing = manager.moveTasks(move("a", TaskMovePlannerTest.CUSTOM, null, null));
        assertThrows(java.util.concurrent.ExecutionException.class, () -> missing.get(5, TimeUnit.SECONDS));
        CompletableFuture<List<String>> anchor = manager.moveTasks(move("a", TaskMovePlannerTest.EVENING, "missing", null));
        assertThrows(java.util.concurrent.ExecutionException.class, () -> anchor.get(5, TimeUnit.SECONDS));
        assertEquals(TaskType.MORNING, manager.getTaskById("a").getType());
    }
}
