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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        int[] possiblePorts = new int[102]; // 9999 + 100
        possiblePorts[0] = 9999;
        for (int i = 1; i < possiblePorts.length; i++) {
            possiblePorts[i] = 9999 + i;
        }

        ServerSocket tempSocket = null;

        // First, check if any existing instance is running on any possible port
        for (int port : possiblePorts) {
            try (ServerSocket testSocket = new ServerSocket(port)) {
                // Port is free, continue
            } catch (IOException e) {
                // Port is bound, check if it's our program
                if (isPortUsedByOurProgram(port)) {
                    // Found our program, bring to front
                    JOptionPane.showMessageDialog(null, "Another instance is already running. Bringing it to the front.");
                    try (Socket socket = new Socket("localhost", port);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                        out.println("bring to front");
                    } catch (IOException ex) {
                        // Ignore
                    }
                    System.exit(0);
                }
                // Not our program, continue checking
            }
        }

        // No existing instance found, bind to the first available port
        for (int port : possiblePorts) {
            try {
                tempSocket = new ServerSocket(port);
                break; // Successfully bound
            } catch (IOException e) {
                // Port taken, try next
            }
        }

        if (tempSocket == null) {
            // Couldn't find any port, start without protection
            SwingUtilities.invokeLater(() -> {
                DailyChecklist checklist = new DailyChecklist();
                checklist.setVisible(true);

                // Add shutdown hook to properly close resources
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    checklist.shutdown();
                }));
            });
            return;
        }

        final ServerSocket serverSocket = tempSocket;

        // If here, we have a serverSocket
        SwingUtilities.invokeLater(() -> {
            DailyChecklist checklist = new DailyChecklist();
            checklist.setVisible(true);

            // Add shutdown hook to properly close resources
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                checklist.shutdown();
            }));

            // Start listener thread
            new Thread(() -> {
                try {
                    while (true) {
                        try (Socket clientSocket = serverSocket.accept();
                             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                            String message = in.readLine();
                            if ("bring to front".equals(message)) {
                                SwingUtilities.invokeLater(() -> {
                                    checklist.bringToFront();
                                });
                            } else if ("verify_daily_checklist".equals(message)) {
                                out.println("daily_checklist_ack");
                            }
                        }
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }).start();
        });
    }

    static boolean isPortUsedByOurProgram(int port) {
        try (Socket socket = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.println("verify_daily_checklist");
            String response = in.readLine();
            return "daily_checklist_ack".equals(response);
        } catch (IOException e) {
            return false;
        }
    }

    static ServerSocket findAvailablePort() {
        for (int p = 10000; p <= 10100; p++) { // Try ports 10000 to 10100
            try {
                return new ServerSocket(p);
            } catch (IOException e) {
                // Port taken, try next
            }
        }
        return null; // No port found
    }
}

