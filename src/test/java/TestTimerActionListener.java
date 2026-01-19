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
import static org.junit.jupiter.api.Assertions.*;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.Timer;

public class TestTimerActionListener {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void testConstructor() {
        JLabel label = new JLabel();
        Timer timer = new Timer(1000, null);
        Runnable updateCircles = () -> {};
        TimerActionListener listener = new TimerActionListener(60, timer, label, updateCircles);
        assertEquals(60, listener.getTimeRemaining());
    }

    @Test
    void testActionPerformedCountdown() {
        JLabel label = new JLabel();
        Timer timer = new Timer(1000, null);
        boolean[] updated = {false};
        Runnable updateCircles = () -> updated[0] = true;
        TimerActionListener listener = new TimerActionListener(10, timer, label, updateCircles);
        ActionEvent event = new ActionEvent(timer, 0, "");

        listener.actionPerformed(event);
        assertEquals(9, listener.getTimeRemaining());
        assertTrue(updated[0]);
        assertEquals("00:09", label.getText());
    }

    @Test
    void testActionPerformedTimeUp() {
        JLabel label = new JLabel();
        Timer timer = new Timer(1000, null);
        Runnable updateCircles = () -> {};
        TimerActionListener listener = new TimerActionListener(1, timer, label, updateCircles);
        ActionEvent event = new ActionEvent(timer, 0, "");

        // First call: decrement to 0, set "00:00"
        listener.actionPerformed(event);
        assertEquals(0, listener.getTimeRemaining());
        assertEquals("00:00", label.getText());

        // Second call: time up, set "Time's up!"
        listener.actionPerformed(event);
        assertEquals(0, listener.getTimeRemaining());
        assertEquals("Time's up!", label.getText());
    }

    @Test
    void testFormatTime() {
        // formatTime is private, so test indirectly through actionPerformed
        JLabel label = new JLabel();
        Timer timer = new Timer(1000, null);
        Runnable updateCircles = () -> {};
        TimerActionListener listener = new TimerActionListener(125, timer, label, updateCircles);
        ActionEvent event = new ActionEvent(timer, 0, "");

        listener.actionPerformed(event);
        assertEquals("02:04", label.getText()); // 124 seconds = 2:04
    }

    @Test
    void testUpdateCirclesNull() {
        JLabel label = new JLabel();
        Timer timer = new Timer(1000, null);
        TimerActionListener listener = new TimerActionListener(10, timer, label, null);
        ActionEvent event = new ActionEvent(timer, 0, "");

        // Should not throw exception
        listener.actionPerformed(event);
        assertEquals(9, listener.getTimeRemaining());
    }
}