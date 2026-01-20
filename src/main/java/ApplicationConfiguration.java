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
import java.io.File;

/**
 * Centralized configuration constants for the Daily Checklist application.
 * This class externalizes magic numbers and settings to improve maintainability.
 */
public final class ApplicationConfiguration {
    // Application metadata
    public static final String APPLICATION_NAME = "Daily Checklist";
    public static final String APPLICATION_VERSION = "1.0";
    public static final String APPLICATION_AUTHOR = "Johan Andersson";

    // File and directory names
    public static final String DATA_FILE_NAME = "tasks.xml";
    public static final String BACKUP_DIRECTORY_NAME = "dailychecklist-backups";
    public static final String SETTINGS_FILE_NAME = "settings.properties";
    public static final String REMINDERS_FILE_NAME = "reminders.properties";
    public static final String CHECKLIST_NAMES_FILE_NAME = "checklist-names.properties";

    // Backup configuration
    public static final int MAX_BACKUP_FILES = 10;
    public static final long BACKUP_INTERVAL_MINUTES = 5;
    public static final long BACKUP_INTERVAL_MILLIS = BACKUP_INTERVAL_MINUTES * 60 * 1000;

    // UI Configuration
    public static final int DEFAULT_WINDOW_WIDTH = 800;
    public static final int DEFAULT_WINDOW_HEIGHT = 600;
    public static final int MINIMUM_WINDOW_WIDTH = 400;
    public static final int MINIMUM_WINDOW_HEIGHT = 300;

    // Timer configuration
    public static final int FOCUS_TIMER_DEFAULT_MINUTES = 25;
    public static final int FOCUS_TIMER_BREAK_MINUTES = 5;
    public static final long TIMER_UPDATE_INTERVAL_MS = 1000; // 1 second

    // Reminder configuration
    public static final long REMINDER_CHECK_INTERVAL_MS = 60000; // 1 minute
    public static final int REMINDER_DIALOG_TIMEOUT_SECONDS = 30;

    // Task management
    public static final int MAX_TASK_TITLE_LENGTH = 200;
    public static final int MAX_TASK_DESCRIPTION_LENGTH = 1000;

    // Memory safety
    public static final long MEMORY_CLEANUP_INTERVAL_MS = 300000; // 5 minutes
    public static final int MAX_UNDO_HISTORY_SIZE = 50;

    // File paths
    public static final String USER_HOME = System.getProperty("user.home");
    public static final String APPLICATION_DATA_DIR = USER_HOME + File.separator + ".dailychecklist";
    public static final String BACKUP_DIRECTORY = USER_HOME + File.separator + BACKUP_DIRECTORY_NAME;
    public static final String SETTINGS_FILE_PATH = USER_HOME + File.separator + ".dailychecklist" + File.separator + SETTINGS_FILE_NAME;
    public static final String REMINDERS_FILE_PATH = USER_HOME + File.separator + ".dailychecklist" + File.separator + REMINDERS_FILE_NAME;
    public static final String CHECKLIST_NAMES_FILE_PATH = USER_HOME + File.separator + ".dailychecklist" + File.separator + CHECKLIST_NAMES_FILE_NAME;

    // XML Configuration
    public static final String XML_ROOT_ELEMENT = "tasks";
    public static final String XML_TASK_ELEMENT = "task";
    public static final String XML_TITLE_ATTRIBUTE = "title";
    public static final String XML_COMPLETED_ATTRIBUTE = "completed";
    public static final String XML_TYPE_ATTRIBUTE = "type";
    public static final String XML_REMINDER_ATTRIBUTE = "reminder";
    public static final String XML_DESCRIPTION_ELEMENT = "description";

    // Date/Time formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // UI Text constants
    public static final String DEFAULT_CHECKLIST_NAME = "Daily Tasks";
    public static final String UNTITLED_CHECKLIST = "Untitled Checklist";

    // Error messages
    public static final String ERROR_TITLE_LOAD_FAILED = "Load Failed";
    public static final String ERROR_TITLE_SAVE_FAILED = "Save Failed";
    public static final String ERROR_TITLE_BACKUP_FAILED = "Backup Failed";

    // Logging
    public static final boolean DEBUG_MODE = Boolean.getBoolean("dailychecklist.debug");
    public static final String LOG_FILE_NAME = "dailychecklist.log";

    // Prevent instantiation
    private ApplicationConfiguration() {
        throw new UnsupportedOperationException("Configuration class cannot be instantiated");
    }

    /**
     * Gets the path to the main data file.
     */
    public static String getDataFilePath() {
        return APPLICATION_DATA_DIR + File.separator + DATA_FILE_NAME;
    }

    /**
     * Gets the path to a backup file with the given timestamp.
     */
    public static String getBackupFilePath(String timestamp) {
        return BACKUP_DIRECTORY + File.separator + "backup-" + timestamp + ".zip";
    }

    /**
     * Ensures the application data directory exists.
     */
    public static void ensureDataDirectoryExists() {
        File dir = new File(APPLICATION_DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Ensures the backup directory exists.
     */
    public static void ensureBackupDirectoryExists() {
        File dir = new File(BACKUP_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}