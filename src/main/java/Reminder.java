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
    private final String taskId; // optional, may be null
    private final boolean recurring;
    // bit 0 = Monday, bit 6 = Sunday
    private final int daysBitmask;

    public Reminder(String checklistName, int year, int month, int day, int hour, int minute) {
        this(checklistName, year, month, day, hour, minute, null);
    }

    public Reminder(String checklistName, int year, int month, int day, int hour, int minute, String taskId) {
        this.checklistName = checklistName;
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.taskId = (taskId == null || taskId.trim().isEmpty()) ? null : taskId;
        this.recurring = false;
        this.daysBitmask = 0;
    }

    /**
     * Create a recurring reminder that repeats on the given days of week at hour:minute.
     * daysBitmask: bit0=Monday .. bit6=Sunday
     */
    public Reminder(String checklistName, int daysBitmask, int hour, int minute, String taskId) {
        this.checklistName = checklistName;
        this.year = 0;
        this.month = 0;
        this.day = 0;
        this.hour = hour;
        this.minute = minute;
        this.taskId = (taskId == null || taskId.trim().isEmpty()) ? null : taskId;
        this.recurring = daysBitmask != 0;
        this.daysBitmask = daysBitmask;
    }

    // Getters
    public String getChecklistName() { return checklistName; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public int getDay() { return day; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }
    public String getTaskId() { return taskId; }
    public boolean isRecurring() { return recurring; }
    public int getDaysBitmask() { return daysBitmask; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Reminder reminder = (Reminder) obj;
         return year == reminder.year && month == reminder.month && day == reminder.day &&
             hour == reminder.hour && minute == reminder.minute &&
             recurring == reminder.recurring &&
             daysBitmask == reminder.daysBitmask &&
             Objects.equals(checklistName, reminder.checklistName) &&
             Objects.equals(taskId, reminder.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checklistName, taskId, year, month, day, hour, minute, recurring, daysBitmask);
    }
}