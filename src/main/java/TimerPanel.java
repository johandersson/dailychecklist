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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

@SuppressWarnings("serial")
public class TimerPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JLabel timerLabel;
    private JLabel taskLabel;
    private Timer timer;
    private int timeRemaining;
    private String lastTask = "";
    private String lastAmountOfMinutesForTask = "";
    private transient TimerFrame frame;
    private CirclePanel circlePanel;
    private static final int CHUNK_SIZE = 5 * 60;

    @SuppressWarnings("this-escape")
    public TimerPanel(TimerFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        setupAndStartTimer();
    }

    public void setupAndStartTimer() {
        if (GraphicsEnvironment.isHeadless()) {
            // In headless mode, set default values
            lastTask = "Test Task";
            lastAmountOfMinutesForTask = "5";
            timeRemaining = 5 * 60;
            return;
        }

        TaskInputPanel inputPanel = new TaskInputPanel(lastTask, lastAmountOfMinutesForTask);
        int result = JOptionPane.showConfirmDialog(null, inputPanel, "Task and Timer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            lastTask = inputPanel.getTask();
            lastAmountOfMinutesForTask = inputPanel.getSelectedTime();
            timeRemaining = inputPanel.getTimeInSeconds();

            removeAll();

            circlePanel = new CirclePanel();
            add(circlePanel, BorderLayout.NORTH);

            timerLabel = new JLabel(formatTime(timeRemaining), SwingConstants.CENTER);
            timerLabel.setFont(new Font("Yu Gothic UI Light", Font.PLAIN, 48));
            add(timerLabel, BorderLayout.CENTER);

            taskLabel = new JLabel(lastTask, SwingConstants.CENTER);
            taskLabel.setFont(new Font("Yu Gothic UI Light", Font.PLAIN, 24));
            add(taskLabel, BorderLayout.SOUTH);

            revalidate();
            repaint();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (timeRemaining <= 0) {
                        removeMouseListener(this);
                        setupAndStartTimer();
                    }
                }
            });

            timer = new Timer(1000, null);
            TimerActionListener listener = new TimerActionListener(timeRemaining, timer, timerLabel, this::updateCircles);
            timer.addActionListener(listener);
            timer.start();
            timer.start();
        } else {
            System.exit(0);
        }
    }

    public void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
    }

    public boolean confirmRestart() {
        if (GraphicsEnvironment.isHeadless()) {
            return true; // In headless mode, assume yes
        }
        int confirm = JOptionPane.showConfirmDialog(null, "Do you want to restart the timer?", "Confirm", JOptionPane.YES_NO_OPTION);
        return confirm == JOptionPane.YES_OPTION;
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private void updateCircles() {
        int completedChunks = (CHUNK_SIZE - timeRemaining) / CHUNK_SIZE;
        circlePanel.updateCircles(completedChunks);
    }

    // ...existing code...
}
