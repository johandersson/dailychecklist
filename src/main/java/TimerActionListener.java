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
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Timer;

public class TimerActionListener implements ActionListener {
    private int timeRemaining;
    private Timer timer;
    private JLabel timerLabel;
    private Runnable updateCircles;

    public TimerActionListener(int timeRemaining, Timer timer, JLabel timerLabel, Runnable updateCircles) {
        this.timeRemaining = timeRemaining;
        this.timer = timer;
        this.timerLabel = timerLabel;
        this.updateCircles = updateCircles;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (timeRemaining > 0) {
            timeRemaining--;
            timerLabel.setText(formatTime(timeRemaining));
            if (updateCircles != null) updateCircles.run();
        } else {
            timer.stop();
            timerLabel.setText("Time's up!");
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Time's up!", "Focus Timer", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }
}
