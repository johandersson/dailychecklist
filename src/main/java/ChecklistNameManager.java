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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Manages custom checklist names persistence.
 */
public class ChecklistNameManager {
    private final String checklistNamesFileName;
    private Set<String> cachedChecklistNames;
    private boolean checklistNamesDirty = true;

    public ChecklistNameManager(String checklistNamesFileName) {
        this.checklistNamesFileName = checklistNamesFileName;
    }

    /**
     * Gets all checklist names, using cache if available.
     */
    public Set<String> getChecklistNames() {
        if (cachedChecklistNames != null && !checklistNamesDirty) {
            return new HashSet<>(cachedChecklistNames);
        }

        Set<String> checklistNames = new HashSet<>();
        Properties props = new Properties();

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(checklistNamesFileName), StandardCharsets.UTF_8)) {
            props.load(reader);
            for (String key : props.stringPropertyNames()) {
                checklistNames.add(key);
            }
        } catch (IOException e) {
            // File doesn't exist or can't be read, return empty set
        }

        cachedChecklistNames = new HashSet<>(checklistNames);
        checklistNamesDirty = false;
        return checklistNames;
    }

    /**
     * Adds a checklist name.
     */
    public void addChecklistName(String name) {
        Set<String> names = getChecklistNames();
        names.add(name);
        saveChecklistNamesToProperties(names);
        cachedChecklistNames = names;
        checklistNamesDirty = false;
    }

    /**
     * Removes a checklist name.
     */
    public void removeChecklistName(String name) {
        Set<String> names = getChecklistNames();
        names.remove(name);
        saveChecklistNamesToProperties(names);
        cachedChecklistNames = names;
        checklistNamesDirty = false;
    }

    /**
     * Saves checklist names to the properties file.
     */
    private void saveChecklistNamesToProperties(Set<String> checklistNames) {
        Properties props = new Properties();
        for (String name : checklistNames) {
            props.setProperty(name, "true");
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(checklistNamesFileName), StandardCharsets.UTF_8)) {
            props.store(writer, "Daily Checklist Custom Checklist Names");
        } catch (IOException e) {
            // Ignore errors
        }
    }

    /**
     * Marks the checklist names cache as dirty.
     */
    public void markDirty() {
        checklistNamesDirty = true;
    }
}