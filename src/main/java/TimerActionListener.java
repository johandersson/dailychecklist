import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Timer;

public class TimerActionListener implements ActionListener {
    private int timeRemaining;
    private Timer timer;
    private JLabel timerLabel;
    private Runnable updateCircles;

    public TimerActionListener(int timeRemaining, Timer timer, JLabel timerLabel, Runnable updateCircles) {
        this.timeRemaining = timeRemaining;
        this.timer = timer;
        this.timerLabel = timerLabel;
        this.updateCircles = updateCircles;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (timeRemaining > 0) {
            timeRemaining--;
            timerLabel.setText(formatTime(timeRemaining));
            if (updateCircles != null) updateCircles.run();
        } else {
            timer.stop();
            timerLabel.setText("Time's up!");
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Time's up!", "Focus Timer", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }
}
