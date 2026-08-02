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
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Handles the "Import Calendar" menu action: prompts for an .ics file,
 * parses it, and persists the resulting events via {@link TaskManager}.
 */
public final class ImportCalendarAction {
    private ImportCalendarAction() {}

    public static void importCalendar(Component parent, TaskManager taskManager, Runnable updateTasks) {
        File icsFile = chooseIcsFile(parent);
        if (icsFile == null) return;

        try {
            List<CalendarEvent> events = IcsCalendarParser.parse(icsFile);
            if (events.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "No events were found in the selected file.", "Import Calendar", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            taskManager.addCalendarEvents(events);
            if (updateTasks != null) updateTasks.run();
            JOptionPane.showMessageDialog(parent,
                    "Imported " + events.size() + " calendar event(s) from " + icsFile.getName() + ".",
                    "Import Calendar", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ErrorDialog.showError(parent, "Failed to import calendar file", ex);
        }
    }

    private static File chooseIcsFile(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select an ICS calendar file to import");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter icsFilter = new FileNameExtensionFilter("iCalendar files (*.ics)", "ics");
        chooser.addChoosableFileFilter(icsFilter);
        chooser.setFileFilter(icsFilter);
        chooser.setAcceptAllFileFilterUsed(false);
        int res = chooser.showOpenDialog(parent);
        if (res != JFileChooser.APPROVE_OPTION) return null;
        return chooser.getSelectedFile();
    }
}
