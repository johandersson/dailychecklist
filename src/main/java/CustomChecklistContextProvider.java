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

/**
 * Provides context information about the custom checklists tab state.
 * Used to determine which checklist is currently selected/marked.
 */
public interface CustomChecklistContextProvider {
    /**
     * Gets the currently selected/marked checklist in the custom checklists tab.
     * @return The selected Checklist, or null if no checklist is selected
     */
    Checklist getSelectedChecklist();
    
    /**
     * Sets the selected checklist in the UI.
     * @param checklist The checklist to select, or null to clear selection
     */
    void setSelectedChecklistInUI(Checklist checklist);
    
    /**
     * Checks if the application is currently in the custom checklists tab.
     * @return true if in custom checklists tab, false otherwise
     */
    boolean isInCustomChecklistsTab();
}
