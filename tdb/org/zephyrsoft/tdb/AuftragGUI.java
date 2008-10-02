package org.zephyrsoft.tdb;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.table.*;

public class AuftragGUI extends JFrame implements TableModelListener {

    public boolean debug = false;
    
    private JTable table = null;
    private TableModel model = null;
    private Vector headers = null;
    private Vector data = null;
    
    protected int[] breite;
    
    protected JPopupMenu popup = null;
    protected JScrollPane scrollpane = null;
    
    public AuftragGUI(boolean maximize) {
        super("Aufträge");
        
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Hauptteil: Tabelle erstellen und mit Daten füllen
        headers = new Vector();
        headers.addElement("Erstellung");
        headers.addElement("Besteller");
        headers.addElement("Medium");
        headers.addElement("Predigt/Sammlung");
        headers.addElement("Fertigstellung");
        headers.addElement("Bemerkung");
        headers.addElement("ID");
        
        data = new Vector();
        ResultSet rs = DB.getInstance().getAuftraege();
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int cols = rsmd.getColumnCount();
            rs.beforeFirst();
            while (rs.next()) {
                Vector objects = new Vector();
                for (int i = 0; i < cols; i++) {
                    Object now = rs.getObject(i + 1);
                    if (i==4) {
                        if (now==null) {
                            objects.addElement("unfertig");
                        } else {
                            objects.addElement(now);
                        }
                    } else {
                        objects.addElement(now);
                    }
                }
                data.addElement(objects);
            }
            Vector objects = new Vector();
            objects.addElement("jetzt");
            for (int i = 0; i < cols-2; i++) {
                if (i==3) {
                    objects.addElement("unfertig");
                } else {
                    objects.addElement("");
                }
            }
            objects.addElement("neu");
            data.addElement(objects);
        } catch(SQLException sqlex) {
            sqlex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ein Fehler trat beim Einlesen der Aufträge auf.", "Fehler", JOptionPane.ERROR_MESSAGE);
        } finally {
        	DB.closeResultSet(rs);
        }
        
