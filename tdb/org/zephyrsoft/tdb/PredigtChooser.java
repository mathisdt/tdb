package org.zephyrsoft.tdb;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class PredigtChooser extends JDialog implements ListSelectionListener {
    
    private String currentValue = ""; 
    
    private JTable table = null;
    private TableModel model = null;
    private Vector headers = null;
    private Vector data = null;
    
    private JComboBox combobox = null;
    private JButton okbutton = null;
    private JButton cancelbutton = null;
    protected PredigtCellEditor editor;
    
    public PredigtChooser(JFrame parent, PredigtCellEditor ed) {
        super(parent);
        this.editor = ed;
        setTitle("Predigt/Sammlung auswählen...");
        try {
            setAlwaysOnTop(true);
        } catch(NoSuchMethodError err) {}
        
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        String[] comboitems = {"Predigten", "Sammlungen"};
        combobox = new JComboBox(comboitems);
        combobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                reloadData(combobox.getSelectedIndex());
            }
        });
        JPanel combopanel = new JPanel(new FlowLayout());
        combopanel.add(combobox);
        content.add(combopanel, BorderLayout.NORTH);
        
        okbutton = new JButton("OK");
        okbutton.setEnabled(false);
        okbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                currentValue = ( combobox.getSelectedIndex()==0 ? "P" : "S" ) + model.getValueAt(table.getSelectedRow(), model.getColumnCount()-1);
                editor.newValue(currentValue);
                hide();
            }
        });
        cancelbutton = new JButton("Abbrechen");
        cancelbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                hide();
            }
        });
        JPanel okcancelpanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        okcancelpanel.add(cancelbutton);
        okcancelpanel.add(okbutton);
        content.add(okcancelpanel, BorderLayout.SOUTH);
        
        // Hauptteil: Tabelle erstellen und mit Predigten füllen
        headers = new Vector();
        headers.addElement("Datum");
        headers.addElement("Klasse");
        headers.addElement("Sprecher");
        headers.addElement("Veranstaltung");
        headers.addElement("Thema");
        headers.addElement("Bemerkung");
        headers.addElement("ID");
        
        data = DB.getInstance().getPredigten();
        
        model = new DefaultTableModel(data, headers) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            public Class getColumnClass(int c) {
                if (c>0 && c<getColumnCount()-1) {
                    return (getValueAt(0, c)==null ? String.class : getValueAt(0, c).getClass());
                } else if (c==getColumnCount()-1) {
                    return Integer.class;
                } else {
                    return String.class;
                }
            }
        };
        table = new JTable(model);
        
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(this);
        
        TableColumn column = table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("ID"));
        column.setResizable(false);
        column.setMaxWidth(0);
        column.setMinWidth(0);
        column.setPreferredWidth(0);
        column.setWidth(0);
        column = table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("Veranstaltung"));
        column.setResizable(false);
        column.setMaxWidth(0);
        column.setMinWidth(0);
        column.setPreferredWidth(0);
        column.setWidth(0);
        
        JScrollPane scrollpane = new JScrollPane(table);
        content.add(scrollpane, BorderLayout.CENTER);
        
        setContentPane(content);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing() {
                dispose();
            }
        });
        pack();
        setLocation(200, 150);
        setSize(600, 500);
    }
    
    private void reloadData(int what) {
        data = new Vector();
        ResultSet rs = null;
        if (what==0) {
            // lade Predigten
            data = DB.getInstance().getPredigten();
        } else {
            // lade Sammlungen
        	try {
	            rs = DB.getInstance().getSammlungen();
	            ResultSetMetaData rsmd = rs.getMetaData();
	            int cols = rsmd.getColumnCount();
	            while (rs.next()) {
	                Vector objects = new Vector();
	                for (int i = 0; i < cols; i++) {
	                    Object now = rs.getObject(i + 1);
	                    objects.addElement(now);
	                }
	                data.addElement(objects);
	            }
            } catch(SQLException sqlex) {
                sqlex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ein Fehler trat beim Einlesen der Predigten auf.", "Fehler", JOptionPane.ERROR_MESSAGE);
            } finally {
            	DB.closeResultSet(rs);
            }
        }
            
        model = new DefaultTableModel(data, headers) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            public Class getColumnClass(int c) {
                if (c>0 && c<getColumnCount()-1) {
                    return (getValueAt(0, c)==null ? String.class : getValueAt(0, c).getClass());
                } else if (c==getColumnCount()-1) {
                    return Integer.class;
                } else {
                    return String.class;
                }
            }
        };
        table.setModel(model);
        
        TableColumn column = table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("ID"));
        column.setResizable(false);
        column.setMaxWidth(0);
        column.setMinWidth(0);
        column.setPreferredWidth(0);
        column.setWidth(0);
        column = table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("Veranstaltung"));
        column.setResizable(false);
        column.setMaxWidth(0);
        column.setMinWidth(0);
        column.setPreferredWidth(0);
        column.setWidth(0);
    }
    
    public void valueChanged(ListSelectionEvent e) {
        if (table.getSelectedRowCount()==0) {
            okbutton.setEnabled(false);
        } else {
            okbutton.setEnabled(true);
        }
    }
}
