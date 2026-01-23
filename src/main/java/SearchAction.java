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
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

@SuppressWarnings("serial")
public class SearchAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    private java.awt.Component parent;
    private TaskManager taskManager;

    private DailyChecklist dailyChecklist;

    public SearchAction(java.awt.Component parent, TaskManager taskManager, DailyChecklist dailyChecklist) {
        super("SEARCH_TASKS");
        this.parent = parent;
        this.taskManager = taskManager;
        this.dailyChecklist = dailyChecklist;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SearchDialog.showSearchDialog(parent, taskManager, dailyChecklist);
    }
}