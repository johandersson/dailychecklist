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

import java.awt.Component;
import java.util.List;
import java.util.Map;

/**
 * Context object carrying parameters required during a restore operation.
 */
public final class RestoreContext {
    public final Component parent;
    public final TaskManager taskManager;
    public final Runnable updateTasks;
    public final Map<String,String> checklistsCopy;
    public final List<Task> customTasks;
    public final List<Task> morningTasks;
    public final List<Task> eveningTasks;
    public final List<Task> currentTasks;

    public RestoreContext(Component parent, TaskManager taskManager, Runnable updateTasks, Map<String,String> checklistsCopy, List<Task> customTasks, List<Task> morningTasks, List<Task> eveningTasks, List<Task> currentTasks) {
        this.parent = parent;
        this.taskManager = taskManager;
        this.updateTasks = updateTasks;
        this.checklistsCopy = checklistsCopy;
        this.customTasks = customTasks;
        this.morningTasks = morningTasks;
        this.eveningTasks = eveningTasks;
        this.currentTasks = currentTasks;
    }
}
