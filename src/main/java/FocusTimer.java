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
 */import java.awt.BorderLayout;
import java.awt.Color;
        import java.awt.FlowLayout;
        import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

        public class FocusTimer {
            private JLabel timerLabel;
            private JLabel taskLabel;
            private Timer timer;
            private int timeRemaining; // Time in seconds
            private JPanel circlePanel;
            private static final int CHUNK_SIZE = 5 * 60; // 5 minutes in seconds
            private final JLabel[] circles = new JLabel[4]; // 4 chunks of 5 minutes
            private JFrame frame;
            private int totalDurationSeconds = 20 * 60; // tracks selected duration for circles
            // removed unused 'lastTask' and 'lastAmountOfMinutesForTask' fields

            private static FocusTimer instance;

            private FocusTimer() {
                if (!java.awt.GraphicsEnvironment.isHeadless()) {
                    frame = new JFrame("Focus Timer");
                    frame.setIconImage(IconCache.getAppIcon());
                    frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                    frame.setSize(400, 300);
                    frame.setAlwaysOnTop(true);
                    frame.getContentPane().setBackground(Color.WHITE);
                    frame.setLayout(new BorderLayout());

                    // Create UI components once and reuse for better performance
                    circlePanel = new JPanel(new FlowLayout());
                    circlePanel.setBackground(Color.WHITE);
                    for (int i = 0; i < circles.length; i++) {
                        circles[i] = new JLabel("\u25CF");
                        circles[i].setFont(new Font("Yu Gothic UI Light", Font.PLAIN, 24));
                        circles[i].setForeground(Color.LIGHT_GRAY);
                        circlePanel.add(circles[i]);
                    }

                    timerLabel = new JLabel(formatTime(timeRemaining), SwingConstants.CENTER);
                    timerLabel.setFont(new Font("Yu Gothic UI Light", Font.PLAIN, 48));

                    taskLabel = new JLabel("", SwingConstants.CENTER);
                    taskLabel.setFont(new Font("Yu Gothic UI Light", Font.PLAIN, 24));

                    frame.add(circlePanel, BorderLayout.NORTH);
                    frame.add(timerLabel, BorderLayout.CENTER);
                    frame.add(taskLabel, BorderLayout.SOUTH);

                    // Single window listener to stop timer when the window closes
                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            if (timer != null && timer.isRunning()) timer.stop();
                            frame.setVisible(false);
                        }
                    });
                }
            }

            public static FocusTimer getInstance() {
                if (instance == null) {
                    instance = new FocusTimer();
                }
                return instance;
            }

            public void startFocusTimer(String taskName, String duration) {
                        // remember strings removed; not used elsewhere

                //popup ask for time
                String[] timeOptions = {"20 minutes", "15 minutes", "10 minutes", "5 minutes"};
                String selectedTime = (String) JOptionPane.showInputDialog(
                        frame,
                        "Select timer countdown:",
                        "Focus Timer",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        timeOptions,
                        timeOptions[0]
                );
                if (selectedTime == null) {
                    return; // User cancelled the dialog
                }
                switch (selectedTime) {
                    case "15 minutes":
                        timeRemaining = 15 * 60;
                        break;
                    case "10 minutes":
                        timeRemaining = 10 * 60;
                        break;
                    case "5 minutes":
                        timeRemaining = 5 * 60;
                        break;
                    case "20 minutes":
                    default:
                        timeRemaining = 20 * 60;
                        break;
                }

                setupAndStartTimer(taskName);
            }

            private void setupAndStartTimer(String taskName) {
                // Reset UI state and reuse components
                if (timer != null && timer.isRunning()) {
                    timer.stop();
                }
                timerLabel.setText(formatTime(timeRemaining));
                taskLabel.setText(taskName);
                // Store the total duration so circles reflect the chosen length
                totalDurationSeconds = timeRemaining;
                updateCircles();

                // Timer setup
                timer = new Timer(1000, e -> {
                    if (timeRemaining > 0) {
                        timeRemaining--;
                        timerLabel.setText(formatTime(timeRemaining));
                        updateCircles();
                    } else {
                        timer.stop();
                        timerLabel.setText("Time's up!");
                        JOptionPane.showMessageDialog(frame, "Time's up!", "Focus Timer", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
                timer.start();

                frame.setVisible(true);
                frame.toFront();
            }

            private String formatTime(int seconds) {
                int mins = seconds / 60;
                int secs = seconds % 60;
                return String.format("%02d:%02d", mins, secs);
            }

            private void updateCircles() {
                int completedChunks = (totalDurationSeconds - timeRemaining) / CHUNK_SIZE;
                if (completedChunks < 0) completedChunks = 0;
                if (completedChunks > circles.length) completedChunks = circles.length;
                for (int i = 0; i < circles.length; i++) {
                    if (i < completedChunks) {
                        var lightGreen = new Color(192, 219, 165);
                        circles[i].setForeground(lightGreen);
                    } else {
                        circles[i].setForeground(Color.LIGHT_GRAY);
                    }
                }
            }
        }
