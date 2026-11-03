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
import javax.swing.JTabbedPane;

/**
 * Context provider that delegates to the appropriate tab's context provider
 * based on the currently selected tab.
 */
public class TabbedPaneContextProvider implements CustomChecklistContextProvider {
    private final JTabbedPane tabbedPane;
    private final CustomChecklistContextProvider customChecklistsProvider;
    private final CustomChecklistContextProvider defaultProvider;
    
    // Tab index for custom checklists (must match the index in DailyChecklist.addTabbedPane)
    private static final int CUSTOM_CHECKLISTS_TAB_INDEX = 1;
    
    public TabbedPaneContextProvider(JTabbedPane tabbedPane, CustomChecklistContextProvider customChecklistsProvider) {
        this.tabbedPane = tabbedPane;
        this.customChecklistsProvider = customChecklistsProvider;
        this.defaultProvider = new DefaultCustomChecklistContextProvider();
    }
    
    /**
     * Returns the currently active provider based on the selected tab.
     */
    private CustomChecklistContextProvider getActiveProvider() {
        if (tabbedPane.getSelectedIndex() == CUSTOM_CHECKLISTS_TAB_INDEX) {
            return customChecklistsProvider;
        }
        return defaultProvider;
    }

    @Override
    public Checklist getSelectedChecklist() {
        return getActiveProvider().getSelectedChecklist();
    }

    @Override
    public void setSelectedChecklistInUI(Checklist checklist) {
        getActiveProvider().setSelectedChecklistInUI(checklist);
    }

    @Override
    public boolean isInCustomChecklistsTab() {
        // Check the tab index directly rather than delegating, to ensure correct behavior
        return tabbedPane.getSelectedIndex() == CUSTOM_CHECKLISTS_TAB_INDEX;
    }
}
