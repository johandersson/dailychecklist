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
import javax.swing.JPanel;

public class TestKeyBindingAddAction {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void testActionPerformedInHeadless() {
        // In headless mode, JOptionPane is not shown, so action does nothing
        JPanel parent = new JPanel();
        TaskManager taskManager = new TaskManager(new XMLTaskRepository());
        Runnable updateTasks = () -> {};
        KeyBindingAddAction action = new KeyBindingAddAction(parent, taskManager, updateTasks);
        ActionEvent event = new ActionEvent(this, 0, "");

        // Should not throw exception
        assertDoesNotThrow(() -> action.actionPerformed(event));
    }
}