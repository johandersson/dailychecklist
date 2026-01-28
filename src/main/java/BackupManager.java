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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Manages automatic and manual backups of application data files.
 * Handles backup creation, rotation, and cleanup.
 */
public class BackupManager {
    private final String backupDir;
    private final int maxBackups;
    private final long backupIntervalMs;
    private final String[] dataFiles; // Array of file paths to backup
    private final Component parentComponent; // Parent component for error dialogs

    private java.util.concurrent.ScheduledExecutorService scheduler;
    private volatile boolean backupRunning = false;

    /**
     * Creates a new BackupManager.
     *
     * @param backupDir Directory to store backup files
     * @param maxBackups Maximum number of backups to keep
     * @param backupIntervalMs Interval between automatic backups in milliseconds
     * @param dataFiles Array of file paths to include in backups
     * @param parentComponent Parent component for error dialogs
     */
    public BackupManager(String backupDir, int maxBackups, long backupIntervalMs, String[] dataFiles, Component parentComponent) {
        this.backupDir = backupDir;
        this.maxBackups = maxBackups;
        this.backupIntervalMs = backupIntervalMs;
        this.dataFiles = dataFiles;
        this.parentComponent = parentComponent;
    }

    /**
     * Initializes the backup system without starting background threads.
     * Call start() to begin automatic backups.
     */
    public void initialize() {
        // Create backup directory if it doesn't exist
        File backupDirFile = new File(backupDir);
        if (!backupDirFile.exists()) {
            backupDirFile.mkdirs();
        }
    }

    /**
     * Starts the periodic backup thread.
     * Should be called after initialization and when the application is ready.
     */
    public void start() {
        if (backupRunning) return;
        backupRunning = true;
        scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "backup-manager");
            t.setDaemon(true);
            return t;
        });

        // Schedule periodic backups at fixed rate
        scheduler.scheduleAtFixedRate(() -> {
            try {
                createPeriodicBackup();
            } catch (Exception e) {
                ApplicationErrorHandler.showBackupError(parentComponent, e);
            }
        }, backupIntervalMs, backupIntervalMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a periodic backup if enough time has passed since the last backup.
     */
    private void createPeriodicBackup() {
        createBackup("periodic");
    }

    /**
     * Creates a backup with the specified reason.
     *
     * @param reason Reason for the backup (e.g., "periodic", "manual", "save")
     */
    public void createBackup(String reason) {
        try {
            // Create backup directory if needed
            File backupDirFile = new File(backupDir);
            if (!backupDirFile.exists()) {
                backupDirFile.mkdirs();
            }

            // Generate timestamp for backup filename
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = sdf.format(new java.util.Date());

            // Calculate checksum of data files for corruption detection
            String checksum = calculateDataChecksum();
            String backupFileName = "dailychecklist-backup-" + timestamp + "-" + reason + "-" + checksum + ".zip";

            // Create zip file containing all data files
            createBackupZip(new File(backupDirFile, backupFileName));

            // Clean up old backups (keep only maxBackups)
            cleanupOldBackups();

        } catch (Exception e) {
            ApplicationErrorHandler.showBackupError(parentComponent, e);
        }
    }

    /**
     * Creates a ZIP file containing all data files.
     */
    private void createBackupZip(File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (String filePath : dataFiles) {
                File file = new File(filePath);
                if (file.exists()) {
                    // Extract just the filename for the ZIP entry
                    String fileName = file.getName();
                    addFileToZip(zos, file, fileName);
                }
            }
        }
    }

    /**
     * Adds a file to the ZIP output stream.
     */
    private void addFileToZip(ZipOutputStream zos, File file, String entryName) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
        }
    }

    /**
     * Removes old backup files, keeping only the most recent maxBackups files.
     */
    private void cleanupOldBackups() {
        File backupDirFile = new File(backupDir);
        File[] backupFiles = backupDirFile.listFiles((dir, name) ->
            name.startsWith("dailychecklist-backup-") && name.endsWith(".zip"));

        if (backupFiles != null && backupFiles.length > maxBackups) {
            // Sort by last modified time (oldest first)
            Arrays.sort(backupFiles, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));

            // Delete oldest files
            for (int i = 0; i < backupFiles.length - maxBackups; i++) {
                backupFiles[i].delete();
            }
        }
    }

    /**
     * Manually triggers a backup.
     */
    public void createManualBackup() {
        createBackup("manual");
    }

    /**
     * Calculates MD5 checksum of all data files for corruption detection.
     */
    private String calculateDataChecksum() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (String filePath : dataFiles) {
                File file = new File(filePath);
                if (file.exists()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            md.update(buffer, 0, bytesRead);
                        }
                    }
                }
            }
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            // Fallback to timestamp-based naming if checksum fails
            return "checksum-error";
        }
    }

    /**
     * Verifies the integrity of a backup file by comparing its stored checksum with current data.
     */
    public boolean verifyBackupIntegrity(File backupFile) {
        try {
            String fileName = backupFile.getName();
            // Extract checksum from filename (format: dailychecklist-backup-yyyy-MM-dd_HH-mm-ss-reason-checksum.zip)
            String[] parts = fileName.split("-");
            if (parts.length >= 6) {
                String storedChecksum = parts[parts.length - 1].replace(".zip", "");
                String currentChecksum = calculateDataChecksum();
                return storedChecksum.equals(currentChecksum);
            }
        } catch (Exception e) {
            // Ignore verification errors
        }
        return false;
    }

    /**
     * Shuts down the backup system and stops the backup thread.
     */
    public void shutdown() {
        backupRunning = false;
        if (scheduler != null) {
            try {
                scheduler.shutdownNow();
                scheduler.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}