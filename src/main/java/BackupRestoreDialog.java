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
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

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
        File chosen = chooseBackupFile(parent);
        if (chosen == null) return;

        // Load backup data (with progress dialog only for large files)
        Runnable loadTask = () -> {
            Map<String,String> checklists = readChecklistsFromZip(chosen);
            List<Task> backupTasks;
            try {
                backupTasks = loadBackupTasks(chosen);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> ErrorDialog.showError(parent, "Failed to load backup tasks", e));
                return;
            }
            if (backupTasks == null) {
                SwingUtilities.invokeLater(() -> ErrorDialog.showError(parent, "Failed to load backup tasks."));
                return;
            }

            // Continue on EDT with loaded data
            SwingUtilities.invokeLater(() -> showRestorePreview(parent, taskManager, updateTasks, chosen, checklists, backupTasks));
        };
        
        // Only show progress dialog for large files (> 100KB suggests many tasks)
        if (chosen.length() > 100 * 1024) {
            RestoreProgressDialog loadDlg = new RestoreProgressDialog(SwingUtilities.getWindowAncestor(parent), "Loading backup file");
            loadDlg.runTask(loadTask);
        } else {
            // Small file - load directly without progress dialog
            new Thread(loadTask, "BackupLoader").start();
        }
    }

    private static void showRestorePreview(Component parent, TaskManager taskManager, Runnable updateTasks, File chosen, Map<String,String> checklists, List<Task> backupTasks) {
        List<Task> morningTasks = backupTasks.stream().filter(t -> t.getType() == TaskType.MORNING).collect(Collectors.toList());
        List<Task> eveningTasks = backupTasks.stream().filter(t -> t.getType() == TaskType.EVENING).collect(Collectors.toList());
        List<Task> customTasks = backupTasks.stream().filter(t -> t.getType() == TaskType.CUSTOM).collect(Collectors.toList());

        List<Task> currentTasks = taskManager.getAllTasks();
        java.util.Set<String> existingIds = currentTasks.stream().map(Task::getId).collect(java.util.stream.Collectors.toSet());

        Map<String,Integer> checklistTaskCounts = computeChecklistTaskCounts(checklists, customTasks, existingIds);
        int newChecklistCount = computeNewChecklistCount(checklists);

        List<Task> backupTasksCopy = new ArrayList<>(backupTasks);
        Map<String,String> checklistsCopy = new LinkedHashMap<>(checklists);

        RestoreContext restoreCtx = new RestoreContext(parent, taskManager, updateTasks, checklistsCopy, customTasks, morningTasks, eveningTasks, currentTasks);
        boolean showProgress = backupTasks.size() > 150; // Only show progress for large backups
        Runnable onMerge = createOnRestoreRunnable(restoreCtx, showProgress);
        Runnable onReplace = createOnReplaceRunnable(restoreCtx, backupTasks, showProgress);

        int morningImportCount = (int) morningTasks.stream().filter(t -> !existingIds.contains(t.getId())).count();
        int eveningImportCount = (int) eveningTasks.stream().filter(t -> !existingIds.contains(t.getId())).count();
        RestorePreview preview = new RestorePreview(checklistsCopy, checklistTaskCounts, newChecklistCount, morningImportCount, eveningImportCount);
        RestorePreviewDialog.showDialog(parent, currentTasks, backupTasksCopy, chosen, onMerge, onReplace, preview);
    }

    private static File chooseBackupFile(Component parent) {
        JFileChooser chooser = new JFileChooser(ApplicationConfiguration.BACKUP_DIRECTORY);
        chooser.setDialogTitle("Select backup ZIP to restore from");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter zipFilter = new FileNameExtensionFilter("Daily Checklist backups (*.zip)", "zip");
        chooser.addChoosableFileFilter(zipFilter);
        chooser.setFileFilter(zipFilter);
        chooser.setAcceptAllFileFilterUsed(false);
        int res = chooser.showOpenDialog(parent);
        if (res != JFileChooser.APPROVE_OPTION) return null;
        File chosen = chooser.getSelectedFile();
        // Require .zip extension to make it explicit that backups must be ZIP files
        if (chosen == null || !chosen.getName().toLowerCase().endsWith(".zip")) {
            JOptionPane.showMessageDialog(parent,
                "Please select a .zip backup file.",
                "Select ZIP Backup", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(chosen)) {
            boolean hasTasks = zf.getEntry("tasks.xml") != null;
            boolean hasNames = zf.getEntry("checklist-names.properties") != null;
            if (!hasTasks && !hasNames) {
                JOptionPane.showMessageDialog(parent,
                    "The selected file does not look like a Daily Checklist backup (missing tasks.xml and checklist-names.properties).",
                    "Invalid Backup", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        } catch (java.util.zip.ZipException ze) {
            JOptionPane.showMessageDialog(parent,
                "The selected file is not a valid ZIP archive or is corrupted.",
                "Invalid ZIP File", JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (java.io.IOException ioe) {
            ApplicationErrorHandler.showBackupError(parent, ioe instanceof Exception ? (Exception)ioe : new Exception(ioe));
            return null;
        }
        return chosen;
    }

    private static Map<String,Integer> computeChecklistTaskCounts(Map<String,String> checklists, List<Task> customTasks, java.util.Set<String> existingIds) {
        Map<String,Integer> counts = new LinkedHashMap<>();
        for (String id : checklists.keySet()) counts.put(id, 0);
        for (Task t : customTasks) {
            String id = t.getChecklistId();
            if (id == null) continue;
            if (existingIds.contains(t.getId())) continue;
            counts.put(id, counts.getOrDefault(id, 0) + 1);
        }
        return counts;
    }

    private static int computeNewChecklistCount(Map<String,String> checklists) {
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
            int newChecklistCount = 0;
            for (String k : checklists.keySet()) if (!liveKeys.contains(k)) newChecklistCount++;
            return newChecklistCount;
        } catch (java.io.IOException ex) {
            return checklists.size();
        }
    }

    

    private static Runnable createOnRestoreRunnable(RestoreContext ctx, boolean showProgress) {
        return () -> {
            // Run restore in background
            File liveBackup = backupLiveData();
            Runnable work = () -> {
                try {
                    if (!ctx.checklistsCopy.isEmpty()) mergeChecklistsToLive(ctx.checklistsCopy);
                    List<Task> merged = new ArrayList<>(ctx.currentTasks);
                    java.util.Set<String> mergedIds = merged.stream().map(Task::getId).collect(Collectors.toSet());
                    for (Task t : ctx.customTasks) if (!mergedIds.contains(t.getId())) merged.add(t);
                    for (Task t : ctx.morningTasks) if (!mergedIds.contains(t.getId())) merged.add(t);
                    for (Task t : ctx.eveningTasks) if (!mergedIds.contains(t.getId())) merged.add(t);
                    ctx.taskManager.setTasks(merged);
                    // Update GUI (with progress dialog only for large imports)
                    Runnable guiUpdate = () -> ctx.updateTasks.run();
                    if (showProgress) {
                        RestoreProgressDialog guiProgress = new RestoreProgressDialog(SwingUtilities.getWindowAncestor(ctx.parent), "Updating display");
                        guiProgress.runTask(guiUpdate);
                    } else {
                        SwingUtilities.invokeLater(guiUpdate);
                    }
                } catch (RuntimeException e) {
                    SwingUtilities.invokeLater(() -> ErrorDialog.showError(ctx.parent, "Failed to import tasks", e));
                    return;
                }
            };
            if (showProgress) {
                RestoreProgressDialog dlg = new RestoreProgressDialog(SwingUtilities.getWindowAncestor(ctx.parent), "Importing backup");
                dlg.runTask(work);
            } else {
                new Thread(work, "RestoreWorker").start();
            }
        };
    }

    private static Runnable createOnReplaceRunnable(RestoreContext ctx, List<Task> backupTasks, boolean showProgress) {
        return () -> {
            // Run replace in background
            File liveBackup = backupLiveData();
            Runnable work = () -> {
                try {
                    if (!ctx.checklistsCopy.isEmpty()) mergeChecklistsToLive(ctx.checklistsCopy);
                    ctx.taskManager.setTasks(new ArrayList<>(backupTasks));
                    // Update GUI (with progress dialog only for large restores)
                    Runnable guiUpdate = () -> ctx.updateTasks.run();
                    if (showProgress) {
                        RestoreProgressDialog guiProgress = new RestoreProgressDialog(SwingUtilities.getWindowAncestor(ctx.parent), "Updating display");
                        guiProgress.runTask(guiUpdate);
                    } else {
                        SwingUtilities.invokeLater(guiUpdate);
                    }
                } catch (RuntimeException e) {
                    SwingUtilities.invokeLater(() -> ErrorDialog.showError(ctx.parent, "Failed to restore tasks", e));
                    return;
                }
            };
            if (showProgress) {
                RestoreProgressDialog dlg = new RestoreProgressDialog(SwingUtilities.getWindowAncestor(ctx.parent), "Restoring backup");
                dlg.runTask(work);
            } else {
                new Thread(work, "ReplaceWorker").start();
            }
        };
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
            // Log merge activity for diagnostics (UTF-8)
            try {
                java.nio.file.Path logPath = java.nio.file.Paths.get(ApplicationConfiguration.APPLICATION_DATA_DIR, "restore-merge.log");
                java.nio.file.Files.createDirectories(logPath.getParent());
                try (java.io.OutputStreamWriter lw = new java.io.OutputStreamWriter(new java.io.FileOutputStream(logPath.toFile(), true), StandardCharsets.UTF_8)) {
                    lw.write("=== mergeChecklistsToLive at " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "\n");
                    lw.write("Incoming checklist entries (from backup):\n");
                    for (Map.Entry<String,String> e : checklists.entrySet()) {
                        lw.write(e.getKey() + "=" + e.getValue() + "\n");
                    }
                    lw.write("Live registry before merge:\n");
                    for (String k : live.stringPropertyNames()) {
                        lw.write(k + "=" + live.getProperty(k) + "\n");
                    }
                    lw.write("---\n");
                    lw.flush();
                }
            } catch (Exception logEx) {
                java.util.logging.Logger.getLogger(BackupRestoreDialog.class.getName()).log(java.util.logging.Level.WARNING, "Failed to write restore merge log", logEx);
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
                // Append resulting registry to the merge log
                try {
                    java.nio.file.Path logPath = java.nio.file.Paths.get(ApplicationConfiguration.APPLICATION_DATA_DIR, "restore-merge.log");
                    try (java.io.OutputStreamWriter lw = new java.io.OutputStreamWriter(new java.io.FileOutputStream(logPath.toFile(), true), StandardCharsets.UTF_8)) {
                        lw.write("Live registry after merge:\n");
                        for (String k : live.stringPropertyNames()) {
                            lw.write(k + "=" + live.getProperty(k) + "\n");
                        }
                        lw.write("=== end merge\n\n");
                        lw.flush();
                    }
                } catch (Exception logEx) {
                    java.util.logging.Logger.getLogger(BackupRestoreDialog.class.getName()).log(java.util.logging.Level.WARNING, "Failed to append restore merge log", logEx);
                }
            }
        } catch (java.io.IOException ex) {
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
        } catch (java.io.IOException e) {
            // ignore and return empty map
        }
        return map;
    }

    // chooseChecklistsDialog removed: not used anywhere in codebase

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
        } catch (java.io.IOException e) {
            return null;
        }
    }

    // findLatestBackup removed: unused helper

    private static List<Task> loadBackupTasks(File backupFile) throws Exception {
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
        }
    }

    

    // escapeHtml removed: not used
}