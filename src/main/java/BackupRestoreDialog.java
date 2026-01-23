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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * Dialog for restoring data from backup with confirmation and diff display.
 */
public class BackupRestoreDialog {
    private BackupRestoreDialog() {} // Utility class

    // (RestorePreview moved to its own top-level class RestorePreview.java)

    /**
     * Shows the restore from backup dialog.
     */
    public static void showRestoreDialog(Component parent, TaskManager taskManager, Runnable updateTasks) {
        // Let the user pick a backup ZIP file instead of auto-selecting the latest.
        JFileChooser chooser = new JFileChooser(ApplicationConfiguration.BACKUP_DIRECTORY);
        chooser.setDialogTitle("Select backup ZIP to restore from");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int res = chooser.showOpenDialog(parent);
        if (res != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosen = chooser.getSelectedFile();

        // Read available checklists from the selected ZIP
        Map<String,String> checklists = readChecklistsFromZip(chosen);

        // Load all tasks from the ZIP and filter for MORNING and EVENING tasks
        List<Task> backupTasks = loadBackupTasks(chosen);
        if (backupTasks == null) {
            ErrorDialog.showError(parent, "Failed to load backup tasks.");
            return;
        }

        // Compute task breakdown from the backup
        List<Task> morningTasks = backupTasks.stream().filter(t -> t.getType() == TaskType.MORNING).collect(Collectors.toList());
        List<Task> eveningTasks = backupTasks.stream().filter(t -> t.getType() == TaskType.EVENING).collect(Collectors.toList());
        List<Task> customTasks = backupTasks.stream().filter(t -> t.getType() == TaskType.CUSTOM).collect(Collectors.toList());

        // Compute how many checklists would be added
        int newChecklistCount = 0;
        try {
            File liveFile = new File(ApplicationConfiguration.CHECKLIST_NAMES_FILE_PATH);
            java.util.Set<String> liveKeys = new java.util.HashSet<>();
            if (liveFile.exists()) {
                Properties p = new Properties();
                try (InputStreamReader r = new InputStreamReader(new java.io.FileInputStream(liveFile), StandardCharsets.UTF_8)) {
                    p.load(r);
                }
                for (String k : p.stringPropertyNames()) liveKeys.add(k);
            }
            for (String k : checklists.keySet()) if (!liveKeys.contains(k)) newChecklistCount++;
        } catch (Exception ex) {
            newChecklistCount = checklists.size();
        }

        // Prepare current and backup task lists and compute per-checklist task counts for stats.
        List<Task> currentTasks = taskManager.getAllTasks();
        java.util.Set<String> existingIds = currentTasks.stream().map(Task::getId).collect(java.util.stream.Collectors.toSet());
        List<Task> backupTasksCopy = new ArrayList<>(backupTasks);
        Map<String,String> checklistsCopy = new LinkedHashMap<>(checklists);

        // Count custom tasks per checklist id (preserve insertion order from checklists)
        // Only count tasks that would actually be imported (i.e., whose UUID is not already present)
        Map<String,Integer> checklistTaskCounts = new LinkedHashMap<>();
        for (String id : checklistsCopy.keySet()) checklistTaskCounts.put(id, 0);
        for (Task t : customTasks) {
            String id = t.getChecklistId();
            if (id == null) continue;
            if (existingIds.contains(t.getId())) continue; // skip tasks already present
            checklistTaskCounts.put(id, checklistTaskCounts.getOrDefault(id, 0) + 1);
        }

        Runnable onRestore = () -> {
            // Backup current data file before merging
            File liveBackup = backupLiveData();

            // Merge all checklist names from backup into live properties
            if (!checklistsCopy.isEmpty()) mergeChecklistsToLive(checklistsCopy);

            // Merge imported tasks into current tasks (avoid duplicates)
            List<Task> merged = new ArrayList<>(currentTasks);
            java.util.Set<String> mergedIds = merged.stream().map(Task::getId).collect(Collectors.toSet());
            for (Task t : customTasks) {
                if (!mergedIds.contains(t.getId())) merged.add(t);
            }
            for (Task t : morningTasks) {
                if (!mergedIds.contains(t.getId())) merged.add(t);
            }
            for (Task t : eveningTasks) {
                if (!mergedIds.contains(t.getId())) merged.add(t);
            }

            // Apply merged tasks
            try {
                taskManager.setTasks(merged);
                updateTasks.run();
                JOptionPane.showMessageDialog(parent, "Imported tasks and merged checklists." + (liveBackup!=null?" (backup created)":""), "Import Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                ErrorDialog.showError(parent, "Failed to import tasks", e);
            }
        };

        // Build preview object and show the diff/preview dialog (single confirmation)
        // Morning/evening counts should also only count tasks that would be imported (skip existing UUIDs)
        int morningImportCount = (int) morningTasks.stream().filter(t -> !existingIds.contains(t.getId())).count();
        int eveningImportCount = (int) eveningTasks.stream().filter(t -> !existingIds.contains(t.getId())).count();
        RestorePreview preview = new RestorePreview(checklistsCopy, checklistTaskCounts, newChecklistCount, morningImportCount, eveningImportCount);
        // Show the diff/preview dialog (delegates to RestorePreviewDialog)
        RestorePreviewDialog.showDialog(parent, currentTasks, backupTasksCopy, chosen, onRestore, preview);
    }

    private static void mergeChecklistsToLive(Map<String,String> checklists) {
        try {
            File f = new File(ApplicationConfiguration.CHECKLIST_NAMES_FILE_PATH);
            Properties live = new Properties();
            if (f.exists()) {
                try (InputStreamReader r = new InputStreamReader(new java.io.FileInputStream(f), StandardCharsets.UTF_8)) {
                    live.load(r);
                }
            } else {
                // ensure directory exists
                new File(ApplicationConfiguration.APPLICATION_DATA_DIR).mkdirs();
            }
            boolean changed = false;
            for (Map.Entry<String,String> e : checklists.entrySet()) {
                if (!live.containsKey(e.getKey())) {
                    live.setProperty(e.getKey(), e.getValue());
                    changed = true;
                }
            }
            if (changed) {
                try (java.io.OutputStreamWriter w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(f), StandardCharsets.UTF_8)) {
                    live.store(w, "Merged from backup on " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
                }
            }
        } catch (Exception ex) {
            // ignore merge failures
        }
    }

    private static Map<String,String> readChecklistsFromZip(File backupFile) {
        Map<String,String> map = new LinkedHashMap<>();
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(backupFile)) {
            java.util.zip.ZipEntry entry = zipFile.getEntry("checklist-names.properties");
            if (entry == null) return map;
            try (InputStream is = zipFile.getInputStream(entry);
                 InputStreamReader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Properties p = new Properties();
                p.load(r);
                for (String key : p.stringPropertyNames()) {
                    map.put(key, p.getProperty(key));
                }
            }
        } catch (Exception e) {
            // ignore and return empty map
        }
        return map;
    }

    private static List<String> chooseChecklistsDialog(Component parent, Map<String,String> checklists) {
        List<String> ids = new ArrayList<>();
        String[] display = new String[checklists.size()];
        int idx = 0;
        for (Map.Entry<String,String> e : checklists.entrySet()) {
            display[idx++] = e.getValue() + " (" + e.getKey() + ")";
        }
        JList<String> list = new JList<>(display);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scroll = new JScrollPane(list);
        int res = JOptionPane.showConfirmDialog(parent, scroll, "Select checklists to import", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return ids;
        int[] sel = list.getSelectedIndices();
        if (sel == null || sel.length == 0) return ids;
        String[] keys = checklists.keySet().toArray(String[]::new);
        for (int i : sel) {
            if (i >= 0 && i < keys.length) ids.add(keys[i]);
        }
        return ids;
    }

    private static File backupLiveData() {
        try {
            File live = new File(ApplicationConfiguration.getDataFilePath());
            if (!live.exists()) return null;
            String stamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
            File target = new File(ApplicationConfiguration.APPLICATION_DATA_DIR + File.separator + "tasks.xml.pre-restore-" + stamp + ".bak");
            try (java.io.InputStream is = new java.io.FileInputStream(live);
                 java.io.OutputStream os = new java.io.FileOutputStream(target)) {
                byte[] buf = new byte[4096];
                int r;
                while ((r = is.read(buf)) > 0) os.write(buf, 0, r);
            }
            return target;
        } catch (Exception e) {
            return null;
        }
    }

    private static File findLatestBackup() {
        File backupDir = new File(ApplicationConfiguration.BACKUP_DIRECTORY);
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return null;
        }

        File[] files = backupDir.listFiles((dir, name) -> name.startsWith("dailychecklist-backup-") && name.endsWith(".zip"));
        if (files == null || files.length == 0) {
            return null;
        }

        File latest = null;
        long latestTime = 0;
        for (File file : files) {
            if (file.lastModified() > latestTime) {
                latest = file;
                latestTime = file.lastModified();
            }
        }
        return latest;
    }

    private static List<Task> loadBackupTasks(File backupFile) {
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(backupFile)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                if (entry.getName().equals("tasks.xml")) {
                    // Create a temporary file for the XML
                    File tempFile = File.createTempFile("backup_tasks", ".xml");
                    tempFile.deleteOnExit();
                    try (java.io.InputStream is = zipFile.getInputStream(entry);
                         java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    // Parse the XML
                    TaskXmlHandler handler = new TaskXmlHandler(tempFile.getAbsolutePath());
                    List<Task> tasks = handler.parseAllTasks();
                    return tasks;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}