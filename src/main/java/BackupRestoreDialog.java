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

        // Show diff dialog with confirmation
        showDiffDialog(parent, currentTasks, backupTasks, latestBackup, () -> {
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

    private static void showDiffDialog(Component parent, List<Task> currentTasks, List<Task> backupTasks, File backupFile, Runnable onRestore) {
        JDialog dialog = new JDialog((java.awt.Frame) parent, "Restore from Backup", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(900, 700);
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Header panel with warning
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 10, 20));
        headerPanel.setBackground(new java.awt.Color(255, 243, 224)); // Light orange background

        String headerHtml = "<html><body style='font-family: Arial, sans-serif; font-size: 14px;'>" +
            "<div style='text-align: center; margin-bottom: 15px;'>" +
            "<h2 style='color: #d9534f; margin: 0 0 10px 0; font-size: 18px;'>⚠️ Restore from Backup</h2>" +
            "<div style='color: #666; font-size: 12px; margin-bottom: 10px;'>Backup file: <b>" + backupFile.getName() + "</b></div>" +
            "</div>" +
            "<div style='background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 5px; padding: 15px; margin: 10px 0;'>" +
            "<div style='color: #856404; font-weight: bold; margin-bottom: 8px;'>⚠️ Warning: This will overwrite your current data!</div>" +
            "<div style='color: #856404; font-size: 12px;'>Review the differences below and confirm if you want to proceed with the restoration.</div>" +
            "</div>" +
            "</body></html>";

        javax.swing.JLabel headerLabel = new javax.swing.JLabel(headerHtml);
        headerPanel.add(headerLabel, BorderLayout.CENTER);
        dialog.add(headerPanel, BorderLayout.NORTH);

        // Diff panel
        TaskDiffPanel diffPanel = new TaskDiffPanel(currentTasks, backupTasks);
        dialog.add(new javax.swing.JScrollPane(diffPanel), BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 20, 10));
        buttonPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton restoreButton = new JButton("Restore from Backup");
        restoreButton.setFont(restoreButton.getFont().deriveFont(java.awt.Font.BOLD, 12.0f));
        restoreButton.setBackground(new java.awt.Color(220, 53, 69));
        restoreButton.setForeground(java.awt.Color.WHITE);
        restoreButton.setFocusPainted(false);
        restoreButton.setPreferredSize(new java.awt.Dimension(160, 35));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(cancelButton.getFont().deriveFont(12.0f));
        cancelButton.setFocusPainted(false);
        cancelButton.setPreferredSize(new java.awt.Dimension(100, 35));

        restoreButton.addActionListener(e -> {
            dialog.dispose();
            onRestore.run();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(restoreButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}