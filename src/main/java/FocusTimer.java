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
            private JLabel[] circles = new JLabel[4]; // 4 chunks of 5 minutes
            private JFrame frame;
            private String lastTask = ""; // Remember the last task
            private String lastAmountOfMinutesForTask = "";

            private static FocusTimer instance;

            private FocusTimer() {
                if (!java.awt.GraphicsEnvironment.isHeadless()) {
                    frame = new JFrame("Focus Timer");
                    frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                    frame.setSize(400, 300);
                    frame.setAlwaysOnTop(true);
                    frame.getContentPane().setBackground(Color.WHITE);
                }
            }

            public static FocusTimer getInstance() {
                if (instance == null) {
                    instance = new FocusTimer();
                }
                return instance;
            }

            public void startFocusTimer(String taskName, String duration) {
                lastTask = taskName;
                lastAmountOfMinutesForTask = duration;

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
                frame.getContentPane().removeAll();
                frame.addWindowListener(
                        new WindowAdapter() {
                            @Override
                            public void windowClosing(WindowEvent e) {
                                timer.stop();
                                frame.dispose();
                            }
                        }
                );
                // Circle panel for time chunks
                circlePanel = new JPanel();
                circlePanel.setLayout(new FlowLayout());
                circlePanel.setBackground(Color.WHITE);
                for (int i = 0; i < circles.length; i++) {
                    circles[i] = new JLabel("\u25CF"); // Unicode for a filled circle
                    circles[i].setFont(new Font("Yu Gothic UI Light", Font.PLAIN, 24));
                    circles[i].setForeground(Color.LIGHT_GRAY);
                    circlePanel.add(circles[i]);
                }

                // Timer label
                timerLabel = new JLabel(formatTime(timeRemaining), SwingConstants.CENTER);
                timerLabel.setFont(new Font("Yu Gothic UI Light", Font.PLAIN, 48));

                // Task label
                taskLabel = new JLabel(taskName, SwingConstants.CENTER);
                taskLabel.setFont(new Font("Yu Gothic UI Light", Font.PLAIN, 24));

                frame.add(circlePanel, BorderLayout.NORTH);
                frame.add(timerLabel, BorderLayout.CENTER);
                frame.add(taskLabel, BorderLayout.SOUTH);

                frame.revalidate();
                frame.repaint();

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
            }

            private String formatTime(int seconds) {
                int mins = seconds / 60;
                int secs = seconds % 60;
                return String.format("%02d:%02d", mins, secs);
            }

            private void updateCircles() {
                int completedChunks = (20 * 60 - timeRemaining) / CHUNK_SIZE;
                for (int i = 0; i < circles.length; i++) {
                    if (i < completedChunks) {
                        circles[i].setForeground(Color.LIGHT_GRAY);
                    } else {
                        var lightGreen = new Color(192, 219, 165);
                        circles[i].setForeground(lightGreen);
                    }
                }
            }
        }
