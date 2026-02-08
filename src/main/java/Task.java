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
 */import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Task {
    private String doneDate;
    private String id;
    private String name;
    private TaskType type;
    private String weekday;
    private boolean done;
    private String checklistId; // Changed from checklistName to checklistId
    private String note; // Optional note for task/subtask (max 1000 words)

    // Subtask support
    private String parentId; // null if not a subtask
    private List<Task> subtasks = new ArrayList<>(); // Only one level of subtasks

    // Transient, runtime-only caches to avoid repeated work during painting
    transient String cachedDisplayFullName;
    transient int[] cachedCumulativeCharWidthsMain;
    
    // Dirty flags for caching optimization
    transient boolean displayDirty = true;
    
    // Lazy date parsing cache
    transient java.util.Date cachedParsedDoneDate;

    // Constructor when loading from file (ID provided)
    public Task(String id, String name, TaskType type, String weekday, boolean done, String doneDate, String checklistId, String parentId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.weekday = weekday;
        this.done = done;
        this.doneDate = doneDate;
        this.checklistId = checklistId != null ? checklistId.trim() : null;
        this.parentId = parentId;
    }

    // Constructor when creating a new task (ID generated)

    public Task(String name, TaskType type, String weekday, String checklistId) {
        this(name, type, weekday, checklistId, null);
    }

    public Task(String name, TaskType type, String weekday, String checklistId, String parentId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.type = type;
        this.weekday = weekday;
        this.done = false;
        this.doneDate = null;
        this.checklistId = checklistId != null ? checklistId.trim() : null;
        this.parentId = parentId;
    }


    // Constructor for backwards compatibility (checklistName = null)
    public Task(String name, TaskType type, String weekday) {
        this(name, type, weekday, null, null);
    }


    // Constructor for loading from file without checklistName (backwards compatibility)
    public Task(String id, String name, TaskType type, String weekday, boolean done, String doneDate) {
        this(id, name, type, weekday, done, doneDate, null, null);
    }
    // Subtask support
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<Task> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<Task> subtasks) {
        this.subtasks = subtasks;
    }

    public boolean hasSubtasks() {
        return subtasks != null && !subtasks.isEmpty();
    }

    public boolean areAllSubtasksDone() {
        if (!hasSubtasks()) return false;
        for (Task t : subtasks) {
            if (!t.isDone()) return false;
        }
        return true;
    }

    public void markAllSubtasksDone(boolean done) {
        if (hasSubtasks()) {
            for (Task t : subtasks) {
                t.setDone(done);
            }
        }
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
        this.displayDirty = true; // Mark dirty when name changes
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
                + ", weekday=" + weekday + ", done=" + done + ", checklistId=" + checklistId + "]";
    }

    public String getChecklistId() {
        return checklistId;
    }

    public void setChecklistId(String checklistId) {
        this.checklistId = checklistId != null ? checklistId.trim() : null;
        this.displayDirty = true; // Mark dirty when checklist changes
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
        // Invalidate cached parsed date
        this.cachedParsedDoneDate = null;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
        this.displayDirty = true;
    }

    public boolean hasNote() {
        return note != null && !note.trim().isEmpty();
    }
    
    // Cache management methods
    public boolean isDisplayDirty() {
        return displayDirty;
    }
    
    public void markDisplayClean() {
        this.displayDirty = false;
    }
    
    public void markDisplayDirty() {
        this.displayDirty = true;
    }
    
    /**
     * Get the parsed done date, parsing lazily only when needed.
     * Returns null if doneDate is null or cannot be parsed.
     */
    public java.util.Date getParsedDoneDate() {
        if (doneDate == null) return null;
        if (cachedParsedDoneDate != null) return cachedParsedDoneDate;
        
        try {
            cachedParsedDoneDate = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(doneDate);
            return cachedParsedDoneDate;
        } catch (java.text.ParseException e) {
            return null;
        }
    }
}

