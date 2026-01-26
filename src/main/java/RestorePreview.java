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
import java.util.Map;

/**
 * Holder for restore preview statistics.
 */
public class RestorePreview {
    public final Map<String,String> checklists;
    public final Map<String,Integer> checklistTaskCounts;
    public final int newChecklistCount;
    public final int morningCount;
    public final int eveningCount;

    public RestorePreview(Map<String,String> checklists, Map<String,Integer> checklistTaskCounts, int newChecklistCount, int morningCount, int eveningCount) {
        this.checklists = checklists;
        this.checklistTaskCounts = checklistTaskCounts;
        this.newChecklistCount = newChecklistCount;
        this.morningCount = morningCount;
        this.eveningCount = eveningCount;
    }
}
