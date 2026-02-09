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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.undo.UndoManager;

/**
 * Dialog for adding/editing notes on tasks and subtasks.
 * Supports max 1000 characters with live character count and undo/redo (Ctrl+Z/Ctrl+Y).
 * Press Ctrl+S to save and close.
 */
@SuppressWarnings("serial")
public class NoteDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final int MAX_CHARACTERS = 1000;
    
    private JTextArea noteArea;
    private JLabel wordCountLabel;
    private String resultNote;
    private boolean confirmed = false;
    private UndoManager undoManager;

    @SuppressWarnings("this-escape")
    public NoteDialog(JFrame parent, String taskName, String currentNote) {
        super(parent, "Add/Edit Note", true);
        this.resultNote = currentNote;
        
        setLayout(new BorderLayout());
        setSize(600, 400);
        setLocationRelativeTo(parent);
        
        // Title panel
        JPanel titlePanel = createTitlePanel(taskName);
        add(titlePanel, BorderLayout.NORTH);
        
        // Text area with scroll pane
        JPanel editorPanel = createEditorPanel(currentNote);
        add(editorPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
        
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private JPanel createTitlePanel(String taskName) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(0, 123, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        
        JLabel titleLabel = new JLabel("Note for: " + taskName);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getHeader1Font());
        panel.add(titleLabel, BorderLayout.WEST);
        
        return panel;
    }

    private JPanel createEditorPanel(String currentNote) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        
        // Create text area with undo/redo support
        noteArea = new JTextArea(currentNote != null ? currentNote : "");
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setFont(FontManager.getTaskListFont());
        
        // Add document filter to enforce character limit
        ((AbstractDocument) noteArea.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                if ((fb.getDocument().getLength() + string.length()) <= MAX_CHARACTERS) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) {
                    super.replace(fb, offset, length, text, attrs);
                    return;
                }
                int currentLength = fb.getDocument().getLength();
                int newLength = currentLength - length + text.length();
                if (newLength <= MAX_CHARACTERS) {
                    super.replace(fb, offset, length, text, attrs);
                } else {
                    // Trim the text to fit within the limit
                    int allowedLength = MAX_CHARACTERS - (currentLength - length);
                    if (allowedLength > 0) {
                        super.replace(fb, offset, length, text.substring(0, allowedLength), attrs);
                    }
                }
            }
        });
        
        // Add undo/redo support
        undoManager = new UndoManager();
        noteArea.getDocument().addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                undoManager.addEdit(e.getEdit());
            }
        });
        
        // Bind Ctrl+Z for undo and Ctrl+Y for redo
        noteArea.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "undo");
        noteArea.getActionMap().put("undo", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                    updateCharCount();
                }
            }
        });
        
        noteArea.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "redo");
        noteArea.getActionMap().put("redo", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                    updateCharCount();
                }
            }
        });
        
        // Bind Ctrl+S to save and close
        noteArea.getInputMap().put(KeyStroke.getKeyStroke("control S"), "save");
        noteArea.getActionMap().put("save", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveAndClose();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(noteArea);
        scrollPane.setPreferredSize(new Dimension(580, 280));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Character count label
        wordCountLabel = new JLabel();
        wordCountLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        updateCharCount();
        panel.add(wordCountLabel, BorderLayout.SOUTH);
        
        // Add document listener to update character count
        noteArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateCharCount();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateCharCount();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateCharCount();
            }
        });
        
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton saveButton = new JButton("Save (Ctrl+S)");
        saveButton.setFont(FontManager.getButtonFont());
        saveButton.addActionListener(e -> saveAndClose());
        
        JButton clearButton = new JButton("Clear Note");
        clearButton.setFont(FontManager.getButtonFont());
        clearButton.setToolTipText("Clear and save the note as empty");
        clearButton.addActionListener(e -> {
            resultNote = null;
            confirmed = true;
            dispose();
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(FontManager.getButtonFont());
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
        
        panel.add(saveButton);
        panel.add(clearButton);
        panel.add(cancelButton);
        
        return panel;
    }

    private void saveAndClose() {
        String text = noteArea.getText();
        // No need to validate since DocumentFilter enforces the limit
        resultNote = text.trim().isEmpty() ? null : text;
        confirmed = true;
        dispose();
    }

    private void updateCharCount() {
        String text = noteArea.getText();
        int charCount = text == null ? 0 : text.length();
        wordCountLabel.setText(charCount + "/" + MAX_CHARACTERS + " characters");
        
        if (charCount > MAX_CHARACTERS) {
            wordCountLabel.setForeground(Color.RED);
        } else if (charCount > MAX_CHARACTERS * 0.9) {
            wordCountLabel.setForeground(new Color(255, 140, 0)); // Orange
        } else {
            wordCountLabel.setForeground(Color.BLACK);
        }
    }

    private boolean validateCharCount(String text) {
        int charCount = text == null ? 0 : text.length();
        if (charCount > MAX_CHARACTERS) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Note exceeds maximum character limit of " + MAX_CHARACTERS + " characters.\nCurrent count: " + charCount + " characters.",
                "Validation Error",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
        return true;
    }

    /**
     * Show the dialog and return the note if confirmed, or null if cancelled.
     * @param parent Parent frame
     * @param taskName Name of the task
     * @param currentNote Current note value (can be null)
     * @return The note text if confirmed, null if cancelled or cleared
     */
    public static String showDialog(JFrame parent, String taskName, String currentNote) {
        NoteDialog dialog = new NoteDialog(parent, taskName, currentNote);
        dialog.setVisible(true);
        return dialog.confirmed ? dialog.resultNote : currentNote;
    }
}
