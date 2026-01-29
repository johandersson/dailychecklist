import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Helper to toggle a task's done state and apply related business rules
 * (propagate to subtasks, update parent when siblings complete, persist changes).
 */
public final class TaskDoneToggler {
    private TaskDoneToggler() {}

    public static void toggle(TaskManager taskManager, Task task, Runnable updateTasks) {
        boolean newDone = !task.isDone();
        System.out.println("[TRACE] TaskDoneToggler.toggle start id=" + task.getId() + ", parentId=" + task.getParentId() + ", newDone=" + newDone + ", thread=" + Thread.currentThread().getName());

        setDoneState(task, newDone);

        List<Task> subs = taskManager.getSubtasks(task.getId());
        boolean batchPersistScheduled = false;
        if (subs != null && !subs.isEmpty() && newDone) {
            batchPersistScheduled = markSubtasksDoneAndPersist(taskManager, task, subs, updateTasks);
        }

        handleParentSiblingLogic(taskManager, task, newDone);

        schedulePersist(taskManager, task, batchPersistScheduled, updateTasks);
    }

    private static void setDoneState(Task task, boolean newDone) {
        task.setDone(newDone);
        if (newDone) task.setDoneDate(new Date(System.currentTimeMillis()));
        else task.setDoneDate(null);
    }

    private static boolean markSubtasksDoneAndPersist(TaskManager taskManager, Task task, List<Task> subs, Runnable updateTasks) {
        List<Task> toUpdate = new ArrayList<>();
        for (Task sub : subs) {
            System.out.println("[TRACE] TaskDoneToggler: parent->sub mark id=" + sub.getId());
            setDoneState(sub, true);
            toUpdate.add(sub);
        }
        toUpdate.add(task);
        boolean ok = taskManager.updateTasksQuiet(toUpdate);
        System.out.println("[TRACE] TaskDoneToggler: scheduled persist " + subs.size() + " subtasks for parent=" + task.getId() + ", ok=" + ok);
        if (updateTasks != null) SwingUtilities.invokeLater(updateTasks);
        return true;
    }

    private static void handleParentSiblingLogic(TaskManager taskManager, Task task, boolean newDone) {
        if (task.getParentId() == null) return;
        List<Task> siblings = taskManager.getSubtasks(task.getParentId());
        boolean allSiblingsDone = true;
        if (siblings != null) {
            for (Task sib : siblings) {
                if (!sib.isDone()) { allSiblingsDone = false; break; }
            }
        }
        Task parent = taskManager.getTaskById(task.getParentId());
        if (parent != null && allSiblingsDone && newDone) {
            System.out.println("[TRACE] TaskDoneToggler: all siblings done -> mark parent id=" + parent.getId());
            setDoneState(parent, true);
            taskManager.updateTaskQuiet(parent);
        }
    }

    private static void schedulePersist(TaskManager taskManager, Task task, boolean batchPersistScheduled, Runnable updateTasks) {
        System.out.println("[TRACE] TaskDoneToggler: scheduling persist for clicked task id=" + task.getId());
        if (!batchPersistScheduled) {
            boolean ok = taskManager.updateTaskQuiet(task);
            if (updateTasks != null) SwingUtilities.invokeLater(updateTasks);
            System.out.println("[TRACE] TaskDoneToggler: scheduled persist clicked task id=" + task.getId() + ", ok=" + ok);
        } else {
            System.out.println("[TRACE] TaskDoneToggler: skipped single-task persist because batch persist scheduled for parent=" + task.getId());
        }
    }
}
