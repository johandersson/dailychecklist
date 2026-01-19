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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;

public class TestMain {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    private ServerSocket testServer;
    private Thread serverThread;

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    @AfterEach
    void tearDown() throws IOException {
        if (testServer != null) {
            testServer.close();
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    @Test
    void testMain() {
        // Just test that main doesn't throw exception in headless mode
        Main.main(new String[0]);
    }

    @Test
    void testIsPortUsedByOurProgram_True() throws Exception {
        int testPort = 9998; // Use a different port for test
        testServer = new ServerSocket(testPort);
        serverThread = new Thread(() -> {
            try {
                Socket client = testServer.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                String message = in.readLine();
                if ("verify_daily_checklist".equals(message)) {
                    out.println("daily_checklist_ack");
                }
                client.close();
            } catch (IOException e) {
                // Ignore
            }
        });
        serverThread.start();

        // Give time for server to start
        Thread.sleep(100);

        assertTrue(Main.isPortUsedByOurProgram(testPort));
    }

    @Test
    void testIsPortUsedByOurProgram_False() throws Exception {
        int testPort = 9997;
        testServer = new ServerSocket(testPort);
        serverThread = new Thread(() -> {
            try {
                Socket client = testServer.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                String message = in.readLine();
                if ("verify_daily_checklist".equals(message)) {
                    out.println("other_response");
                }
                client.close();
            } catch (IOException e) {
                // Ignore
            }
        });
        serverThread.start();

        Thread.sleep(100);

        assertFalse(Main.isPortUsedByOurProgram(testPort));
    }

    @Test
    void testIsPortUsedByOurProgram_NoServer() {
        int testPort = 9996; // Assume not in use
        assertFalse(Main.isPortUsedByOurProgram(testPort));
    }

    @Test
    void testFindAvailablePort() throws Exception {
        // Find an available port
        ServerSocket socket = Main.findAvailablePort();
        assertNotNull(socket);
        int port = socket.getLocalPort();
        assertTrue(port >= 10000 && port <= 10100);
        socket.close();
    }
}