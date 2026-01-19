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
import java.awt.GraphicsEnvironment;

import javax.swing.JOptionPane;

public class MemorySafetyManager {
    // Reasonable limits to prevent memory overflow and performance issues
    public static final int MAX_TASKS = 100000;
    public static final int MAX_REMINDERS = 10000;

    /**
     * Checks if the given count exceeds the safe limit for tasks.
     * Shows a warning dialog if the limit is exceeded.
     * @param count The number of tasks
     * @return true if the count exceeds the limit, false otherwise
     */
    public static boolean checkTaskLimit(int count) {
        if (count > MAX_TASKS) {
            showTaskLimitWarning(count);
            return true;
        }
        return false;
    }

    /**
     * Checks if the given count exceeds the safe limit for reminders.
     * Shows a warning dialog if the limit is exceeded.
     * @param count The number of reminders
     * @return true if the count exceeds the limit, false otherwise
     */
    public static boolean checkReminderLimit(int count) {
        if (count > MAX_REMINDERS) {
            showReminderLimitWarning(count);
            return true;
        }
        return false;
    }

    /**
     * Shows a warning dialog about exceeding the task limit.
     */
    private static void showTaskLimitWarning(int count) {
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null,
                "Warning: Your task file contains " + count + " tasks, which exceeds the safe limit of " + MAX_TASKS + ".\n" +
                "For performance and memory safety, only the first " + MAX_TASKS + " tasks will be loaded.\n" +
                "Consider archiving old completed tasks or splitting into multiple checklists.",
                "Large Dataset Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Shows a warning dialog about exceeding the reminder limit.
     */
    private static void showReminderLimitWarning(int count) {
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null,
                "Warning: Your reminders file contains " + count + " reminders, which exceeds the safe limit of " + MAX_REMINDERS + ".\n" +
                "For performance and memory safety, only the first " + MAX_REMINDERS + " reminders will be loaded.\n" +
                "Consider removing old or unnecessary reminders.",
                "Large Dataset Warning", JOptionPane.WARNING_MESSAGE);
        }
    }
}