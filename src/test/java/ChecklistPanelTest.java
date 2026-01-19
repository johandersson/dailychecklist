import javax.swing.*;

public class ChecklistPanelTest {
    public static void main(String[] args) {
        // Create stubs
        StubTaskManager taskManager = new StubTaskManager();
        StubTaskUpdater taskUpdater = new StubTaskUpdater();

        // Add some tasks
        taskManager.addTask(new Task("Morning Task 1", TaskType.MORNING, null));
        taskManager.addTask(new Task("Evening Task 1", TaskType.EVENING, null));

        // Create the panel
        ChecklistPanel panel = new ChecklistPanel(taskManager, taskUpdater);

        // Create a JFrame to display the panel
        JFrame frame = new JFrame("ChecklistPanel Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setSize(400, 600);
        frame.setVisible(true);

        // Update tasks to populate the lists
        SwingUtilities.invokeLater(panel::updateTasks);
    }
}