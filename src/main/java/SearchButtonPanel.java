import java.awt.*;
import javax.swing.*;

public class SearchButtonPanel extends JPanel {
    public final JButton goToButton;
    public final JButton closeButton;

    public SearchButtonPanel() {
        super(new FlowLayout());
        goToButton = new JButton("Open");
        goToButton.setEnabled(false);
        goToButton.setFont(FontManager.getButtonFont());
        closeButton = new JButton("Close");
        closeButton.setFont(FontManager.getButtonFont());
        add(goToButton);
        add(closeButton);
    }
}
