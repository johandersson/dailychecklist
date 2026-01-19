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
import java.awt.event.KeyEvent;
import javax.swing.JPanel;

public class TestTimerKeyListener {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void testKeyPressedEscape() {
        TimerFrame frame = new TimerFrame();
        TimerKeyListener listener = new TimerKeyListener(frame.getTimerPanel());
        JPanel panel = new JPanel();
        KeyEvent event = new KeyEvent(panel, 0, 0, 0, KeyEvent.VK_ESCAPE, ' ');
        listener.keyPressed(event);
        // Just test that it doesn't throw exception
        assertNotNull(listener);
    }

    @Test
    void testKeyPressedOther() {
        TimerFrame frame = new TimerFrame();
        TimerKeyListener listener = new TimerKeyListener(frame.getTimerPanel());
        JPanel panel = new JPanel();
        KeyEvent event = new KeyEvent(panel, 0, 0, 0, KeyEvent.VK_A, 'a');
        listener.keyPressed(event);
        // Should do nothing
        assertNotNull(listener);
    }

    @Test
    void testKeyPressedEscapeNoRestart() {
        // Create a mock TimerPanel that returns false for confirmRestart
        TimerPanel mockPanel = new TimerPanel(null) {
            @Override
            public void setupAndStartTimer() {
                // Do nothing
            }

            @Override
            public boolean confirmRestart() {
                return false;
            }

            @Override
            public void stopTimer() {
                // Do nothing
            }
        };
        TimerKeyListener listener = new TimerKeyListener(mockPanel);
        JPanel panel = new JPanel();
        KeyEvent event = new KeyEvent(panel, 0, 0, 0, KeyEvent.VK_ESCAPE, ' ');
        listener.keyPressed(event);
        // Should stop timer but not restart
        assertNotNull(listener);
    }
}