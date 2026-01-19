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
 */import javax.swing.*;
import java.awt.*;

public class CirclePanel extends JPanel {
    private JLabel[] circles;

    public CirclePanel() {
        setLayout(new FlowLayout());
        setBackground(Color.WHITE);
        circles = new JLabel[4];
        for (int i = 0; i < circles.length; i++) {

            circles[i] = new JLabel("\u25CF"); // Unicode for a filled circle
            circles[i].setFont(new Font("Yu Gothic UI Light", Font.PLAIN, 24));
            circles[i].setForeground(Color.LIGHT_GRAY);
            add(circles[i]);
        }
    }

    public void updateCircles(int completedChunks) {
        for (int i = 0; i < circles.length; i++) {
            if (i < completedChunks) {
                circles[i].setForeground(Color.LIGHT_GRAY);
            } else {
                circles[i].setForeground(Color.PINK);
            }
        }
    }
}

