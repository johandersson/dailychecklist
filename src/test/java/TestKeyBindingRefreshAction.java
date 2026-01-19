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

public class TestKeyBindingRefreshAction {
    @Test
    void testActionPerformed() {
        boolean[] called = {false};
        Runnable updateTasks = () -> called[0] = true;
        KeyBindingRefreshAction action = new KeyBindingRefreshAction(updateTasks);
        ActionEvent event = new ActionEvent(this, 0, "");

        action.actionPerformed(event);
        assertTrue(called[0]);
    }
}