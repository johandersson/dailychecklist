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
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TaskMovePlannerTest {
    static final TaskMovePlanner.Destination MORNING = new TaskMovePlanner.Destination(TaskType.MORNING, null);
    static final TaskMovePlanner.Destination EVENING = new TaskMovePlanner.Destination(TaskType.EVENING, null);
    static final TaskMovePlanner.Destination CUSTOM = new TaskMovePlanner.Destination(TaskType.CUSTOM, "custom-id");

    static Task task(String id, TaskType type, String parent) {
        Task task = new Task(id, id, type, "monday", true, "2099-12-31",
            type == TaskType.CUSTOM ? "custom-id" : null, parent);
        task.setNote("note & <" + id + ">");
        return task;
    }

    static List<String> ids(List<Task> tasks) {
        return tasks.stream().map(Task::getId).collect(Collectors.toList());
    }

    static TaskMovePlanner.Result plan(List<Task> tasks, List<String> selected,
                                       TaskMovePlanner.Destination destination, String before, String parent) {
        return TaskMovePlanner.plan(TaskMovePlanner.snapshot(tasks),
            new TaskMovePlanner.Intent(selected, destination, before, parent));
    }

    @Test public void immutableInputAndOutputCaptureAllPersistedFields() {
        Task task = task("a", TaskType.MORNING, null);
        List<Task> input = new ArrayList<>(List.of(task));
        List<String> selected = new ArrayList<>(List.of("a"));
        TaskMovePlanner.Snapshot snapshot = TaskMovePlanner.snapshot(input);
        TaskMovePlanner.Intent intent = new TaskMovePlanner.Intent(selected, EVENING, null, null);
        selected.clear();
        input.clear();
        task.setName("changed");
        task.setNote("changed");
        task.setDone(false);
        TaskMovePlanner.Result result = TaskMovePlanner.plan(snapshot, intent);
        assertTrue(result.accepted());
        Task moved = result.snapshot().toTasks().get(0);
        assertEquals("a", moved.getName());
        assertEquals("note & <a>", moved.getNote());
        assertEquals("2099-12-31", moved.getDoneDate());
        assertEquals("monday", moved.getWeekday());
        assertTrue(moved.isDone());
        moved.setName("mutated output");
        assertEquals("a", result.snapshot().toTasks().get(0).getName());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.tasks().clear());
        assertThrows(UnsupportedOperationException.class, () -> intent.taskIds().clear());
    }

    @Test public void parentBlockIncludesHiddenChildrenAndHeadingAndMixedSelectionOnce() {
        List<Task> tasks = List.of(task("p", TaskType.MORNING, null),
            task("h", TaskType.HEADING, "p"), task("hidden", TaskType.MORNING, "p"),
            task("c", TaskType.MORNING, "p"), task("q", TaskType.EVENING, null),
            task("qc", TaskType.EVENING, "q"), task("other", TaskType.CUSTOM, null));
        TaskMovePlanner.Result result = plan(tasks, List.of("c", "p"), EVENING, "q", null);
        assertTrue(result.accepted());
        List<Task> output = result.snapshot().toTasks();
        assertEquals(List.of("p", "h", "hidden", "c", "q", "qc", "other"), ids(output));
        assertEquals(List.of("p", "c"), result.selectedIds());
        assertEquals(TaskType.HEADING, output.get(1).getType());
        assertEquals("p", output.get(1).getParentId());
        assertEquals(TaskType.EVENING, output.get(2).getType());
        assertEquals(TaskType.MORNING, tasks.get(2).getType());
        assertEquals(tasks.size(), new java.util.HashSet<>(ids(output)).size());
    }

    @Test public void selectedSiblingsKeepCanonicalOrderNotPayloadOrder() {
        List<Task> tasks = List.of(task("p", TaskType.MORNING, null), task("a", TaskType.MORNING, "p"),
            task("hidden", TaskType.MORNING, "p"), task("b", TaskType.MORNING, "p"),
            task("q", TaskType.MORNING, null), task("x", TaskType.MORNING, "q"));
        TaskMovePlanner.Result result = plan(tasks, List.of("b", "a"), MORNING, "x", "q");
        assertTrue(result.accepted());
        List<Task> output = result.snapshot().toTasks();
        assertEquals(List.of("p", "hidden", "q", "a", "b", "x"), ids(output));
        assertEquals("q", output.get(3).getParentId());
        assertEquals("p", output.get(1).getParentId());
    }

    @Test public void appendTopLevelAfterLastWholeDestinationBlock() {
        List<Task> tasks = List.of(task("a", TaskType.MORNING, null), task("p", TaskType.EVENING, null),
            task("heading", TaskType.HEADING, "p"), task("hidden", TaskType.EVENING, "p"),
            task("other", TaskType.CUSTOM, null));
        TaskMovePlanner.Result result = plan(tasks, List.of("a"), EVENING, null, null);
        assertEquals(List.of("p", "heading", "hidden", "a", "other"), ids(result.snapshot().toTasks()));
    }

    @Test public void appendChildAfterHeadingAndHiddenSiblings() {
        List<Task> tasks = List.of(task("a", TaskType.MORNING, null), task("p", TaskType.EVENING, null),
            task("heading", TaskType.HEADING, "p"), task("hidden", TaskType.EVENING, "p"),
            task("other", TaskType.CUSTOM, null));
        TaskMovePlanner.Result result = plan(tasks, List.of("a"), EVENING, null, "p");
        assertEquals(List.of("p", "heading", "hidden", "a", "other"), ids(result.snapshot().toTasks()));
        assertEquals("p", result.snapshot().toTasks().get(3).getParentId());
    }

    @Test public void childCanBePromotedAcrossChecklistsAndDailyMembershipIsCleared() {
        List<Task> tasks = List.of(task("p", TaskType.CUSTOM, null), task("c", TaskType.CUSTOM, "p"));
        TaskMovePlanner.Result result = plan(tasks, List.of("c"), MORNING, null, null);
        Task moved = result.snapshot().toTasks().get(1);
        assertNull(moved.getParentId());
        assertNull(moved.getChecklistId());
        assertEquals(TaskType.MORNING, moved.getType());
        TaskMovePlanner.Result back = plan(result.snapshot().toTasks(), List.of("c"), CUSTOM, "p", null);
        assertEquals("custom-id", back.snapshot().toTasks().get(0).getChecklistId());
    }

    @Test public void parentAndIndependentChildMoveAsSeparateBlocks() {
        List<Task> tasks = List.of(task("p", TaskType.MORNING, null), task("pc", TaskType.MORNING, "p"),
            task("q", TaskType.MORNING, null), task("qc", TaskType.MORNING, "q"));
        TaskMovePlanner.Result result = plan(tasks, List.of("qc", "p", "pc"), EVENING, null, null);
        assertEquals(List.of("q", "p", "pc", "qc"), ids(result.snapshot().toTasks()));
        assertEquals("p", result.snapshot().toTasks().get(2).getParentId());
        assertNull(result.snapshot().toTasks().get(3).getParentId());
    }

    @Test public void rejectsSelfCyclesGrandchildrenAndChildTargets() {
        List<Task> tasks = List.of(task("p", TaskType.MORNING, null), task("c", TaskType.MORNING, "p"),
            task("q", TaskType.MORNING, null));
        assertFalse(plan(tasks, List.of("q"), MORNING, null, "q").accepted());
        assertFalse(plan(tasks, List.of("p"), MORNING, null, "c").accepted());
        assertEquals(TaskMovePlanner.Rejection.GRANDCHILD,
            plan(tasks, List.of("p"), MORNING, null, "q").rejection());
        assertEquals(TaskMovePlanner.Rejection.INVALID_PARENT,
            plan(tasks, List.of("q"), MORNING, null, "c").rejection());
    }

    @Test public void rejectsStaleWrongGroupOrSelectedAnchor() {
        List<Task> tasks = List.of(task("p", TaskType.MORNING, null), task("c", TaskType.MORNING, "p"),
            task("q", TaskType.EVENING, null));
        assertEquals(TaskMovePlanner.Rejection.INVALID_ANCHOR,
            plan(tasks, List.of("p"), EVENING, "missing", null).rejection());
        assertFalse(plan(tasks, List.of("q"), MORNING, "c", null).accepted());
        assertFalse(plan(tasks, List.of("q"), EVENING, "q", null).accepted());
        assertFalse(plan(tasks, List.of("c"), EVENING, null, "p").accepted());
    }

    @Test public void rejectsInvalidDestinationChecklistHeadingAndMissingSelection() {
        List<Task> tasks = List.of(task("p", TaskType.MORNING, null), task("h", TaskType.HEADING, "p"));
        assertFalse(plan(tasks, List.of("h"), EVENING, null, null).accepted());
        assertFalse(plan(tasks, List.of("missing"), EVENING, null, null).accepted());
        assertFalse(plan(tasks, List.of(), EVENING, null, null).accepted());
        assertFalse(plan(tasks, List.of("p"), new TaskMovePlanner.Destination(TaskType.MORNING, "id"), null, null).accepted());
        assertFalse(plan(tasks, List.of("p"), new TaskMovePlanner.Destination(TaskType.CUSTOM, null), null, null).accepted());
        TaskMovePlanner.Result missingChecklist = TaskMovePlanner.plan(TaskMovePlanner.snapshot(tasks, Set.of()),
            new TaskMovePlanner.Intent(List.of("p"), CUSTOM, null, null));
        assertEquals(TaskMovePlanner.Rejection.UNKNOWN_CHECKLIST, missingChecklist.rejection());
    }

    @Test public void invalidCanonicalHierarchyOrDuplicateIdsRejectWithoutDroppingAnything() {
        assertEquals(TaskMovePlanner.Rejection.INVALID_SNAPSHOT, plan(
            List.of(task("a", TaskType.MORNING, "missing")), List.of("a"), EVENING, null, null).rejection());
        assertFalse(plan(List.of(task("a", TaskType.MORNING, null), task("a", TaskType.MORNING, null)),
            List.of("a"), EVENING, null, null).accepted());
    }

    @Test public void managerCopyIsDeepAndPreservesNotesAndCompletion() {
        Task task = task("p", TaskType.MORNING, null);
        Task child = task("c", TaskType.MORNING, "p");
        task.setSubtasks(new ArrayList<>(List.of(child)));
        Task copy = TaskManager.copyTask(task);
        copy.getSubtasks().get(0).setNote("edited");
        assertEquals("note & <c>", child.getNote());
        assertEquals(task.getDoneDate(), copy.getDoneDate());
        assertEquals(task.getNote(), copy.getNote());
        assertEquals(task.isDone(), copy.isDone());
        assertNotSame(task, copy);
        assertNotSame(task.getSubtasks(), copy.getSubtasks());
    }
}
