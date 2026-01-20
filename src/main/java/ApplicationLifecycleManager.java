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

/**
 * Manages the application lifecycle including proper initialization and shutdown.
 * Ensures all resources are properly cleaned up and components are initialized in the correct order.
 */
public class ApplicationLifecycleManager {
    private final Component parentComponent;
    private TaskRepository repository;
    private SettingsManager settingsManager;
    private ReminderQueue reminderQueue;
    private FocusTimer focusTimer;

    /**
     * Creates a new ApplicationLifecycleManager.
     *
     * @param parentComponent Parent component for error dialogs
     */
    public ApplicationLifecycleManager(Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    /**
     * Initializes all application components in the correct order.
     */
    public void initialize() {
        try {
            // Initialize settings first
            settingsManager = new SettingsManager(parentComponent);
            settingsManager.load();

            // Initialize repository
            repository = new XMLTaskRepository(parentComponent);
            repository.initialize();

            // Note: ReminderQueue and reminder checking thread are initialized in the UI layer
            // because they require UI callbacks

        } catch (Exception e) {
            ApplicationErrorHandler.showInitializationError(parentComponent, e);
            throw new RuntimeException("Failed to initialize application", e);
        }
    }

    /**
     * Starts background services after the application is fully initialized.
     */
    public void start() {
        try {
            // Start repository background services (like automatic backups)
            repository.start();
        } catch (Exception e) {
            ApplicationErrorHandler.showInitializationError(parentComponent, e);
            throw new RuntimeException("Failed to start application services", e);
        }
    }

    /**
     * Shuts down all application components in the correct order.
     */
    public void shutdown() {
        try {
            // Shutdown repository (which shuts down backup system)
            if (repository != null) {
                repository.shutdown();
            }

            // Note: ReminderQueue and reminder checking thread are shut down in the UI layer

        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
            // Don't throw during shutdown to avoid masking the original error
        }
    }

    /**
     * Gets the initialized task repository.
     */
    public TaskRepository getRepository() {
        return repository;
    }

    /**
     * Gets the initialized settings manager.
     */
    public SettingsManager getSettingsManager() {
        return settingsManager;
    }
}