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
 */import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class Task {
    private String doneDate;
    private String id;
    private String name;
    private TaskType type;
    private String weekday;
    private boolean done;
    private String checklistName;
    // Constructor when loading from file (ID provided)
    public Task(String id, String name, TaskType type, String weekday, boolean done, String doneDate, String checklistName) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.weekday = weekday;
        this.done = done;
        this.doneDate = doneDate;
        this.checklistName = checklistName != null ? checklistName.trim() : null;
    }

    // Constructor when creating a new task (ID generated)
    public Task(String name, TaskType type, String weekday, String checklistName) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.type = type;
        this.weekday = weekday;
        this.done = false;
        this.doneDate = null;
        this.checklistName = checklistName != null ? checklistName.trim() : null;
    }

    // Constructor for backwards compatibility (checklistName = null)
    public Task(String name, TaskType type, String weekday) {
        this(name, type, weekday, null);
    }

    // Constructor for loading from file without checklistName (backwards compatibility)
    public Task(String id, String name, TaskType type, String weekday, boolean done, String doneDate) {
        this(id, name, type, weekday, done, doneDate, null);
    }



    // Getters and setters
    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public TaskType getType() {
        return type;
    }
    public void setType(TaskType type) {
        this.type = type;
    }
    public String getWeekday() {
        return weekday;
    }
    public void setWeekday(String weekday) {
        this.weekday = weekday;
    }
    public boolean isDone() {
        return done;
    }
    public void setDone(boolean done) {
        this.done = done;
    }

    // Compare Tasks by their unique id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Task other = (Task) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Task [id=" + id + ", name=" + name + ", type=" + type
                + ", weekday=" + weekday + ", done=" + done + ", checklistName=" + checklistName + "]";
    }

    public String getChecklistName() {
        return checklistName;
    }
    public void setChecklistName(String checklistName) {
        this.checklistName = checklistName;
    }

    public String getDoneDate() {
        return doneDate;
    }

    public void setDoneDate(Date doneDate) {
        //set on the format 2023-10-01
        if (doneDate != null) {
            //set on the format 2023-10-01
            this.doneDate = String.format("%tY-%<tm-%<td", doneDate);
        } else {
            this.doneDate = null;
        }
    }
}