        model = new DefaultTableModel(data, headers) {
            public boolean isCellEditable(int row, int col) {
                debug("editable? - begin");
                try {
                    if (col==0 || col==4 || col==getColumnCount()-1 || Integer.valueOf(String.valueOf(getValueAt(row, getColumnCount()-1))).intValue()==-3) {
                        debug("editable? - normal line");
                        return false;
                    } else {
                        debug("editable? - normal line");
                        return true;
                    }
                } catch(NumberFormatException nfe) {
                    debug("editable? - last line");
                    // letzte Zeile, ID ist "neu"
                    if (col==0 || col==4 || col==getColumnCount()-1) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
            
            public Class getColumnClass(int c) {
                return (getValueAt(0, c)==null || getValueAt(0, c) instanceof String ? String.class : getValueAt(0, c).getClass());
            }
        };
        model.addTableModelListener(this);
        table = new JTable(model);
        scrollpane = new JScrollPane(table);
        
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.getTableHeader().setReorderingAllowed(false);
        
        TableColumn column = table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("ID"));
        column.setResizable(false);
        column.setMaxWidth(0);
        column.setMinWidth(0);
        column.setPreferredWidth(0);
        column.setWidth(0);
        column = table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("Erstellung"));
        column.setResizable(false);
        column.setMaxWidth(0);
        column.setMinWidth(0);
        column.setPreferredWidth(0);
        column.setWidth(0);
        
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
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
        };
        table.getColumn("Erstellung").setCellRenderer(renderer);
        table.getColumn("Fertigstellung").setCellRenderer(renderer);
        
        String[] items = { "Kassette", "Audio-CD", "MP3-CD" };
        JComboBox combobox = new JComboBox(items);
        table.getColumn("Medium").setCellEditor(new DefaultCellEditor(combobox));

        table.getColumn("Predigt/Sammlung").setCellRenderer(
            new DefaultTableCellRenderer() {
                public void setValue(Object value) {
                    setText((value == null || value.equals("")) ? "" : DB.getInstance().getItemShort(String.valueOf(value)));
                }
            });
        table.getColumn("Predigt/Sammlung").setCellEditor(new PredigtCellEditor(this));
        
        popup = new JPopupMenu();
        JMenuItem titel = new JMenuItem("mögliche Aktionen");
        titel.setEnabled(false);
        titel.updateUI();
        popup.add(titel);
        popup.addSeparator();
        
        JMenuItem copy = new JMenuItem("markierten Auftrag duplizieren");
        copy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int copyreally = JOptionPane.showConfirmDialog(AuftragGUI.this, "Wirklich diesen Auftrag duplizieren?", "Sicherheitsabfrage", JOptionPane.OK_CANCEL_OPTION);
                if (copyreally == JOptionPane.OK_OPTION) {
                    int selrow = getSelectedRow();
                    DB.getInstance().insertAuftrag(String.valueOf(model.getValueAt(selrow, 1)), String.valueOf(model.getValueAt(selrow, 2)), String.valueOf(model.getValueAt(selrow, 3)), String.valueOf(model.getValueAt(selrow, 5)));
                    reloadData();
                    setSelectedID(-2);
                }
            }
        });
        popup.add(copy);
        
        JMenuItem remove = new JMenuItem("markierten Auftrag löschen");
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int deletereally = JOptionPane.showConfirmDialog(AuftragGUI.this, "Wirklich diesen Auftrag unwiderruflich löschen?\nEs könnte sein, dass er schon bearbeitet wird!\n\nAnmerkung:\n    Unfertige Abo-Aufträge können nicht gelöscht werden,\n    und fertige Abo-Aufträge erscheinen wieder als unfertig,\n    wenn man sie löscht.", "Sicherheitsabfrage", JOptionPane.OK_CANCEL_OPTION);
                if (deletereally == JOptionPane.OK_OPTION) {
                    int selrow = getSelectedRow();
                    DB.getInstance().deleteAuftrag(Integer.valueOf(String.valueOf(model.getValueAt(table.getSelectedRow(), model.getColumnCount()-1))).intValue());
                    reloadData();
                    setSelectedRow(selrow);
                }
            }
        });
        popup.add(remove);
        
        JMenuItem ready = new JMenuItem("markierten Auftrag als fertig kennzeichnen");
        ready.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                ready();
            }
        });
        popup.add(ready);
        
        JMenuItem unready = new JMenuItem("markierten Auftrag als UNFERTIG kennzeichnen");
        unready.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                unready();
            }
        });
        popup.add(unready);
        
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
        
        content.add(scrollpane, BorderLayout.CENTER);
        
        Timer periodicReloader = new Timer(30000, new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                periodicReload();
            }
        });
        periodicReloader.start();
        
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
    
    protected void ready() {
        if (table.getSelectedRow() != -1 && String.valueOf(model.getValueAt(table.getSelectedRow(), 4)).equalsIgnoreCase("unfertig")) {
            int really = JOptionPane.showConfirmDialog(AuftragGUI.this, "Wirklich diesen Auftrag als erledigt markieren?", "Sicherheitsabfrage", JOptionPane.OK_CANCEL_OPTION);
            if (really == JOptionPane.OK_OPTION) {
                int selrow = getSelectedRow();
                if (Integer.valueOf(String.valueOf(model.getValueAt(selrow, 6))).intValue()==-3) {
                    DB.getInstance().setAboAuftragErledigt(String.valueOf(model.getValueAt(selrow, 1)), String.valueOf(model.getValueAt(selrow, 5)), String.valueOf(model.getValueAt(selrow, 2)), String.valueOf(model.getValueAt(selrow, 3)));
                } else {
                    DB.getInstance().setAuftragErledigt(Integer.valueOf(String.valueOf(model.getValueAt(selrow, 6))).intValue());
                }
                reloadData();
                setSelectedRow(selrow);
            }
        }
    }
    
    protected void unready() {
        if (table.getSelectedRow() != -1 && !String.valueOf(model.getValueAt(table.getSelectedRow(), 4)).equalsIgnoreCase("unfertig")) {
            int really = JOptionPane.showConfirmDialog(AuftragGUI.this, "Wirklich diesen Auftrag als UNERLEDIGT markieren?", "Sicherheitsabfrage", JOptionPane.OK_CANCEL_OPTION);
            if (really == JOptionPane.OK_OPTION) {
                int selrow = getSelectedRow();
                DB.getInstance().setAuftragUnerledigt(Integer.valueOf(String.valueOf(model.getValueAt(selrow, 6))).intValue());
                reloadData();
                setSelectedRow(selrow);
            }
        }
    }
    
    private synchronized void reloadData() {
        debug("reload!");
        // Layout speichern
        breite = new int[table.getColumnModel().getColumnCount()];
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            breite[i] = table.getColumnModel().getColumn(i).getWidth();
        }
        // Daten holen
        data = new Vector();
        ResultSet rs = DB.getInstance().getAuftraege();
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int cols = rsmd.getColumnCount();
            while (rs.next()) {
                Vector objects = new Vector();
                for (int i = 0; i < cols; i++) {
                    Object now = rs.getObject(i + 1);
                    if (i==4) {
                        if (now==null) {
                            objects.addElement("unfertig");
                        } else {
                            objects.addElement(now);
                        }
                    } else {
                        objects.addElement(now);
                    }
                }
                data.addElement(objects);
            }
            Vector objects = new Vector();
            objects.addElement("jetzt");
            for (int i = 0; i < cols-2; i++) {
                if (i==3) {
                    objects.addElement("unfertig");
                } else {
                    objects.addElement("");
                }
            }
            objects.addElement("neu");
            data.addElement(objects);
        } catch(SQLException sqlex) {
            sqlex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ein Fehler trat beim Einlesen der Aufträge auf.", "Fehler", JOptionPane.ERROR_MESSAGE);
        } finally {
        	DB.closeResultSet(rs);
        }
        
        model = new DefaultTableModel(data, headers) {
            public boolean isCellEditable(int row, int col) {
                debug("editable? - begin");
                try {
                    if (col==0 || col==4 || col==getColumnCount()-1 || Integer.valueOf(String.valueOf(getValueAt(row, getColumnCount()-1))).intValue()==-3) {
                        debug("editable? - normal line");
                        return false;
                    } else {
                        debug("editable? - normal line");
                        return true;
                    }
                } catch(NumberFormatException nfe) {
                    debug("editable? - last line");
                    // letzte Zeile, ID ist "neu"
                    if (col==0 || col==4 || col==getColumnCount()-1) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
            
            public Class getColumnClass(int c) {
                return (getValueAt(0, c)==null || getValueAt(0, c) instanceof String ? String.class : getValueAt(0, c).getClass());
            }
        };
        model.addTableModelListener(this);
        table.setModel(model);
        
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.getTableHeader().setReorderingAllowed(false);
        
        TableColumn column = table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("ID"));
        column.setResizable(false);
        column.setMaxWidth(0);
        column.setMinWidth(0);
        column.setPreferredWidth(0);
        column.setWidth(0);
        column = table.getColumnModel().getColumn(table.getColumnModel().getColumnIndex("Erstellung"));
        column.setResizable(false);
        column.setMaxWidth(0);
        column.setMinWidth(0);
        column.setPreferredWidth(0);
        column.setWidth(0);
        
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
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
        };
        table.getColumn("Erstellung").setCellRenderer(renderer);
        table.getColumn("Fertigstellung").setCellRenderer(renderer);
        
        String[] items = { "Kassette", "Audio-CD", "MP3-CD" };
        JComboBox combobox = new JComboBox(items);
        table.getColumn("Medium").setCellEditor(new DefaultCellEditor(combobox));

        table.getColumn("Predigt/Sammlung").setCellRenderer(
            new DefaultTableCellRenderer() {
                public void setValue(Object value) {
                    setText((value == null || value.equals("")) ? "" : DB.getInstance().getItemShort(String.valueOf(value)));
                }
            });
        table.getColumn("Predigt/Sammlung").setCellEditor(new PredigtCellEditor(this));
        // Breiten wieder setzen
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumn(headers.elementAt(i)).setPreferredWidth(breite[i]);
        }
    }
    
    protected synchronized void periodicReload() {
        if (!table.isEditing()) {
            int selrow = getSelectedRow();
            reloadData();
            setSelectedRow(selrow);
        }
    }
    
    public void tableChanged(TableModelEvent tme) {
        int row = tme.getFirstRow();
        int column = tme.getColumn();
        String columnName = model.getColumnName(column);
        String id = String.valueOf(model.getValueAt(row, model.getColumnCount()-1));
        
        String besteller = String.valueOf(model.getValueAt(row, 1));
        String medium = String.valueOf(model.getValueAt(row, 2));
        String was = String.valueOf(model.getValueAt(row, 3));
        String bemerkung = String.valueOf(model.getValueAt(row, 5));
        
        if (id.equalsIgnoreCase("neu")) {
            // Einfügen
            DB.getInstance().insertAuftrag(besteller, bemerkung, medium, was);
            reloadData();
            setSelectedID(-2);
        } else {
            // Aktualisieren
            DB.getInstance().updateAuftrag(Integer.valueOf(id).intValue(), besteller, bemerkung, medium, was);
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
    
    public void debug(String string) {
        if (debug) {
            System.out.println(string);
        }
    }
}
