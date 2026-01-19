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
 */import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;

import javax.swing.JFrame;

public class TimerFrame {
    private JFrame frame;
    private TimerPanel timerPanel;

    public TimerFrame() {
        if (!GraphicsEnvironment.isHeadless()) {
            frame = new JFrame();
            frame.setTitle("Focus Timer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setAlwaysOnTop(true);
            frame.getContentPane().setBackground(Color.WHITE);

            timerPanel = new TimerPanel(this);
            frame.add(timerPanel);

            frame.addKeyListener(new TimerKeyListener(timerPanel));

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = screenSize.width - frame.getWidth();
            int y = 0;
            frame.setLocation(x, y);
        } else {
            timerPanel = new TimerPanel(this);
        }
    }

    public TimerPanel getTimerPanel() {
        return timerPanel;
    }
}

