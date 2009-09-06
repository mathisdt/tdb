package org.zephyrsoft.tdb;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

public class PredigtCellEditor extends AbstractCellEditor implements TableCellEditor,
        ActionListener {
    
    String currentValue;
    JButton button;
    PredigtChooser chooser;
    
    public PredigtCellEditor(JFrame parent) {
        button = new JButton("Klicken zum Wählen");
        button.addActionListener(this);
        button.setBorderPainted(false);

        chooser = new PredigtChooser(parent, this); 
    }
    
    public void newValue(String newvalue) {
        currentValue = newvalue;
        fireEditingStopped();
    }

    public void actionPerformed(ActionEvent e) {
        chooser.setVisible(true);
    }

    // Implement the one CellEditor method that AbstractCellEditor doesn't.
    public Object getCellEditorValue() {
        return currentValue;
    }

    //Implement the one method defined by TableCellEditor.
    public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {
        currentValue = (String) value;
        return button;
    }
}
