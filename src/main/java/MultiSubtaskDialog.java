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
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for showing a multi-line dialog to add multiple subtasks at once.
 */
public final class MultiSubtaskDialog {
    private MultiSubtaskDialog() {}
    
    /**
     * Shows a dialog for entering multiple subtask names (one per line).
     * 
     * @param parent The parent component for the dialog
     * @param parentTaskName The name of the parent task
     * @return List of subtask names (trimmed, non-empty), or empty list if cancelled
     */
    public static List<String> show(Component parent, String parentTaskName) {
        JTextArea textArea = new JTextArea(5, 35);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(FontManager.getTaskListFont());
        
        // Add undo/redo support
        final javax.swing.undo.UndoManager undoManager = new javax.swing.undo.UndoManager();
        textArea.getDocument().addUndoableEditListener(undoManager);
        
        // Bind Ctrl+Z for undo and Ctrl+Y for redo
        textArea.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("control Z"), "undo");
        textArea.getActionMap().put("undo", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });
        
        textArea.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("control Y"), "redo");
        textArea.getActionMap().put("redo", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        String prompt = "Add subtasks to: " + parentTaskName + "\n(Enter one subtask per line. Prefix with # for heading)";
        int result = JOptionPane.showConfirmDialog(
            parent, 
            scrollPane, 
            prompt, 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE
        );
        
        List<String> subtaskNames = new ArrayList<>();
        
        if (result == JOptionPane.OK_OPTION) {
            String input = textArea.getText();
            if (input != null && !input.trim().isEmpty()) {
                String[] lines = input.split("\\r?\\n");
                
                // Limit to prevent excessive batch additions
                final int MAX_BATCH_SUBTASKS = 1000;
                if (lines.length > MAX_BATCH_SUBTASKS) {
                    JOptionPane.showMessageDialog(
                        parent,
                        "Too many lines (" + lines.length + "). Maximum allowed is " + MAX_BATCH_SUBTASKS + " subtasks at once.",
                        "Limit Exceeded",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return new ArrayList<>();
                }
                
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        subtaskNames.add(trimmed);
                    }
                }
            }
        }
        
        return subtaskNames;
    }
}
