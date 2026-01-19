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
import org.junit.jupiter.api.Assumptions;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestTimerPanel {

    @Test
    void testConstructor() {
        TimerFrame frame = new TimerFrame();
        TimerPanel panel = new TimerPanel(frame);
        assertNotNull(panel);
    }

    @Test
    void testSetupAndStartTimerHeadless() {
        Assumptions.assumeTrue(GraphicsEnvironment.isHeadless());
        TimerFrame frame = new TimerFrame();
        TimerPanel panel = new TimerPanel(frame);
        // In headless, setupAndStartTimer does nothing
        panel.setupAndStartTimer();
        // No assertion, just that it doesn't throw
    }

    @Test
    void testStopTimer() {
        TimerFrame frame = new TimerFrame();
        TimerPanel panel = new TimerPanel(frame);
        // stopTimer should not throw
        assertDoesNotThrow(() -> panel.stopTimer());
    }

    @Test
    void testConfirmRestartHeadless() {
        Assumptions.assumeTrue(GraphicsEnvironment.isHeadless());
        TimerFrame frame = new TimerFrame();
        TimerPanel panel = new TimerPanel(frame);
        // In headless mode, confirmRestart returns true
        assertTrue(panel.confirmRestart());
    }

    @Test
    void testFormatTime() throws Exception {
        TimerFrame frame = new TimerFrame();
        TimerPanel panel = new TimerPanel(frame);
        Method formatTimeMethod = TimerPanel.class.getDeclaredMethod("formatTime", int.class);
        formatTimeMethod.setAccessible(true);
        assertEquals("05:00", formatTimeMethod.invoke(panel, 300));
        assertEquals("01:30", formatTimeMethod.invoke(panel, 90));
        assertEquals("00:05", formatTimeMethod.invoke(panel, 5));
    }

    @Test
    void testSetupAndStartTimerSetsFieldsInHeadless() throws Exception {
        Assumptions.assumeTrue(GraphicsEnvironment.isHeadless());
        TimerFrame frame = new TimerFrame();
        TimerPanel panel = new TimerPanel(frame);
        panel.setupAndStartTimer();

        Field lastTaskField = TimerPanel.class.getDeclaredField("lastTask");
        lastTaskField.setAccessible(true);
        assertEquals("Test Task", lastTaskField.get(panel));

        Field lastAmountField = TimerPanel.class.getDeclaredField("lastAmountOfMinutesForTask");
        lastAmountField.setAccessible(true);
        assertEquals("5", lastAmountField.get(panel));

        Field timeRemainingField = TimerPanel.class.getDeclaredField("timeRemaining");
        timeRemainingField.setAccessible(true);
        assertEquals(300, timeRemainingField.get(panel));
    }

    @Test
    void testStopTimerWithNullTimer() throws Exception {
        TimerFrame frame = new TimerFrame();
        TimerPanel panel = new TimerPanel(frame);
        // timer is null initially
        assertDoesNotThrow(() -> panel.stopTimer());
    }

    @Test
    void testConstructorSetsLayoutAndBackground() {
        TimerFrame frame = new TimerFrame();
        TimerPanel panel = new TimerPanel(frame);
        assertEquals(java.awt.BorderLayout.class, panel.getLayout().getClass());
        assertEquals(java.awt.Color.WHITE, panel.getBackground());
    }

    @Test
    void testTimerPanelMouseListenerConstructor() {
        TimerFrame frame = new TimerFrame();
        TimerPanel panel = new TimerPanel(frame);
        AtomicBoolean restartCalled = new AtomicBoolean(false);
        TimerPanelMouseListener listener = new TimerPanelMouseListener(panel, () -> 0, () -> restartCalled.set(true));
        assertNotNull(listener);
    }

    @Test
    void testTimerPanelMouseListenerMouseClickedWhenTimeRemaining() {
        TimerFrame frame = new TimerFrame();
        TimerPanel panel = new TimerPanel(frame);
        AtomicBoolean restartCalled = new AtomicBoolean(false);
        TimerPanelMouseListener listener = new TimerPanelMouseListener(panel, () -> 10, () -> restartCalled.set(true));
        
        // Add listener to panel
        panel.addMouseListener(listener);
        
        // Simulate mouse click
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false);
        listener.mouseClicked(event);
        
        // Should not remove listener or call restart since time > 0
        assertFalse(restartCalled.get());
        // Note: Can't easily test if listener is still attached without reflection
    }

    @Test
    void testTimerPanelMouseListenerMouseClickedWhenTimeFinished() {
        TimerFrame frame = new TimerFrame();
        TimerPanel panel = new TimerPanel(frame);
        AtomicBoolean restartCalled = new AtomicBoolean(false);
        TimerPanelMouseListener listener = new TimerPanelMouseListener(panel, () -> 0, () -> restartCalled.set(true));
        
        // Add listener to panel
        panel.addMouseListener(listener);
        
        // Simulate mouse click
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false);
        listener.mouseClicked(event);
        
        // Should call restart since time <= 0
        assertTrue(restartCalled.get());
    }

    @Test
    void testTimerPanelMouseListenerMouseClickedWhenTimeFinishedNullRestart() {
        TimerFrame frame = new TimerFrame();
        TimerPanel panel = new TimerPanel(frame);
        TimerPanelMouseListener listener = new TimerPanelMouseListener(panel, () -> 0, null);
        
        // Add listener to panel
        panel.addMouseListener(listener);
        
        // Simulate mouse click - should not throw
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false);
        assertDoesNotThrow(() -> listener.mouseClicked(event));
    }
}