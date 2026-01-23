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

