import org.junit.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RecurringReminderUiTest {
    private TaskManager taskManagerWithReminders(List<Reminder> reminders) {
        TestHelpers.InMemoryTaskRepository repository = new TestHelpers.InMemoryTaskRepository() {
            @Override
            public List<Reminder> getReminders() {
                return new java.util.ArrayList<>(reminders);
            }
        };
        return new TaskManager(repository);
    }

    @Test
    public void reminderDateTime_handlesRecurringReminder() {
        LocalDateTime now = LocalDateTime.now();
        int daysBitmask = 1 << (now.getDayOfWeek().getValue() - 1);
        Reminder reminder = new Reminder("Work", daysBitmask, now.getHour(), now.getMinute(), null);

        LocalDateTime resolved = ReminderSelector.reminderDateTime(reminder);

        assertNotNull(resolved);
    }

    @Test
    public void checklistCellRenderer_handlesRecurringReminderWithoutInvalidDate() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        int daysBitmask = 1 << (now.getDayOfWeek().getValue() - 1);
        Reminder recurring = new Reminder("Work", daysBitmask, now.getHour(), now.getMinute(), null);
        TaskManager taskManager = taskManagerWithReminders(Arrays.asList(recurring));
        ChecklistCellRenderer renderer = new ChecklistCellRenderer(taskManager);

        Method method = ChecklistCellRenderer.class.getDeclaredMethod("nearestReminderForChecklist", String.class);
        method.setAccessible(true);

        Reminder resolved = (Reminder) method.invoke(renderer, "Work");
        assertEquals(recurring, resolved);
    }
}