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
               checklistName.equals(reminder.checklistName);
    }

    @Override
    public int hashCode() {
        return checklistName.hashCode() + year + month + day + hour + minute;
    }
}