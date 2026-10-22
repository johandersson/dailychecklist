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
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

public final class TaskDropController {
    private static final String PENDING = TaskDropController.class.getName() + ".pending";
    private static final String CONTEXT = TaskDropController.class.getName() + ".context";
    // Overview drops may target a panel that has not been constructed yet.
    private static final Map<TaskManager, Map<TaskMovePlanner.Destination, List<String>>> pending =
            new WeakHashMap<>();

    private record Context(TaskManager manager, Supplier<TaskMovePlanner.Destination> destination) {}

    private TaskDropController() {}

    static void register(JList<Task> list, TaskManager manager,
            Supplier<TaskMovePlanner.Destination> destination) {
        list.putClientProperty(CONTEXT, new Context(manager, destination));
    }

    static TaskMovePlanner.Destination resolveDestination(TaskManager manager,
            String name, JList<Task> list) {
        return resolveDestination(manager, name, list.getModel());
    }

    static TaskMovePlanner.Destination resolveDestination(TaskManager manager,
            String name, ListModel<Task> rows) {
        // A custom checklist is resolved before reserved daily display names.
        for (Checklist checklist : manager.getCustomChecklists()) {
            if (Objects.equals(checklist.getName(), name)) {
                return new TaskMovePlanner.Destination(TaskType.CUSTOM, checklist.getId());
            }
        }
        if ("MORNING".equals(name) || "EVENING".equals(name)) {
            return new TaskMovePlanner.Destination(TaskType.valueOf(name), null);
        }
        if (name == null && rows.getSize() > 0) {
            Task row = rows.getElementAt(0);
            if (row.getType() == TaskType.MORNING || row.getType() == TaskType.EVENING) {
                return new TaskMovePlanner.Destination(row.getType(), null);
            }
        }
        throw new IllegalArgumentException("The destination checklist no longer exists");
    }

    static List<Task> selectedTasks(TaskManager manager, List<String> ids) {
        List<Task> selected = new ArrayList<>();
        for (String id : ids) {
            Task task = manager.getTaskById(id);
            if (task == null) {
                throw new IllegalArgumentException("A dragged task no longer exists");
            }
            if (task.getType() == TaskType.HEADING && !ids.contains(task.getParentId())) {
                throw new IllegalArgumentException("Headings move only with their parent task");
            }
            selected.add(task);
        }
        return selected;
    }

    static void validateCustomSelection(TaskManager manager, List<String> ids) {
        for (Task task : selectedTasks(manager, ids)) {
            if (task.getType() != TaskType.CUSTOM && task.getType() != TaskType.HEADING) {
                throw new IllegalArgumentException("Only custom tasks can be moved in the overview");
            }
            if (task.getChecklistId() == null || manager.getCustomChecklists().stream()
                    .noneMatch(c -> c.getId().equals(task.getChecklistId()))) {
                throw new IllegalArgumentException("The source checklist no longer exists");
            }
        }
    }

