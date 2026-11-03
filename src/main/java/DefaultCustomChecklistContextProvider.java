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
 * Default context provider for when NOT in the custom checklists tab.
 * Always returns false for isInCustomChecklistsTab() and null for selected checklist.
 */
public class DefaultCustomChecklistContextProvider implements CustomChecklistContextProvider {
    
    @Override
    public Checklist getSelectedChecklist() {
        return null;
    }
    
    @Override
    public void setSelectedChecklistInUI(Checklist checklist) {
        // No-op for non-custom-checklist contexts
    }
    
    @Override
    public boolean isInCustomChecklistsTab() {
        return false;
    }
}
