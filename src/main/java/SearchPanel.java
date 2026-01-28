import java.awt.*;
import javax.swing.*;

public class SearchPanel extends JPanel {
    public final JTextField searchField;
    public final JButton searchButton;
    public final JCheckBox searchAllWeekdayBox;

    public SearchPanel() {
        super(new FlowLayout());
        searchField = new JTextField(28);
        searchField.setFont(FontManager.getTaskListFont());
        searchButton = new JButton("Search");
        searchButton.setFont(FontManager.getButtonFont());
        searchAllWeekdayBox = new JCheckBox("Include all weekday tasks");
        searchAllWeekdayBox.setToolTipText("When checked, weekday-specific tasks for any weekday will be included in results.");
        add(new JLabel("Search:"));
        add(searchField);
        add(searchButton);
        add(searchAllWeekdayBox);
    }
}
