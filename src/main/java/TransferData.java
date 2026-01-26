import java.util.List;

/**
 * Simple data holder for transfer operations.
 */
public final class TransferData {
    public final String sourceChecklistName;
    public final List<Task> tasks;

    public TransferData(String sourceChecklistName, List<Task> tasks) {
        this.sourceChecklistName = sourceChecklistName;
        this.tasks = tasks;
    }
}
