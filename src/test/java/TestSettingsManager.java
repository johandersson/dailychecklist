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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestSettingsManager {

    @Test
    public void testSaveAndLoad() throws Exception {
        Path tempDir = Files.createTempDirectory("settingsTest");
        Path tempFile = tempDir.resolve("settings.ini");
        SettingsManager manager = new SettingsManager();
        // Use reflection to set the path
        java.lang.reflect.Field field = SettingsManager.class.getDeclaredField("settingsPath");
        field.setAccessible(true);
        field.set(manager, tempFile.toString());
        
        // Test save true
        manager.save(true);
        // Load in new instance
        SettingsManager manager2 = new SettingsManager();
        field.set(manager2, tempFile.toString());
        manager2.load();
        assertTrue(manager2.getShowWeekdayTasks());
        
        // Test save false
        manager.save(false);
        SettingsManager manager3 = new SettingsManager();
        field.set(manager3, tempFile.toString());
        manager3.load();
        assertFalse(manager3.getShowWeekdayTasks());
        
        // Cleanup
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    public void testGetShowWeekdayTasks() {
        SettingsManager manager = new SettingsManager();
        // Default false
        assertFalse(manager.getShowWeekdayTasks());
    }

    @Test
    public void testCheckDateAndUpdate() throws Exception {
        Path tempDir = Files.createTempDirectory("settingsTest");
        Path tempFile = tempDir.resolve("settings.ini");
        SettingsManager manager = new SettingsManager();
        java.lang.reflect.Field field = SettingsManager.class.getDeclaredField("settingsPath");
        field.setAccessible(true);
        field.set(manager, tempFile.toString());
        
        // Mock TaskManager
        TaskManager taskManager = new TaskManager(new XMLTaskRepository());
        boolean[] updated = {false};
        Runnable updateTasks = () -> updated[0] = true;
        
        // Set lastDate to yesterday
        java.lang.reflect.Field lastDateField = SettingsManager.class.getDeclaredField("lastDate");
        lastDateField.setAccessible(true);
        lastDateField.set(manager, "2025-11-18"); // yesterday
        
        manager.checkDateAndUpdate(taskManager, updateTasks, true);
        
        // Should have updated
        assertTrue(updated[0]);
        
        // Cleanup
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(tempDir);
    }
}