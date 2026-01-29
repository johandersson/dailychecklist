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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Manages custom checklist persistence.
 */
public class ChecklistNameManager {
    private final String checklistNamesFileName;
    private Map<String, Checklist> cachedChecklists;
    private boolean checklistsDirty = true;

    public ChecklistNameManager(String checklistNamesFileName) {
        this.checklistNamesFileName = checklistNamesFileName;
    }

    /**
     * Gets all checklists, using cache if available.
     */
    public Set<Checklist> getChecklists() {
        if (cachedChecklists != null && !checklistsDirty) {
            return new HashSet<>(cachedChecklists.values());
        }

        Map<String, Checklist> checklists = new HashMap<>();
        Properties props = new Properties();

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(checklistNamesFileName), StandardCharsets.UTF_8)) {
            props.load(reader);
            for (String id : props.stringPropertyNames()) {
                String name = props.getProperty(id);
                if (name != null && !name.trim().isEmpty()) {
                    checklists.put(id, new Checklist(id, name.trim()));
                }
            }
        } catch (IOException e) {
            // File doesn't exist or can't be read, return empty set
        }

        cachedChecklists = new HashMap<>(checklists);
        checklistsDirty = false;
        return new HashSet<>(checklists.values());
    }

    /**
     * Gets a checklist by ID.
     */
    public Checklist getChecklistById(String id) {
        if (id == null) return null;
        Set<Checklist> checklists = getChecklists();
        return checklists.stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Convenience: get the checklist name by id, or null if not found.
     */
    public String getNameById(String id) {
        Checklist c = getChecklistById(id);
        return c == null ? null : c.getName();
    }

    /**
     * Gets a checklist by name.
     */
    public Checklist getChecklistByName(String name) {
        if (name == null) return null;
        Set<Checklist> checklists = getChecklists();
        return checklists.stream()
                .filter(c -> name.trim().equals(c.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds a checklist.
     */
    public void addChecklist(Checklist checklist) {
        if (checklist == null) return;
        Map<String, Checklist> checklists = getChecklistsMap();
        checklists.put(checklist.getId(), checklist);
        saveChecklistsToProperties(checklists);
        cachedChecklists = checklists;
        checklistsDirty = false;
    }

    /**
     * Removes a checklist.
     */
    public void removeChecklist(Checklist checklist) {
        if (checklist == null) return;
        Map<String, Checklist> checklists = getChecklistsMap();
        checklists.remove(checklist.getId());
        saveChecklistsToProperties(checklists);
        cachedChecklists = checklists;
        checklistsDirty = false;
    }

    /**
     * Updates a checklist name.
     */
    public void updateChecklistName(Checklist checklist, String newName) {
        if (checklist == null || newName == null) return;
        checklist.setName(newName.trim());
        Map<String, Checklist> checklists = getChecklistsMap();
        checklists.put(checklist.getId(), checklist);
        saveChecklistsToProperties(checklists);
        cachedChecklists = checklists;
        checklistsDirty = false;
    }

    /**
     * Gets checklists as a map for internal use.
     */
    private Map<String, Checklist> getChecklistsMap() {
        if (cachedChecklists != null && !checklistsDirty) {
            return new HashMap<>(cachedChecklists);
        }
        getChecklists(); // This will populate cachedChecklists
        return new HashMap<>(cachedChecklists);
    }

    /**
     * Saves checklists to the properties file.
     */
    private void saveChecklistsToProperties(Map<String, Checklist> checklists) {
        Properties props = new Properties();
        for (Checklist checklist : checklists.values()) {
            props.setProperty(checklist.getId(), checklist.getName());
        }

        java.nio.file.Path target = java.nio.file.Paths.get(checklistNamesFileName);
        java.nio.file.Path parent = target.toAbsolutePath().getParent();
        try {
            if (parent != null) java.nio.file.Files.createDirectories(parent);
        } catch (IOException e) {
            java.util.logging.Logger.getLogger(ChecklistNameManager.class.getName())
                    .log(java.util.logging.Level.WARNING, "Failed creating parent directory for checklist names file: " + parent, e);
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(target.toFile()), StandardCharsets.UTF_8)) {
            props.store(writer, "Daily Checklist Custom Checklists");
        } catch (IOException e) {
            java.util.logging.Logger.getLogger(ChecklistNameManager.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Failed to write checklist names file: " + checklistNamesFileName, e);
        }
    }

    /**
     * Marks the checklists cache as dirty.
     */
    public void markDirty() {
        checklistsDirty = true;
    }
}