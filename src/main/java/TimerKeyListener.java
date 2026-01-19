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
 */import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class TimerKeyListener extends KeyAdapter {
    private TimerPanel timerPanel;

    public TimerKeyListener(TimerPanel timerPanel) {
        this.timerPanel = timerPanel;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            timerPanel.stopTimer();
            if (timerPanel.confirmRestart()) {
                timerPanel.setupAndStartTimer();
            }
        }
    }
}

