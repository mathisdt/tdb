package org.zephyrsoft.tdb;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.sql.Date;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class AboGUI extends JFrame implements TableModelListener {

    private JTable table = null;
    private TableModel model = null;
    private Vector headers = null;
    private Vector data = null;
    
    protected int[] breite;
    
    protected JPopupMenu popup = null;
    protected JScrollPane scrollpane = null;
    
    public AboGUI(boolean maximize) {
        super("Abonnements");
        
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Hauptteil: Tabelle erstellen und mit Daten füllen
        headers = new Vector();
        headers.addElement("Erstellung");
        headers.addElement("Besteller");
        headers.addElement("Klasse");
        headers.addElement("Abo aktiv");
        headers.addElement("Bemerkung");
        headers.addElement("ID");
        headers.addElement("Medium");
        
        data = new Vector();
        ResultSet rs = DB.getInstance().getAbos();
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int cols = rsmd.getColumnCount();
            while (rs.next()) {
            	Vector objects = new Vector();
                for (int i = 0; i < cols; i++) {
                    Object now = rs.getObject(i + 1);
                    try {
                        if (i==3 && (Integer.valueOf(String.valueOf(now)).intValue()==1)) {
                            objects.addElement(new Boolean(true));
                        } else if (i==3 && (Integer.valueOf(String.valueOf(now)).intValue()==0)) {
                            objects.addElement(new Boolean(false));
                        } else {
                            objects.addElement(now);
                        }
                    } catch(NumberFormatException nfe) {
                        objects.addElement(now);
                    }
                }
                data.addElement(objects);
            }
            Vector objects = new Vector();
            objects.addElement("jetzt");
            for (int i = 0; i < cols-1; i++) {
                if (i==2) {
                    objects.addElement(new Boolean(true));
                } else if (i==4) {
                	objects.addElement("neu");
                } else if (i==5) {
                	objects.addElement("Kassette");
                } else {
                	objects.addElement("");
                }
            }
            data.addElement(objects);
        } catch(SQLException sqlex) {
            sqlex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ein Fehler trat beim Einlesen der Abos auf.", "Fehler", JOptionPane.ERROR_MESSAGE);
        } finally {
        	DB.closeResultSet(rs);
        }
        
        model = new DefaultTableModel(data, headers) {
            public boolean isCellEditable(int row, int col) {
                if (col==0 || (col==3 && row==getRowCount()-1) || col==5) {
                    return false;
                } else {
                    return true;
                }
            }
            
            public Class getColumnClass(int c) {
                return (getValueAt(0, c)==null ? String.class : getValueAt(0, c).getClass());
            }
        };
        model.addTableModelListener(this);
        table = new JTable(model);
        scrollpane = new JScrollPane(table);
        
        TableColumn column = table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("ID"));
        column.setResizable(false);
        column.setMaxWidth(0);
        column.setMinWidth(0);
        column.setPreferredWidth(0);
        column.setWidth(0);
                
        final JTextField textfield = new JTextField();
        textfield.setBackground(Color.GREEN);
        
        popup = new JPopupMenu();
        JMenuItem titel = new JMenuItem("mögliche Aktionen");
        titel.setEnabled(false);
        titel.updateUI();
        popup.add(titel);
        popup.addSeparator();
        
        JMenuItem copy = new JMenuItem("markiertes Abo duplizieren");
        copy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int copyreally = JOptionPane.showConfirmDialog(AboGUI.this, "Wirklich dieses Abo duplizieren?", "Sicherheitsabfrage", JOptionPane.OK_CANCEL_OPTION);
                if (copyreally == JOptionPane.OK_OPTION) {
                    int selrow = getSelectedRow();
                    DB.getInstance().insertAbo(String.valueOf(model.getValueAt(selrow, 1)), String.valueOf(model.getValueAt(selrow, 2)), String.valueOf(model.getValueAt(selrow, 4)), String.valueOf(model.getValueAt(selrow, 6)));
                    reloadData();
                    setSelectedID(-2);
                }
            }
        });
        popup.add(copy);
        
        JMenuItem remove = new JMenuItem("markiertes Abo löschen");
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int deletereally = JOptionPane.showConfirmDialog(AboGUI.this, "Wirklich dies Abo unwiderruflich löschen?\nZum Aussetzen kann man \"Abo aktiv\" deselektieren!", "Sicherheitsabfrage", JOptionPane.OK_CANCEL_OPTION);
                if (deletereally == JOptionPane.OK_OPTION) {
                    int selrow = getSelectedRow();
                    DB.getInstance().deleteAbo(Integer.valueOf(String.valueOf(model.getValueAt(table.getSelectedRow(), model.getColumnCount()-1))).intValue());
                    reloadData();
                    setSelectedRow(selrow);
                }
            }
        });
        popup.add(remove);
        
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
            	if (me.getButton()!=MouseEvent.BUTTON1 && !table.isEditing()) {
                    int x = me.getX();
                    int y = me.getY();
                    // Zeile an (x/y) finden und markieren
                    int proZeile = table.getRowHeight();
                    int zeile = (int)Math.floor(y/proZeile);
                    table.getSelectionModel().setSelectionInterval(zeile, zeile);
                    Point left_top = scrollpane.getViewport().getViewPosition();
                    Dimension extents = scrollpane.getViewport().getExtentSize();
                    if (x + popup.getWidth() - left_top.getX() > extents.getWidth()) {
                        x -= popup.getWidth();
                    }
                    if (y + popup.getHeight() - left_top.getY() > extents.getHeight()) {
                        y -= popup.getHeight();
                    }
                    if (zeile != table.getRowCount()-1) {
                    		popup.show(me.getComponent(), x, y);
                    }
                }
            }
            public void mousePressed(MouseEvent me) {
                mouseClicked(me);
            }
            public void mouseReleased(MouseEvent me) {
                mouseClicked(me);
            }
        });
        
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.getTableHeader().setReorderingAllowed(false);
        
        table.getColumn("Erstellung").setCellRenderer(new DefaultTableCellRenderer() {
            SimpleDateFormat formatter;
            public void setValue(Object value) {
                if (formatter==null) {
                    formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                }
                try {
                    setText((value == null) ? "" : formatter.format(value));
                } catch(IllegalArgumentException ex) {
                    setText(String.valueOf(value));
                }
            }
        });
        
        String[] items = { "Kassette", "Audio-CD", "MP3-CD" };
        JComboBox combobox = new JComboBox(items);
        table.getColumn("Medium").setCellEditor(new DefaultCellEditor(combobox));

        content.add(scrollpane, BorderLayout.CENTER);
        
        setContentPane(content);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing() {
                dispose();
            }
        });
        pack();
        setLocation(100, 80);
        setSize(800, 600);
        show();
        if (maximize) {
            setExtendedState(MAXIMIZED_BOTH);
        }
    }
    
    private void reloadData() {
        // Layout speichern
        breite = new int[table.getColumnModel().getColumnCount()];
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            breite[i] = table.getColumnModel().getColumn(i).getWidth();
        }
        // Daten holen
        data = new Vector();
        ResultSet rs = DB.getInstance().getAbos();
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int cols = rsmd.getColumnCount();
            while (rs.next()) {
                Vector objects = new Vector();
                for (int i = 0; i < cols; i++) {
                    Object now = rs.getObject(i + 1);
                    try {
                        if (i==3 && (Integer.valueOf(String.valueOf(now)).intValue()==1)) {
                            objects.addElement(new Boolean(true));
                        } else if (i==3 && (Integer.valueOf(String.valueOf(now)).intValue()==0)) {
                            objects.addElement(new Boolean(false));
                        } else {
                            objects.addElement(now);
                        }
                    } catch(NumberFormatException nfe) {
                        objects.addElement(now);
                    }
                }
                data.addElement(objects);
            }
            Vector objects = new Vector();
            objects.addElement("jetzt");
            for (int i = 0; i < cols-1; i++) {
                if (i==2) {
                    objects.addElement(new Boolean(true));
                } else if (i==4) {
                	objects.addElement("neu");
                } else if (i==5) {
                	objects.addElement("Kassette");
                } else {
                	objects.addElement("");
                }
            }
            data.addElement(objects);
        } catch(SQLException sqlex) {
            sqlex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ein Fehler trat beim Einlesen der Abos auf.", "Fehler", JOptionPane.ERROR_MESSAGE);
        } finally {
        	DB.closeResultSet(rs);
        }
        
        model = new DefaultTableModel(data, headers) {
            public boolean isCellEditable(int row, int col) {
                if (col==0 || (col==3 && row==getRowCount()-1) || col==5) {
                    return false;
                } else {
                    return true;
                }
            }

            public Class getColumnClass(int c) {
                return (getValueAt(0, c)==null ? String.class : getValueAt(0, c).getClass());
            }
        };
        model.addTableModelListener(this);
        table.setModel(model);
        
        TableColumn column = table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("ID"));
        column.setResizable(false);
        column.setMaxWidth(0);
        column.setMinWidth(0);
        column.setPreferredWidth(0);
        column.setWidth(0);
        
        table.getColumn("Erstellung").setCellRenderer(new DefaultTableCellRenderer() {
            SimpleDateFormat formatter;
            public void setValue(Object value) {
                if (formatter==null) {
                    formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                }
                try {
                    setText((value == null) ? "" : formatter.format(value));
                } catch(IllegalArgumentException ex) {
                    setText(String.valueOf(value));
                }
            }
        });
        
        String[] items = { "Kassette", "Audio-CD", "MP3-CD" };
        JComboBox combobox = new JComboBox(items);
        table.getColumn("Medium").setCellEditor(new DefaultCellEditor(combobox));

        // Breiten wieder setzen
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumn(headers.elementAt(i)).setPreferredWidth(breite[i]);
        }
    }
    
    public void tableChanged(TableModelEvent tme) {
        int row = tme.getFirstRow();
        int column = tme.getColumn();
        String columnName = model.getColumnName(column);
        String id = String.valueOf(model.getValueAt(row, 5));
        
        String besteller = String.valueOf(model.getValueAt(row, 1));
        String klasse = String.valueOf(model.getValueAt(row, 2));
        Boolean ist_aktiv = (Boolean)model.getValueAt(row, 3);
        String bemerkung = String.valueOf(model.getValueAt(row, 4));
        String medium = String.valueOf(model.getValueAt(row, 6));
        
        
        if (id.equalsIgnoreCase("neu")) {
            // Einfügen
            DB.getInstance().insertAbo(besteller, klasse, bemerkung, medium);
            reloadData();
            setSelectedID(-2);
        } else {
            // Aktualisieren
            DB.getInstance().updateAbo(Integer.valueOf(id).intValue(), besteller, klasse, ist_aktiv.booleanValue(), bemerkung, medium);
            int selrow = getSelectedRow();
            reloadData();
            setSelectedRow(selrow);
        }
        
    }
    
    /* -1 keine Selektion, -2 maximale ID, 0-... diese ID selektieren */
    private void setSelectedID(int id) {
        if (id == -1) {
            table.getSelectionModel().clearSelection();
        } else if (id == -2) {
            int bishermax = -1;
            for (int i = 0; i < model.getRowCount(); i++) {
                int actid;
                try {
                    actid = Integer.valueOf(String.valueOf(model.getValueAt(i, model.getColumnCount()-1))).intValue();
                } catch(NumberFormatException nfe) {
                    actid = -2;
                }
                if (actid > bishermax) {
                    bishermax = actid;
                }
            }
            setSelectedID(bishermax);
        } else if (id >= 0) {
            for (int i = 0; i < model.getRowCount(); i++) {
                int actid;
                try {
                    actid = Integer.valueOf(String.valueOf(model.getValueAt(i, model.getColumnCount()-1))).intValue();
                } catch(NumberFormatException nfe) {
                    actid = -2;
                }
                if (actid == id) {
                    table.getSelectionModel().setSelectionInterval(i, i);
                }
            }
        }
    }
    
    private int getSelectedRow() {
        return table.getSelectionModel().getMinSelectionIndex();
    }
    
    private void setSelectedRow(int row) {
        table.getSelectionModel().setSelectionInterval(row, row);
    }
    
}
