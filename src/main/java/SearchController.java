import java.util.List;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;

/**
 * Encapsulates search/query logic used by the search dialog.
 * Extracted to separate class for separation of concerns and testability.
 */
public class SearchController {
    private final TaskManager taskManager;

    public SearchController(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    /**
     * Updates the supplied unified model with checklists and matching tasks.
     */
    public void updateModel(String query, boolean includeAllWeekday, DefaultListModel<Object> unifiedModel) {
        final String q = query == null ? "" : query.toLowerCase();
        final String currentWeekday = java.time.LocalDateTime.now().getDayOfWeek().toString().toLowerCase();

        List<Task> allTasks = taskManager.getAllTasks();
        List<Task> results = allTasks.stream()
            .filter(task -> task.getName() != null && task.getName().toLowerCase().contains(q))
            .filter(task -> {
                if (task.getWeekday() == null) return true;
                if (includeAllWeekday) return true;
                return task.getWeekday().toLowerCase().equals(currentWeekday);
            })
            .collect(Collectors.toList());

        List<Checklist> allLists = taskManager.getCustomChecklists().stream()
            .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(q))
            .collect(Collectors.toList());

        javax.swing.SwingUtilities.invokeLater(() -> {
            unifiedModel.clear();
            for (Checklist c : allLists) unifiedModel.addElement(c);
            for (Task t : results) unifiedModel.addElement(t);
        });
    }
}
