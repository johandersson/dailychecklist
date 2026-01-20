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
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JOptionPane;

/**
 * Centralized error handling service that provides user-friendly HTML-formatted
 * error messages with recovery suggestions for common issues.
 */
public class ApplicationErrorHandler {
    private static final String HTML_HEADER = "<html><head><style>h1 { font-size: 24pt; color: #333; } h2 { font-size: 18pt; color: #555; } h3 { font-size: 14pt; color: #777; } h4 { font-size: 12pt; color: #777; font-weight: bold; } body { font-family: Arial, sans-serif; font-size: 12pt; color: #333; }</style></head><body style='margin: 10px;'>";
    private static final String HTML_FOOTER = "</body></html>";

    /**
     * Shows a user-friendly error dialog for file I/O operations with recovery suggestions.
     */
    public static void showFileError(Component parent, String operation, Exception e, String filePath) {
        String title = "File Access Error";
        String message = buildFileErrorMessage(operation, e, filePath);
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows a user-friendly error dialog for backup operations.
     */
    public static void showBackupError(Component parent, Exception e) {
        String title = "Backup Error";
        String message = buildBackupErrorMessage(e);
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Shows a user-friendly error dialog for data loading operations.
     */
    public static void showDataLoadError(Component parent, String dataType, Exception e) {
        String title = "Data Loading Error";
        String message = buildDataLoadErrorMessage(dataType, e);
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows a user-friendly error dialog for data saving operations.
     */
    public static void showDataSaveError(Component parent, String dataType, Exception e) {
        String title = "Data Saving Error";
        String message = buildDataSaveErrorMessage(dataType, e);
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows a user-friendly error dialog for initialization failures.
     */
    public static void showInitializationError(Component parent, Exception e) {
        String title = "Application Initialization Error";
        String message = buildInitializationErrorMessage(e);
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private static String buildFileErrorMessage(String operation, Exception e, String filePath) {
        StringBuilder sb = new StringBuilder(HTML_HEADER);
        sb.append("<h3>File Access Problem</h3>");
        sb.append("<p><b>Operation:</b> ").append(operation).append("</p>");
        sb.append("<p><b>File:</b> ").append(filePath).append("</p>");

        String errorType = analyzeException(e);
        sb.append("<p><b>Problem:</b> ").append(errorType).append("</p>");

        sb.append("<h4>Solutions:</h4><ul>");

        switch (errorType) {
            case "File not found":
                sb.append("<li>The file may have been deleted or moved</li>");
                sb.append("<li>Check if the file path is correct: <code>").append(filePath).append("</code></li>");
                sb.append("<li>The application will create a new file automatically</li>");
                break;
            case "Permission denied":
                sb.append("<li>You don't have write access to this location</li>");
                sb.append("<li>Try running the application as administrator</li>");
                sb.append("<li>Move the application to a folder where you have write access</li>");
                sb.append("<li>Check antivirus software isn't blocking the application</li>");
                break;
            case "Disk full":
                sb.append("<li>Your disk is full or almost full</li>");
                sb.append("<li>Free up some disk space</li>");
                sb.append("<li>Move the application to a different drive</li>");
                break;
            case "File locked":
                sb.append("<li>The file is being used by another program</li>");
                sb.append("<li>Close other instances of this application</li>");
                sb.append("<li>Check if antivirus software is scanning the file</li>");
                break;
            default:
                sb.append("<li>Try restarting the application</li>");
                sb.append("<li>Check your disk space and permissions</li>");
                sb.append("<li>Contact support if the problem persists</li>");
        }

        sb.append("</ul>");
        sb.append("<p style='color: #777; font-size: 11pt;'>Error details: ").append(e.getMessage()).append("</p>");
        sb.append(HTML_FOOTER);
        return sb.toString();
    }

    private static String buildBackupErrorMessage(Exception e) {
        StringBuilder sb = new StringBuilder(HTML_HEADER);
        sb.append("<h3>Backup Creation Failed</h3>");
        sb.append("<p>Your data could not be backed up automatically.</p>");

        String errorType = analyzeException(e);
        sb.append("<p><b>Problem:</b> ").append(errorType).append("</p>");

        sb.append("<h4>What this means:</h4>");
        sb.append("<ul>");
        sb.append("<li>Your main data is safe and unaffected</li>");
        sb.append("<li>Automatic backups are temporarily disabled</li>");
        sb.append("<li>You can still save your data manually</li>");
        sb.append("</ul>");

        sb.append("<h4>Solutions:</h4><ul>");

        switch (errorType) {
            case "Permission denied":
                sb.append("<li>Grant write access to the backup folder</li>");
                sb.append("<li>Change backup location in settings</li>");
                break;
            case "Disk full":
                sb.append("<li>Free up disk space in the backup location</li>");
                sb.append("<li>Change backup location to a different drive</li>");
                break;
            default:
                sb.append("<li>Try creating a manual backup</li>");
                sb.append("<li>Check backup folder permissions</li>");
        }

        sb.append("</ul>");
        sb.append("<p style='color: #777; font-size: 11pt;'>Error details: ").append(e.getMessage()).append("</p>");
        sb.append(HTML_FOOTER);
        return sb.toString();
    }

    private static String buildDataLoadErrorMessage(String dataType, Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Data Loading Problem\n\n");
        sb.append("Data Type: ").append(dataType).append("\n\n");

        String errorType = analyzeException(e);
        sb.append("Problem: ").append(errorType).append("\n\n");

        sb.append("What this means:\n");
        sb.append("- The application will use default/empty data\n");
        sb.append("- Your data may not be lost - check the file location\n");
        sb.append("- You can try to recover from a backup\n\n");

        sb.append("Solutions:\n");
        sb.append("- Check if data files exist and are readable\n");
        sb.append("- Try restoring from a backup file\n");
        sb.append("- Restart the application\n\n");

        sb.append("Error details: ").append(e.getMessage());
        return sb.toString();
    }

    private static String buildDataSaveErrorMessage(String dataType, Exception e) {
        StringBuilder sb = new StringBuilder(HTML_HEADER);
        sb.append("<h3>Data Saving Problem</h3>");
        sb.append("<p><b>Data Type:</b> ").append(dataType).append("</p>");

        String errorType = analyzeException(e);
        sb.append("<p><b>Problem:</b> ").append(errorType).append("</p>");

        sb.append("<h4>What this means:</h4>");
        sb.append("<ul>");
        sb.append("<li>Your recent changes may not be saved</li>");
        sb.append("<li>Data in memory is still intact</li>");
        sb.append("<li>Try saving again after fixing the issue</li>");
        sb.append("</ul>");

        sb.append("<h4>Solutions:</h4><ul>");
        sb.append("<li>Check file permissions and disk space</li>");
        sb.append("<li>Try saving to a different location</li>");
        sb.append("<li>Close other programs that might be using the file</li>");
        sb.append("</ul>");

        sb.append("<p style='color: #777; font-size: 11pt;'>Error details: ").append(e.getMessage()).append("</p>");
        sb.append(HTML_FOOTER);
        return sb.toString();
    }

    private static String buildInitializationErrorMessage(Exception e) {
        StringBuilder sb = new StringBuilder(HTML_HEADER);
        sb.append("<h3>Application Startup Failed</h3>");
        sb.append("<p>The application could not start properly.</p>");

        String errorType = analyzeException(e);
        sb.append("<p><b>Problem:</b> ").append(errorType).append("</p>");

        sb.append("<h4>Solutions:</h4><ul>");
        sb.append("<li>Check file permissions in the user home directory</li>");
        sb.append("<li>Ensure sufficient disk space is available</li>");
        sb.append("<li>Try running as administrator</li>");
        sb.append("<li>Check if antivirus software is blocking the application</li>");
        sb.append("<li>Try deleting temporary files or reinstalling</li>");
        sb.append("</ul>");

        sb.append("<p style='color: #777; font-size: 11pt;'>Error details: ").append(e.getMessage()).append("</p>");
        sb.append(HTML_FOOTER);
        return sb.toString();
    }

    /**
     * Analyzes an exception to provide a user-friendly error type description.
     */
    private static String analyzeException(Exception e) {
        String message = e.getMessage().toLowerCase();

        if (message.contains("permission denied") || message.contains("access denied")) {
            return "Permission denied";
        } else if (message.contains("no such file") || message.contains("file not found")) {
            return "File not found";
        } else if (message.contains("disk full") || message.contains("no space")) {
            return "Disk full";
        } else if (message.contains("being used by another process") || message.contains("locked")) {
            return "File locked";
        } else if (message.contains("read-only")) {
            return "Read-only file system";
        } else if (e instanceof java.net.UnknownHostException) {
            return "Network connection problem";
        } else {
            return "Unknown error";
        }
    }

    /**
     * Attempts to open the backup directory in the system file explorer.
     */
    public static void openBackupDirectory() {
        try {
            String backupDir = System.getProperty("user.home") + File.separator + "dailychecklist-backups";
            Path path = Paths.get(backupDir);
            if (Files.exists(path)) {
                Desktop.getDesktop().open(path.toFile());
            }
        } catch (IOException e) {
            // Ignore - user can navigate manually
        }
    }

    /**
     * Checks if a file path is writable and provides suggestions if not.
     */
    public static boolean checkFileWritable(Component parent, String filePath) {
        Path path = Paths.get(filePath);
        Path parentPath = path.getParent();

        if (parentPath != null && !Files.exists(parentPath)) {
            try {
                Files.createDirectories(parentPath);
            } catch (IOException e) {
                showFileError(parent, "Create directory", e, parentPath.toString());
                return false;
            }
        }

        if (Files.exists(path) && !Files.isWritable(path)) {
            showFileError(parent, "Write to file", new IOException("File is read-only"), filePath);
            return false;
        }

        return true;
    }
}