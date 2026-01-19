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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntSupplier;

public class TimerPanelMouseListener extends MouseAdapter {
    private final TimerPanel panel;
    private final IntSupplier getTimeRemaining;
    private final Runnable onRestart;

    public TimerPanelMouseListener(TimerPanel panel, IntSupplier getTimeRemaining, Runnable onRestart) {
        this.panel = panel;
        this.getTimeRemaining = getTimeRemaining;
        this.onRestart = onRestart;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (getTimeRemaining.getAsInt() <= 0) {
            panel.removeMouseListener(this);
            if (onRestart != null) {
                onRestart.run();
            }
        }
    }
}