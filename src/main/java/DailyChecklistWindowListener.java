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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Supplier;

public class DailyChecklistWindowListener extends WindowAdapter {
    private final SettingsManager settingsManager;
    private final TaskManager checklistManager;
    private final Runnable updateTasks;
    private final Supplier<Boolean> isShowWeekdayTasks;

    public DailyChecklistWindowListener(SettingsManager settingsManager, TaskManager checklistManager, Runnable updateTasks, Supplier<Boolean> isShowWeekdayTasks) {
        this.settingsManager = settingsManager;
        this.checklistManager = checklistManager;
        this.updateTasks = updateTasks;
        this.isShowWeekdayTasks = isShowWeekdayTasks;
    }

    @Override
    public void windowActivated(WindowEvent e) {
        settingsManager.checkDateAndUpdate(checklistManager, updateTasks, isShowWeekdayTasks.get());
    }
}