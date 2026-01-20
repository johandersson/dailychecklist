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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class SettingsManager {
    private Properties settings = new Properties();
    private String settingsPath = ApplicationConfiguration.SETTINGS_FILE_PATH;
    private String lastDate;
    private Component parentComponent;

    /**
     * Creates a new SettingsManager with no parent component.
     * Error dialogs will not be shown.
     */
    public SettingsManager() {
        this(null);
    }

    /**
     * Creates a new SettingsManager with a parent component for error dialogs.
     *
     * @param parentComponent Parent component for error dialogs, or null to disable dialogs
     */
    public SettingsManager(Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    public void load() {
        ApplicationConfiguration.ensureDataDirectoryExists();
        
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(settingsPath), StandardCharsets.UTF_8)) {
            settings.load(reader);
        } catch (IOException e) {
            // File doesn't exist or can't be read, use defaults
            // Only show error dialog if file exists but can't be read (not for missing file on first run)
            if (new File(settingsPath).exists() && parentComponent != null) {
                ApplicationErrorHandler.showDataLoadError(parentComponent, "settings", e);
            }
        }
        lastDate = settings.getProperty("lastDate", "");
    }

    public void save(boolean showWeekdayTasks) {
        ApplicationConfiguration.ensureDataDirectoryExists();
        settings.setProperty("showWeekdayTasks", String.valueOf(showWeekdayTasks));
        settings.setProperty("lastDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(settingsPath), StandardCharsets.UTF_8)) {
            settings.store(writer, null);
        } catch (IOException e) {
            if (parentComponent != null) {
                ApplicationErrorHandler.showDataSaveError(parentComponent, "settings", e);
            }
        }
    }

    public boolean getShowWeekdayTasks() {
        return Boolean.parseBoolean(settings.getProperty("showWeekdayTasks", "false"));
    }

    public void checkDateAndUpdate(TaskManager taskManager, Runnable updateTasks, boolean showWeekdayTasks) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        if (!lastDate.isEmpty() && !currentDate.equals(lastDate)) {
            // reset all tasks to undone
            java.util.List<Task> allTasks = taskManager.getAllTasks();
            for (Task task : allTasks) {
                if (task.isDone()) {
                    task.setDone(false);
                    task.setDoneDate(null);
                    taskManager.updateTask(task);
                }
            }
            updateTasks.run();
            lastDate = currentDate;
            save(showWeekdayTasks);
        }
    }
}