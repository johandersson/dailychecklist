import java.util.Objects;

/**
 * A reminder for a checklist at a specific date and time.
 */
public class Reminder {
    private final String checklistName;
    private final int year;
    private final int month;
    private final int day;
    private final int hour;
    private final int minute;

    public Reminder(String checklistName, int year, int month, int day, int hour, int minute) {
        this.checklistName = checklistName;
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
    }

    // Getters
    public String getChecklistName() { return checklistName; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public int getDay() { return day; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Reminder reminder = (Reminder) obj;
        return year == reminder.year && month == reminder.month && day == reminder.day &&
               hour == reminder.hour && minute == reminder.minute &&
               Objects.equals(checklistName, reminder.checklistName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(checklistName) + year + month + day + hour + minute;
    }
}