    static TaskMovePlanner.Intent buildIntent(TaskManager manager, List<String> ids,
            TaskMovePlanner.Destination destination, ListModel<Task> rows,
            int dropIndex, boolean insert) {
        List<Task> tasks = selectedTasks(manager, ids);
        boolean topLevel = tasks.stream().filter(t -> t.getType() != TaskType.HEADING)
                .filter(t -> t.getParentId() == null || !ids.contains(t.getParentId()))
                .allMatch(t -> t.getParentId() == null);
        boolean same = tasks.stream().filter(t -> t.getType() != TaskType.HEADING)
                .allMatch(t -> belongsTo(t, destination));
        boolean crossDaily = destination.type() != TaskType.CUSTOM
                && tasks.stream().allMatch(t -> t.getType() == TaskType.MORNING
                        || t.getType() == TaskType.EVENING || t.getType() == TaskType.HEADING);
        int size = rows.getSize();
        if (dropIndex < 0 || dropIndex > size || (!insert && dropIndex == size)) {
            throw new IllegalArgumentException("Invalid task drop location");
        }
        Task row = dropIndex < size ? rows.getElementAt(dropIndex) : null;
        String parent = null;
        String before = null;
        boolean forceTopLevel = (same && topLevel) || (!same && crossDaily);
        if (!forceTopLevel && row != null && !(insert && dropIndex == 0)) {
            if (row.getParentId() != null) {
                parent = row.getParentId();
                if (row.getType() != TaskType.HEADING) {
                    before = row.getId();
                }
            } else if (!insert) {
                parent = row.getId();
            }
        }
        if (!forceTopLevel && insert && row == null && size > 0) {
            Task previous = rows.getElementAt(size - 1);
            if (previous.getParentId() != null && previous.getType() != TaskType.HEADING) {
                parent = previous.getParentId();
            }
        }
        if (parent == null) {
            // A visible child is an anchor for its entire canonical parent group.
            for (int i = dropIndex; i < size; i++) {
                Task candidate = rows.getElementAt(i);
                String group = candidate.getParentId() == null
                        ? candidate.getId() : candidate.getParentId();
                if (!ids.contains(group)) {
                    before = group;
                    break;
                }
                before = null;
            }
        } else if (before != null && ids.contains(before)) {
            before = null;
            for (int i = dropIndex + 1; i < size; i++) {
                Task candidate = rows.getElementAt(i);
                if (!parent.equals(candidate.getParentId())) {
                    break;
                }
                if (candidate.getType() != TaskType.HEADING && !ids.contains(candidate.getId())) {
                    before = candidate.getId();
                    break;
                }
            }
        }
        return new TaskMovePlanner.Intent(ids, destination, before, parent);
    }

    private static boolean belongsTo(Task task, TaskMovePlanner.Destination destination) {
        return task.getType() == destination.type()
                && (destination.type() != TaskType.CUSTOM
                    || Objects.equals(task.getChecklistId(), destination.checklistId()));
    }

    static boolean submit(TaskManager manager, TaskMovePlanner.Intent intent,
            JList<Task> target, Runnable refresh, Runnable afterSuccess) {
        try {
            manager.moveTasks(intent).whenComplete((ids, failure) -> SwingUtilities.invokeLater(() -> {
                if (failure != null) {
                    showFailure(target, failure);
                    return;
                }
                List<String> selected = List.copyOf(ids);
                pending.computeIfAbsent(manager, ignored -> new HashMap<>())
                        .put(intent.destination(), selected);
                if (target != null) {
                    target.putClientProperty(PENDING, selected);
                }
                if (afterSuccess != null) {
                    afterSuccess.run();
                }
                // Explicit refresh also covers callers without task-change listeners.
                if (refresh != null) {
                    refresh.run();
                }
            }));
            return true;
        } catch (RuntimeException failure) {
            showFailure(target, failure);
            return false;
        }
    }

    public static void restoreSelection(JList<Task> list) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Selection must be restored on the EDT");
        }
        Context context = (Context) list.getClientProperty(CONTEXT);
        TaskMovePlanner.Destination destination = null;
        Map<TaskMovePlanner.Destination, List<String>> byDestination = null;
        if (context != null) {
            try {
                destination = context.destination().get();
                byDestination = pending.get(context.manager());
            } catch (IllegalArgumentException ignored) {
                return;
            }
        }
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) list.getClientProperty(PENDING);
        if (ids == null && byDestination != null) {
            ids = byDestination.get(destination);
            list.putClientProperty(PENDING, ids);
        }
        if (ids == null) {
            return;
        }
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < list.getModel().getSize(); i++) {
            if (ids.contains(list.getModel().getElementAt(i).getId())) {
                indices.add(i);
            }
        }
        if (indices.isEmpty()) {
            return;
        }
        list.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
        list.ensureIndexIsVisible(indices.get(0));
        list.putClientProperty(PENDING, null);
        if (byDestination != null) {
            byDestination.remove(destination);
        }
    }

    static void showFailure(Component parent, Throwable failure) {
        while (failure instanceof CompletionException && failure.getCause() != null) {
            failure = failure.getCause();
        }
        if (!GraphicsEnvironment.isHeadless()) {
            Exception error = new Exception(failure.getMessage() == null
                    ? "The task move failed" : failure.getMessage(), failure);
            ApplicationErrorHandler.showDataSaveError(parent, "Task move", error);
        }
    }
}
