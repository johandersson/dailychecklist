import javax.swing.*;

public class SearchListPanel extends JScrollPane {
    public final JList<Object> unifiedList;
    public final DefaultListModel<Object> unifiedModel;

    public SearchListPanel() {
        unifiedModel = new DefaultListModel<>();
        unifiedList = new JList<>(unifiedModel);
        setViewportView(unifiedList);
        setBorder(javax.swing.BorderFactory.createEmptyBorder());
    }
}
