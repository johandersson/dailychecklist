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

import java.awt.*;
import javax.swing.*;

public class SearchPanel extends JPanel {
    public final JTextField searchField;
    public final JButton searchButton;
    public final JCheckBox searchAllWeekdayBox;

    public SearchPanel() {
        super(new FlowLayout());
        searchField = new JTextField(28);
        searchField.setFont(FontManager.getTaskListFont());
        searchButton = new JButton("Search");
        searchButton.setFont(FontManager.getButtonFont());
        searchAllWeekdayBox = new JCheckBox("Include all weekday tasks");
        searchAllWeekdayBox.setToolTipText("<html><p style='font-family:Arial,sans-serif;font-size:11px;margin:0;'>When checked, weekday-specific tasks for any weekday will be included in results.</p></html>");
        add(new JLabel("Search:"));
        add(searchField);
        add(searchButton);
        add(searchAllWeekdayBox);
    }
}
