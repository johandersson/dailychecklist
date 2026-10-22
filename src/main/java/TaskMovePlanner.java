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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Pure moves over the complete, flat persisted task sequence (not filtered UI rows). */
public final class TaskMovePlanner {
    private TaskMovePlanner() {}

    public record Destination(TaskType type, String checklistId) {}

    public record Intent(List<String> taskIds, Destination destination,
                         String beforeTaskId, String parentId) {
        public Intent {
            taskIds = taskIds == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(taskIds));
        }
    }

    /** All persisted fields, captured by value; Task's display/nested caches are not canonical. */
    public record TaskValue(String id, String name, TaskType type, String weekday,
                            boolean done, String doneDate, String checklistId,
                            String parentId, String note) {
        public static TaskValue of(Task task) {
            return new TaskValue(task.getId(), task.getName(), task.getType(), task.getWeekday(),
                task.isDone(), task.getDoneDate(), task.getChecklistId(), task.getParentId(), task.getNote());
        }

        public Task toTask() {
            Task task = new Task(id, name, type, weekday, done, doneDate, checklistId, parentId);
            task.setNote(note);
            return task;
        }

        private TaskValue moved(Destination destination, String parent) {
            return new TaskValue(id, name, type == TaskType.HEADING ? type : destination.type(),
                weekday, done, doneDate, destination.checklistId(), parent, note);
        }
    }

    public record Snapshot(List<TaskValue> tasks, Set<String> checklistIds) {
        public Snapshot {
            tasks = List.copyOf(tasks);
            checklistIds = checklistIds == null ? null : Set.copyOf(checklistIds);
        }

        public List<Task> toTasks() {
            List<Task> result = new ArrayList<>();
            for (TaskValue value : tasks) result.add(value.toTask());
            return result;
        }
    }

    public enum Rejection {
        INVALID_DESTINATION, UNKNOWN_CHECKLIST, EMPTY_SELECTION, INVALID_SNAPSHOT,
        UNKNOWN_TASK, HEADING_SELECTION, INVALID_PARENT, INVALID_ANCHOR, CYCLE, GRANDCHILD
    }

    public record Result(Snapshot snapshot, List<String> selectedIds, Rejection rejection) {
        public Result {
            selectedIds = List.copyOf(selectedIds);
        }
        public boolean accepted() { return rejection == null; }
        private static Result reject(Rejection rejection) {
            return new Result(null, List.of(), rejection);
        }
    }

    public static Snapshot snapshot(List<Task> tasks) {
        return snapshotWithIds(tasks, null);
    }

    public static Snapshot snapshot(List<Task> tasks, Set<Checklist> checklists) {
        Set<String> ids = new HashSet<>();
        for (Checklist checklist : checklists) ids.add(checklist.getId());
        return snapshotWithIds(tasks, ids);
    }

    private static Snapshot snapshotWithIds(List<Task> tasks, Set<String> ids) {
        List<TaskValue> values = new ArrayList<>();
        for (Task task : tasks) values.add(TaskValue.of(task));
        return new Snapshot(values, ids);
    }

    public static Result plan(Snapshot source, Intent intent) {
        if (intent == null || intent.destination() == null) return Result.reject(Rejection.INVALID_DESTINATION);
        Destination destination = intent.destination();
        if (destination.type() == null || destination.type() == TaskType.HEADING
            || (destination.type() == TaskType.CUSTOM
                ? destination.checklistId() == null || destination.checklistId().isBlank()
                    || !destination.checklistId().equals(destination.checklistId().trim())
                : destination.checklistId() != null)) return Result.reject(Rejection.INVALID_DESTINATION);
        if (destination.type() == TaskType.CUSTOM && source.checklistIds() != null
            && !source.checklistIds().contains(destination.checklistId())) return Result.reject(Rejection.UNKNOWN_CHECKLIST);
        Set<String> selected = new LinkedHashSet<>(intent.taskIds());
        if (selected.isEmpty()) return Result.reject(Rejection.EMPTY_SELECTION);
        Map<String, TaskValue> byId = new LinkedHashMap<>();
        for (TaskValue value : source.tasks()) {
            if (value.id() == null || value.type() == null || byId.put(value.id(), value) != null)
                return Result.reject(Rejection.INVALID_SNAPSHOT);
        }
        for (TaskValue value : source.tasks()) {
            if (value.parentId() == null) {
                if (value.type() == TaskType.HEADING) return Result.reject(Rejection.INVALID_SNAPSHOT);
            } else {
                TaskValue parent = byId.get(value.parentId());
                if (parent == null || parent.parentId() != null || parent.type() == TaskType.HEADING
                    || parent.id().equals(value.id())) return Result.reject(Rejection.INVALID_SNAPSHOT);
            }
        }
        for (String id : selected) {
            TaskValue value = byId.get(id);
            if (value == null) return Result.reject(Rejection.UNKNOWN_TASK);
            if (value.type() == TaskType.HEADING) return Result.reject(Rejection.HEADING_SELECTION);
        }
        List<TaskValue> roots = new ArrayList<>();
        List<String> selectedIds = new ArrayList<>();
        for (TaskValue value : source.tasks()) {
            if (selected.contains(value.id())) {
                selectedIds.add(value.id());
                if (!selected.contains(value.parentId())) roots.add(value);
            }
        }
        Set<String> moving = new HashSet<>();
        List<TaskValue> block = new ArrayList<>();
        for (TaskValue root : roots) {
            moving.add(root.id());
            block.add(root.moved(destination, intent.parentId()));
            for (TaskValue child : source.tasks()) {
                if (root.id().equals(child.parentId())) {
                    if (intent.parentId() != null) return Result.reject(Rejection.GRANDCHILD);
                    moving.add(child.id());
                    block.add(child.moved(destination, root.id()));
                }
            }
        }
        if (intent.parentId() != null) {
            if (moving.contains(intent.parentId())) return Result.reject(Rejection.CYCLE);
            TaskValue parent = byId.get(intent.parentId());
            if (parent == null || parent.parentId() != null || parent.type() == TaskType.HEADING
                || !belongs(parent, destination)) return Result.reject(Rejection.INVALID_PARENT);
        }
        if (intent.beforeTaskId() != null) {
            TaskValue anchor = byId.get(intent.beforeTaskId());
            if (anchor == null || anchor.type() == TaskType.HEADING || moving.contains(anchor.id())
                || !belongs(anchor, destination) || !Objects.equals(anchor.parentId(), intent.parentId()))
                return Result.reject(Rejection.INVALID_ANCHOR);
        }
        List<TaskValue> remaining = new ArrayList<>();
        for (TaskValue value : source.tasks()) if (!moving.contains(value.id())) remaining.add(value);
        int insertion = remaining.size();
        if (intent.beforeTaskId() != null) {
            for (int i = 0; i < remaining.size(); i++) {
                if (remaining.get(i).id().equals(intent.beforeTaskId())) { insertion = i; break; }
            }
        } else {
            // Append after the complete last sibling block, including hidden children/headings.
            Set<String> destinationRoots = new HashSet<>();
            if (intent.parentId() != null) destinationRoots.add(intent.parentId());
            for (TaskValue value : remaining) {
                if (belongs(value, destination) && value.parentId() == null) destinationRoots.add(value.id());
            }
            for (int i = 0; i < remaining.size(); i++) {
                TaskValue value = remaining.get(i);
                boolean inGroup = intent.parentId() == null
                    ? destinationRoots.contains(value.id()) || destinationRoots.contains(value.parentId())
                    : intent.parentId().equals(value.id()) || intent.parentId().equals(value.parentId());
                if (inGroup) insertion = i + 1;
            }
        }
        remaining.addAll(insertion, block);
        return new Result(new Snapshot(remaining, source.checklistIds()), selectedIds, null);
    }

    private static boolean belongs(TaskValue value, Destination destination) {
        return value.type() == destination.type()
            && (destination.type() != TaskType.CUSTOM || Objects.equals(value.checklistId(), destination.checklistId()));
    }
}
