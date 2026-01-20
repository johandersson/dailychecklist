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
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {
    private static ApplicationLifecycleManager lifecycleManager;
    private static DailyChecklist checklist;

    public static void main(String[] args) {
        // Start the application
        SwingUtilities.invokeLater(() -> {
            startApplication();
        });
    }

    private static void startApplication() {
        try {
            // Create lifecycle manager
            lifecycleManager = new ApplicationLifecycleManager(null); // Parent component will be set later
            lifecycleManager.initialize();

            // Create UI
            checklist = new DailyChecklist(lifecycleManager);
            checklist.setVisible(true);

            // Update lifecycle manager with the frame as parent component
            lifecycleManager = new ApplicationLifecycleManager(checklist.getFrame());
            lifecycleManager.initialize(); // Re-initialize with proper parent

            // Update the repository parent component for error dialogs
            ((XMLTaskRepository) checklist.getRepository()).setParentComponent(checklist.getFrame());

            // Start background services
            lifecycleManager.start();

            // Add shutdown hook to properly close resources
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                lifecycleManager.shutdown();
                checklist.shutdown();
            }));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to start application: " + e.getMessage(), "Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}

