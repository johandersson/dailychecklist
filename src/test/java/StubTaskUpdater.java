import javax.swing.DefaultListModel;
import java.util.List;

public class StubTaskUpdater extends TaskUpdater {
    @Override
    public void updateTasks(List<Task> allTasks, DefaultListModel<Task> morningListModel, DefaultListModel<Task> eveningListModel, boolean showWeekdayTasks) {
        morningListModel.clear();
        eveningListModel.clear();
        for (Task task : allTasks) {
            if (task.getType() == TaskType.MORNING) {
                morningListModel.addElement(task);
            } else {
                eveningListModel.addElement(task);
            }
        }
    }
}