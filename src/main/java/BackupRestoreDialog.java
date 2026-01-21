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
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Dialog for restoring data from backup with confirmation and diff display.
 */
public class BackupRestoreDialog {
    private BackupRestoreDialog() {} // Utility class

    /**
     * Shows the restore from backup dialog.
     */
    public static void showRestoreDialog(Component parent, TaskManager taskManager, Runnable updateTasks) {
        // Find the latest backup
        File latestBackup = findLatestBackup();
        if (latestBackup == null) {
            JOptionPane.showMessageDialog(parent,
                "No backup files found.",
                "No Backup Available",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show warning dialog
        int confirm = JOptionPane.showConfirmDialog(parent,
            "Restoring from backup will overwrite your current data.\n" +
            "This action cannot be undone.\n\n" +
            "Backup file: " + latestBackup.getName() + "\n\n" +
            "Do you want to proceed?",
            "Confirm Restore",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Load backup data
        List<Task> backupTasks = loadBackupTasks(latestBackup);
        if (backupTasks == null) {
            JOptionPane.showMessageDialog(parent,
                "Failed to load backup data.",
                "Load Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Load current data
        List<Task> currentTasks = taskManager.getAllTasks();

        // Show diff dialog
        showDiffDialog(parent, currentTasks, backupTasks, () -> {
            // Perform restore
            try {
                taskManager.setTasks(backupTasks);
                updateTasks.run();
                JOptionPane.showMessageDialog(parent,
                    "Data restored successfully from backup.",
                    "Restore Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent,
                    "Failed to restore data: " + e.getMessage(),
                    "Restore Failed",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static File findLatestBackup() {
        File backupDir = new File("backups");
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
        try {
            java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(backupFile);
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
                    zipFile.close();
                    return tasks;
                }
            }
            zipFile.close();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void showDiffDialog(Component parent, List<Task> currentTasks, List<Task> backupTasks, Runnable onRestore) {
        JDialog dialog = new JDialog((java.awt.Frame) parent, "Backup Differences", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(parent);

        // Diff panel
        TaskDiffPanel diffPanel = new TaskDiffPanel(currentTasks, backupTasks);
        dialog.add(new javax.swing.JScrollPane(diffPanel), BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton restoreButton = new JButton("Restore");
        JButton cancelButton = new JButton("Cancel");

        restoreButton.addActionListener(e -> {
            dialog.dispose();
            onRestore.run();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(restoreButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}