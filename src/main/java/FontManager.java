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
import java.awt.Font;

public class FontManager {
    public static final String FONT_NAME = "Yu Gothic UI";

    public static final int SIZE_DEFAULT = 16;
    public static final int SIZE_TASK_LIST = 14;
    public static final int SIZE_BUTTON = 12;
    public static final int SIZE_SMALL = 10;
    public static final int SIZE_SMALL_MEDIUM = 11;
    public static final int SIZE_HEADER_2 = 18;
    public static final int SIZE_HEADER_1 = 24;
    public static final int SIZE_TITLE = 28;

    public static Font getDefaultFont() {
        return new Font(FONT_NAME, Font.PLAIN, SIZE_DEFAULT);
    }

    public static Font getTaskListFont() {
        return new Font(FONT_NAME, Font.PLAIN, SIZE_TASK_LIST);
    }

    public static Font getButtonFont() {
        return new Font(FONT_NAME, Font.PLAIN, SIZE_BUTTON);
    }

    public static Font getSmallFont() {
        return new Font(FONT_NAME, Font.PLAIN, SIZE_SMALL);
    }

    public static Font getSmallMediumFont() {
        return new Font(FONT_NAME, Font.PLAIN, SIZE_SMALL_MEDIUM);
    }

    public static Font getHeader2Font() {
        return new Font(FONT_NAME, Font.PLAIN, SIZE_HEADER_2);
    }

    public static Font getHeader1Font() {
        return new Font(FONT_NAME, Font.BOLD, SIZE_HEADER_1);
    }

    public static Font getTitleFont() {
        return new Font(FONT_NAME, Font.BOLD, SIZE_TITLE);
    }

    public static Font getArialButtonFont() {
        return new Font("Arial", Font.PLAIN, SIZE_BUTTON);
    }

    public static Font getArialTitleFont() {
        return new Font("Arial", Font.BOLD, SIZE_TITLE);
    }

    public static Font getArialHeader1Font() {
        return new Font("Arial", Font.BOLD, SIZE_HEADER_1);
    }
}