import java.util.Map;

/**
 * Holder for restore preview statistics.
 */
public class RestorePreview {
    public final Map<String,String> checklists;
    public final Map<String,Integer> checklistTaskCounts;
    public final int newChecklistCount;
    public final int morningCount;
    public final int eveningCount;

    public RestorePreview(Map<String,String> checklists, Map<String,Integer> checklistTaskCounts, int newChecklistCount, int morningCount, int eveningCount) {
        this.checklists = checklists;
        this.checklistTaskCounts = checklistTaskCounts;
        this.newChecklistCount = newChecklistCount;
        this.morningCount = morningCount;
        this.eveningCount = eveningCount;
    }
}
