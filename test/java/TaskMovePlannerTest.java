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

    @Test public void headingSelectionIsAllowedOnlyWithItsSelectedParent() {
        List<Task> tasks = List.of(task("p", TaskType.MORNING, null), task("h", TaskType.HEADING, "p"),
            task("q", TaskType.MORNING, null));
        assertEquals(TaskMovePlanner.Rejection.HEADING_SELECTION,
            plan(tasks, List.of("h"), MORNING, null, null).rejection());
        assertEquals(TaskMovePlanner.Rejection.HEADING_SELECTION,
            plan(tasks, List.of("h", "q"), MORNING, null, null).rejection());
        TaskMovePlanner.Result result = plan(tasks, List.of("h", "p"), EVENING, null, null);
        assertTrue(result.accepted());
        assertEquals(List.of("q", "p", "h"), ids(result.snapshot().toTasks()));
        assertEquals(List.of("p"), result.selectedIds());
        assertEquals("p", result.snapshot().toTasks().get(2).getParentId());
    }

    @Test public void headingMetadataDoesNotPreventNestingAnOtherwiseChildlessTask() {
        List<Task> tasks = List.of(task("p", TaskType.MORNING, null), task("h", TaskType.HEADING, "p"),
            task("q", TaskType.MORNING, null));
        TaskMovePlanner.Result nested = plan(tasks, List.of("p", "h"), MORNING, null, "q");
        assertTrue(nested.accepted());
        assertEquals(List.of("q", "p", "h"), ids(nested.snapshot().toTasks()));
        assertEquals("q", nested.snapshot().toTasks().get(1).getParentId());
        assertEquals("p", nested.snapshot().toTasks().get(2).getParentId());
        TaskMovePlanner.Result promoted = plan(nested.snapshot().toTasks(), List.of("p", "h"), EVENING, null, null);
        assertTrue(promoted.accepted());
        assertNull(promoted.snapshot().toTasks().get(1).getParentId());
        assertEquals("p", promoted.snapshot().toTasks().get(2).getParentId());
    }

    @Test public void invalidCanonicalHierarchyOrDuplicateIdsRejectWithoutDroppingAnything() {
        assertEquals(TaskMovePlanner.Rejection.INVALID_SNAPSHOT, plan(
            List.of(task("a", TaskType.MORNING, "missing")), List.of("a"), EVENING, null, null).rejection());
        assertFalse(plan(List.of(task("a", TaskType.MORNING, null), task("a", TaskType.MORNING, null)),
            List.of("a"), EVENING, null, null).accepted());
    }

    @Test public void orphanHeadingIsRemovedBeforePlanningMoves() {
        List<Task> tasks = List.of(
            task("parent", TaskType.CUSTOM, null),
            task("orphan-heading", TaskType.HEADING, "missing-parent"),
            task("other", TaskType.CUSTOM, null));

        TaskMovePlanner.Snapshot snapshot = TaskMovePlanner.snapshot(tasks);
        assertEquals(List.of("parent", "other"), ids(snapshot.toTasks()));

        TaskMovePlanner.Result result = TaskMovePlanner.plan(snapshot,
            new TaskMovePlanner.Intent(List.of("parent"), CUSTOM, "other", null));
        assertTrue(result.accepted());
    }

    @Test public void rejectsExistingChildMembershipMismatch() {
        assertEquals(TaskMovePlanner.Rejection.INVALID_SNAPSHOT, plan(
            List.of(task("p", TaskType.MORNING, null), task("c", TaskType.EVENING, "p")),
            List.of("p"), EVENING, null, null).rejection());
        Task child = task("c", TaskType.CUSTOM, "p");
        child.setChecklistId("different-id");
        assertEquals(TaskMovePlanner.Rejection.INVALID_SNAPSHOT, plan(
            List.of(task("p", TaskType.CUSTOM, null), child),
            List.of("p"), EVENING, null, null).rejection());
    }

    @Test public void insertingBeforeParentDoesNotSplitLeadingHeadingDecoration() {
        List<Task> tasks = List.of(task("a", TaskType.MORNING, null),
            task("h", TaskType.HEADING, "p"), task("p", TaskType.EVENING, null),
            task("c", TaskType.EVENING, "p"));
        TaskMovePlanner.Result result = plan(tasks, List.of("a"), EVENING, "p", null);
        assertTrue(result.accepted());
        assertEquals(List.of("a", "h", "p", "c"), ids(result.snapshot().toTasks()));
    }

    @Test public void parentBlockIncludesMetadataAttachedToItsChild() {
        List<Task> tasks = List.of(task("p", TaskType.MORNING, null), task("c", TaskType.MORNING, "p"),
            task("h", TaskType.HEADING, "c"), task("q", TaskType.EVENING, null));
        TaskMovePlanner.Result result = plan(tasks, List.of("p"), EVENING, null, null);
        assertTrue(result.accepted());
        List<Task> moved = result.snapshot().toTasks();
        assertEquals(List.of("q", "p", "c", "h"), ids(moved));
        assertEquals(TaskType.HEADING, moved.get(3).getType());
        assertEquals("c", moved.get(3).getParentId());
        TaskMovePlanner.Result append = plan(moved, List.of("q"), EVENING, null, null);
        assertEquals(List.of("p", "c", "h", "q"), ids(append.snapshot().toTasks()));
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

    @Test public void headingWithValidParentCanBeMoveAlongWithParent() {
        // When both a task and its HEADING are selected, the move should be accepted
        List<Task> tasks = List.of(task("p", TaskType.MORNING, null), task("h", TaskType.HEADING, "p"),
            task("q", TaskType.MORNING, null));
        TaskMovePlanner.Result result = plan(tasks, List.of("p", "h"), EVENING, null, null);
        assertTrue("Moving task with its HEADING should be accepted", result.accepted());
        assertEquals(List.of("q", "p", "h"), ids(result.snapshot().toTasks()));
    }

    @Test public void movinJustTaskWithoutHeadingIgnoresMetadata() {
        // When only the task is selected (not its HEADING), the move should still work
        // The HEADING will automatically follow as metadata
        List<Task> tasks = List.of(task("p", TaskType.MORNING, null), task("h", TaskType.HEADING, "p"),
            task("q", TaskType.MORNING, null));
        TaskMovePlanner.Result result = plan(tasks, List.of("p"), MORNING, null, "q");
        assertTrue("Moving just the task should work; HEADING follows as metadata", result.accepted());
        // After moving p under q, the h should still be under p
        List<Task> moved = result.snapshot().toTasks();
        Task movedP = moved.stream().filter(t -> t.getId().equals("p")).findFirst().orElse(null);
        assertNotNull("Task p should exist after move", movedP);
        assertEquals("p should now be a subtask of q", "q", movedP.getParentId());
    }

    @Test public void topLevelTaskBecomesChildOfAnotherTopLevelTask() {
        // Scenario: p and q are both top-level MORNING tasks
        // Drag p under q to make p a child of q
        List<Task> tasks = List.of(
            task("p", TaskType.MORNING, null),
            task("h1", TaskType.HEADING, "p"),
            task("q", TaskType.MORNING, null),
            task("h2", TaskType.HEADING, "q")
        );
        
        // Move p to become a child of q (parentId="q")
        TaskMovePlanner.Result result = plan(tasks, List.of("p"), MORNING, null, "q");
        assertTrue("Should accept moving top-level task as child of another top-level task", result.accepted());
        
        List<Task> moved = result.snapshot().toTasks();
        Task movedP = moved.stream().filter(t -> t.getId().equals("p")).findFirst().orElse(null);
        assertNotNull("Task p should exist after move", movedP);
        assertEquals("p should now have q as parent", "q", movedP.getParentId());
        assertEquals("p should still be MORNING type", TaskType.MORNING, movedP.getType());
        
        // Verify h1 (heading of p) also moved and kept its parent relationship
        Task movedH1 = moved.stream().filter(t -> t.getId().equals("h1")).findFirst().orElse(null);
        assertNotNull("Heading h1 should exist after move", movedH1);
        assertEquals("h1 should still have p as parent", "p", movedH1.getParentId());
    }

    @Test public void childTaskBecomesTopLevel() {
        // Scenario: c is a child of p, p is top-level
        // Drag c up to make it top-level (remove parent)
        List<Task> tasks = List.of(
            task("p", TaskType.MORNING, null),
            task("c", TaskType.MORNING, "p"),
            task("h", TaskType.HEADING, "c")
        );
        
        // Move c to top-level (parentId=null)
        TaskMovePlanner.Result result = plan(tasks, List.of("c"), MORNING, null, null);
        assertTrue("Should accept moving child task to top-level", result.accepted());
        
        List<Task> moved = result.snapshot().toTasks();
        Task movedC = moved.stream().filter(t -> t.getId().equals("c")).findFirst().orElse(null);
        assertNotNull("Task c should exist after move", movedC);
        assertNull("c should have no parent (top-level)", movedC.getParentId());
        assertEquals("c should still be MORNING type", TaskType.MORNING, movedC.getType());
        
        // Verify h (heading of c) stayed with c
        Task movedH = moved.stream().filter(t -> t.getId().equals("h")).findFirst().orElse(null);
        assertNotNull("Heading h should exist after move", movedH);
        assertEquals("h should still have c as parent", "c", movedH.getParentId());
    }

    @Test public void childTaskBecomesChildOfDifferentParent() {
        // Scenario: c1 is a child of p1, move c1 to be a child of p2
        List<Task> tasks = List.of(
            task("p1", TaskType.MORNING, null),
            task("c1", TaskType.MORNING, "p1"),
            task("h1", TaskType.HEADING, "c1"),
            task("p2", TaskType.MORNING, null),
            task("c2", TaskType.MORNING, "p2")
        );
        
        // Move c1 to become a child of p2
        TaskMovePlanner.Result result = plan(tasks, List.of("c1"), MORNING, null, "p2");
        assertTrue("Should accept moving child from one parent to another", result.accepted());
        
        List<Task> moved = result.snapshot().toTasks();
        Task movedC1 = moved.stream().filter(t -> t.getId().equals("c1")).findFirst().orElse(null);
        assertNotNull("Task c1 should exist after move", movedC1);
        assertEquals("c1 should now have p2 as parent", "p2", movedC1.getParentId());
        assertEquals("c1 should still be MORNING type", TaskType.MORNING, movedC1.getType());
        
        // Verify h1 stayed with c1
        Task movedH1 = moved.stream().filter(t -> t.getId().equals("h1")).findFirst().orElse(null);
        assertNotNull("Heading h1 should exist after move", movedH1);
        assertEquals("h1 should still have c1 as parent", "c1", movedH1.getParentId());
    }

    @Test public void multipleTopLevelTasksMovedTogether() {
        // Scenario: p1 and p2 are both top-level, move both to become children of p3
        List<Task> tasks = List.of(
            task("p1", TaskType.MORNING, null),
            task("p2", TaskType.MORNING, null),
            task("p3", TaskType.MORNING, null)
        );
        
        // Move both p1 and p2 to become children of p3
        TaskMovePlanner.Result result = plan(tasks, List.of("p1", "p2"), MORNING, null, "p3");
        assertTrue("Should accept moving multiple top-level tasks as children", result.accepted());
        
        List<Task> moved = result.snapshot().toTasks();
        Task movedP1 = moved.stream().filter(t -> t.getId().equals("p1")).findFirst().orElse(null);
        Task movedP2 = moved.stream().filter(t -> t.getId().equals("p2")).findFirst().orElse(null);
        
        assertNotNull("Task p1 should exist after move", movedP1);
        assertNotNull("Task p2 should exist after move", movedP2);
        assertEquals("p1 should have p3 as parent", "p3", movedP1.getParentId());
        assertEquals("p2 should have p3 as parent", "p3", movedP2.getParentId());
    }

    @Test public void reorderingTopLevelTasks() {
        // Scenario: p1, p2, p3 are all top-level; reorder them to p2, p1, p3
        List<Task> tasks = List.of(
            task("p1", TaskType.MORNING, null),
            task("p2", TaskType.MORNING, null),
            task("p3", TaskType.MORNING, null)
        );
        
        // Move p1 to come before p2 (but both stay top-level)
        TaskMovePlanner.Result result = plan(tasks, List.of("p1"), MORNING, "p2", null);
        assertTrue("Should accept reordering top-level tasks", result.accepted());
        
        List<Task> moved = result.snapshot().toTasks();
        List<String> taskIds = ids(moved);
        
        // p1 should come before p2 in the result
        int p1Idx = taskIds.indexOf("p1");
        int p2Idx = taskIds.indexOf("p2");
        assertTrue("p1 should come before p2", p1Idx < p2Idx);
    }

    @Test public void moveTaskBetweenDailyTypes() {
        // Scenario: Move a MORNING task to EVENING
        List<Task> tasks = List.of(
            task("m1", TaskType.MORNING, null),
            task("e1", TaskType.EVENING, null)
        );
        
        // Move m1 from MORNING to EVENING
        TaskMovePlanner.Result result = plan(tasks, List.of("m1"), EVENING, null, null);
        assertTrue("Should accept moving task between MORNING and EVENING", result.accepted());
        
        List<Task> moved = result.snapshot().toTasks();
        Task movedM1 = moved.stream().filter(t -> t.getId().equals("m1")).findFirst().orElse(null);
        assertNotNull("Task m1 should exist after move", movedM1);
        assertEquals("m1 should now be EVENING type", TaskType.EVENING, movedM1.getType());
    }
}
