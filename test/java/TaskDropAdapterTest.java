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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class TaskDropAdapterTest {
    private static final TaskMovePlanner.Destination CUSTOM =
            new TaskMovePlanner.Destination(TaskType.CUSTOM, "custom");

    private static class Manager extends TaskManager {
        final List<Task> tasks = new ArrayList<>();
        CompletableFuture<List<String>> result = new CompletableFuture<>();
        final AtomicInteger submissions = new AtomicInteger();
        final Checklist checklist = new Checklist("Name;with;semicolons", "custom");

        Manager() {
            super(new TestHelpers.InMemoryTaskRepository());
        }

        @Override public Task getTaskById(String id) {
            return tasks.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
        }

        @Override public java.util.Set<Checklist> getCustomChecklists() {
            return java.util.Set.of(checklist);
        }

        @Override public CompletableFuture<List<String>> moveTasks(TaskMovePlanner.Intent intent) {
            submissions.incrementAndGet();
            return result;
        }
    }

    private static Task task(String id, TaskType type, String parent) {
        return new Task(id, id, type, null, false, null,
                type == TaskType.CUSTOM || type == TaskType.HEADING ? "custom" : null, parent);
    }

    private static DefaultListModel<Task> rows(Task... tasks) {
        DefaultListModel<Task> rows = new DefaultListModel<>();
        for (Task task : tasks) rows.addElement(task);
        return rows;
    }

    @Test public void payloadIsLocalTypedImmutableAndOrdered() throws Exception {
        List<String> original = new ArrayList<>(List.of("b;id", "a"));
        TaskTransferable transferable = new TaskTransferable(original);
        original.clear();
        assertEquals(List.of("b;id", "a"), TaskTransferable.readIds(transferable));
        DataFlavor flavor = transferable.getTransferDataFlavors()[0];
        assertTrue(flavor.isMimeTypeEqual(DataFlavor.javaJVMLocalObjectMimeType));
        assertEquals(TaskTransferable.TaskIds.class, flavor.getRepresentationClass());
        assertFalse(transferable.isDataFlavorSupported(DataFlavor.stringFlavor));
        assertFalse(TaskTransferable.supports(new StringSelection("custom;id")));
        assertThrows(UnsupportedOperationException.class,
                () -> TaskTransferable.readIds(transferable).add("other"));
        transferable.getTransferDataFlavors()[0] = DataFlavor.stringFlavor;
        assertTrue(TaskTransferable.supports(transferable));
    }

    @Test public void sourceCapturesIdsAtDragAndDoesNotRefreshOnCancel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Manager manager = new Manager();
            Task a = task("a", TaskType.CUSTOM, null);
            Task b = task("b", TaskType.CUSTOM, null);
            DefaultListModel<Task> model = rows(a, b);
            JList<Task> list = new JList<>(model);
            AtomicInteger refreshes = new AtomicInteger();
            TaskTransferHandler handler = new TaskTransferHandler(list, model, manager,
                    manager.checklist.getName(), refreshes::incrementAndGet);
            list.setSelectedIndices(new int[] {0, 1});
            Transferable payload = handler.createTransferable(list);
            list.clearSelection();
            model.clear();
            try {
                assertEquals(List.of("a", "b"), TaskTransferable.readIds(payload));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            assertEquals(0, refreshes.get());
        });
    }

    @Test public void sameListTopLevelDropAnchorsWholeGroupWithoutReparenting() {
        Manager manager = new Manager();
        Task a = task("a", TaskType.CUSTOM, null);
        Task parent = task("parent", TaskType.CUSTOM, null);
        Task child = task("child", TaskType.CUSTOM, "parent");
        manager.tasks.add(a);
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(manager, List.of("a"),
                CUSTOM, rows(parent, child), 1, false);
        assertNull(intent.parentId());
        assertEquals("parent", intent.beforeTaskId());
    }

    @Test public void selectedAnchorSkipsToNextUnmovedVisibleGroup() {
        Manager manager = new Manager();
        Task a = task("a", TaskType.CUSTOM, null);
        Task child = task("child", TaskType.CUSTOM, "a");
        Task b = task("b", TaskType.CUSTOM, null);
        manager.tasks.add(a);
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(manager, List.of("a"),
                CUSTOM, rows(a, child, b), 0, true);
        assertEquals("b", intent.beforeTaskId());
        assertNull(intent.parentId());
    }

    @Test public void crossDailyDropRemainsTopLevel() {
        Manager manager = new Manager();
        manager.tasks.add(task("moving", TaskType.MORNING, null));
        Task parent = task("parent", TaskType.EVENING, null);
        Task child = task("child", TaskType.EVENING, "parent");
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(manager, List.of("moving"),
                new TaskMovePlanner.Destination(TaskType.EVENING, null),
                rows(parent, child), 1, false);
        assertEquals("parent", intent.beforeTaskId());
        assertNull(intent.parentId());
    }

    @Test public void childInsertionUsesVisibleSiblingIdAndGroupAppendUsesNull() {
        Manager manager = new Manager();
        manager.tasks.add(task("moving", TaskType.CUSTOM, "old"));
        Task parent = task("parent", TaskType.CUSTOM, null);
        Task child = task("child", TaskType.CUSTOM, "parent");
        TaskMovePlanner.Intent between = TaskDropController.buildIntent(manager,
                List.of("moving"), CUSTOM, rows(parent, child), 1, true);
        assertEquals("parent", between.parentId());
        assertEquals("child", between.beforeTaskId());
        TaskMovePlanner.Intent onParent = TaskDropController.buildIntent(manager,
                List.of("moving"), CUSTOM, rows(parent, child), 0, false);
        assertEquals("parent", onParent.parentId());
        assertNull(onParent.beforeTaskId());
        TaskMovePlanner.Intent append = TaskDropController.buildIntent(manager,
                List.of("moving"), CUSTOM, rows(parent, child), 2, true);
        assertNull(append.parentId());
        assertNull(append.beforeTaskId());
    }

    @Test public void headingsCannotBeDraggedAloneButCanAccompanyTheirParent() {
        Manager manager = new Manager();
        manager.tasks.add(task("parent", TaskType.CUSTOM, null));
        manager.tasks.add(task("heading", TaskType.HEADING, "parent"));
        assertThrows(IllegalArgumentException.class, () -> TaskDropController.buildIntent(
                manager, List.of("heading"), CUSTOM, rows(), 0, true));
        TaskMovePlanner.Intent intent = TaskDropController.buildIntent(manager,
                List.of("parent", "heading"), CUSTOM, rows(), 0, true);
        assertNull(intent.parentId());
    }

    @Test public void overviewValidatesEveryAuthoritativeTaskNotPayloadName() {
        Manager manager = new Manager();
        manager.tasks.add(task("custom", TaskType.CUSTOM, null));
        manager.tasks.add(task("daily", TaskType.MORNING, null));
        TaskDropController.validateCustomSelection(manager, List.of("custom"));
        assertThrows(IllegalArgumentException.class, () ->
                TaskDropController.validateCustomSelection(manager, List.of("custom", "daily")));
        assertThrows(IllegalArgumentException.class, () ->
                TaskDropController.validateCustomSelection(manager, List.of("missing")));
    }

    @Test public void successRestoresAfterRenderOnceAndRefreshRunsOnEdt() throws Exception {
        Manager manager = new Manager();
        Task moved = task("moved", TaskType.CUSTOM, null);
        DefaultListModel<Task> model = rows();
        JList<Task> list = new JList<>(model);
        AtomicInteger refreshes = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            TaskDropController.register(list, manager, () -> CUSTOM);
            TaskDropController.submit(manager,
                    new TaskMovePlanner.Intent(List.of("moved"), CUSTOM, null, null),
                    list, () -> {
                        assertTrue(SwingUtilities.isEventDispatchThread());
                        refreshes.incrementAndGet();
                        TaskDropController.restoreSelection(list);
                    }, null);
        });
        manager.result.complete(List.of("moved"));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, refreshes.get());
            model.addElement(moved);
            TaskDropController.restoreSelection(list);
            assertEquals(moved, list.getSelectedValue());
            list.clearSelection();
            TaskDropController.restoreSelection(list);
            assertTrue(list.isSelectionEmpty());
        });
    }

    @Test public void overviewPendingSelectionReachesPanelCreatedAfterCommit() throws Exception {
        Manager manager = new Manager();
        AtomicInteger callback = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> TaskDropController.submit(manager,
                new TaskMovePlanner.Intent(List.of("moved"), CUSTOM, null, null),
                null, null, callback::incrementAndGet));
        manager.result.complete(List.of("moved"));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, callback.get());
            Task moved = task("moved", TaskType.CUSTOM, null);
            JList<Task> list = new JList<>(rows(moved));
            TaskDropController.register(list, manager, () -> CUSTOM);
            TaskDropController.restoreSelection(list);
            assertEquals(moved, list.getSelectedValue());
        });
    }

    @Test public void failedCommitDoesNotRefreshOrSelectOverviewDestination() throws Exception {
        Manager manager = new Manager();
        AtomicInteger callbacks = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> TaskDropController.submit(manager,
                new TaskMovePlanner.Intent(List.of("moved"), CUSTOM, null, null),
                null, callbacks::incrementAndGet, callbacks::incrementAndGet));
        manager.result.completeExceptionally(new IllegalArgumentException("rejected"));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(0, callbacks.get());
            assertEquals(1, manager.submissions.get());
        });
    }

    @Test public void destinationNameResolvesToStableIdIncludingSemicolons() {
        Manager manager = new Manager();
        assertEquals(CUSTOM, TaskDropController.resolveDestination(manager,
                manager.checklist.getName(), new JList<>(rows())));
    }
}
