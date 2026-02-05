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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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
            // Prefer system LAF then apply a few UI defaults to make the app appear flatter
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignore) {
                // fall back to default LAF
            }
            // Make common components use flatter borders/colors
            UIManager.put("Button.border", new javax.swing.border.EmptyBorder(6, 12, 6, 12));
            UIManager.put("TitledBorder.border", new javax.swing.border.EmptyBorder(6, 6, 6, 6));
            UIManager.put("SplitPane.dividerSize", 6);
            UIManager.put("SplitPane.border", new javax.swing.border.EmptyBorder(0, 0, 0, 0));
            UIManager.put("Separator.background", UIManager.getColor("Panel.background"));
            UIManager.put("Separator.foreground", UIManager.getColor("Panel.background"));

            // Create lifecycle manager
            lifecycleManager = new ApplicationLifecycleManager(null); // Parent component will be set later
            lifecycleManager.initialize();

            // Create UI
            checklist = new DailyChecklist(lifecycleManager);
            checklist.setVisible(true);

            // Update the repository parent component for error dialogs
            ((XMLTaskRepository) checklist.getRepository()).setParentComponent(checklist.getFrame());

            // Write test logs for debugging (immediate on startup)
            try {
                java.nio.file.Path mergeLog = java.nio.file.Paths.get(ApplicationConfiguration.APPLICATION_DATA_DIR, "restore-merge.log");
                java.nio.file.Files.createDirectories(mergeLog.getParent());
                try (java.io.OutputStreamWriter w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(mergeLog.toFile(), true), java.nio.charset.StandardCharsets.UTF_8)) {
                    w.write("=== TEST startup merge log at " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "\n");
                    w.flush();
                }
                java.nio.file.Path debugLog = java.nio.file.Paths.get(ApplicationConfiguration.APPLICATION_DATA_DIR, "restore-debug.log");
                try (java.io.OutputStreamWriter w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(debugLog.toFile(), true), java.nio.charset.StandardCharsets.UTF_8)) {
                    w.write("=== TEST startup debug log at " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "\n");
                    w.flush();
                }
            } catch (Exception ex) {
                // ignore test log failures
            }

            // Start background services
            lifecycleManager.start();

            // Add shutdown hook to properly close resources
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                lifecycleManager.shutdown();
                checklist.shutdown();
            }));

        } catch (Exception e) {
            e.printStackTrace();
            ErrorDialog.showError(null, "Failed to start application", e);
            System.exit(1);
        }
    }
}

