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

public final class UiLayout {
    private UiLayout() {}

    // Reserved areas on the right for icons: reminder area + weekday area.
    public static final int REMINDER_ICON_AREA = 80; // space reserved for reminder clock + optional time text
    public static final int WEEKDAY_ICON_AREA = 40; // space reserved for weekday circle
    public static final int NOTE_ICON_AREA = 35; // space reserved for note icon (note indicator)
    public static final int RIGHT_ICON_SPACE = REMINDER_ICON_AREA + WEEKDAY_ICON_AREA + NOTE_ICON_AREA + 12; // total reserved space
    // Spacing used by renderer/handlers for add-subtask icon
    public static final int ADD_SUBTASK_OFFSET = 36;
    // Checkbox constants
    public static final int CHECKBOX_X = 10;
    public static final int CHECKBOX_SIZE = 22;
    // Text/list indentation
    public static final int BASE_INDENT = 40;
    public static final int SUBTASK_INDENT = 24;
}
