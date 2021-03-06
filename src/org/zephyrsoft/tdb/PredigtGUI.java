package org.zephyrsoft.tdb;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.table.*;

import org.zephyrsoft.util.*;

public class PredigtGUI extends JFrame implements TableModelListener {

    private JTable table = null;
    private TableModel model = null;
    private Vector headers = null;
    private Vector data = null;
    
    protected int[] breite;
    
    protected JPopupMenu popup = null;
    protected JScrollPane scrollpane = null;
    
    public PredigtGUI(boolean maximize) {
        super("Predigten");
        
        
        
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Hauptteil: Tabelle erstellen und mit Daten füllen
        headers = new Vector();
        headers.addElement("Datum");
        headers.addElement("Klasse");
        headers.addElement("Sprecher");
        headers.addElement("Veranstaltung");
        headers.addElement("Thema");
        headers.addElement("Bemerkung");
        headers.addElement("ID");
        
        data = DB.getInstance().getPredigten();
        if (!NullSafeUtils.safeEquals(DB.getProperty(DB.PROPERTY_NAMES.ITEM_SOURCE), "files")) {
        	// nur wenn die Quelle der Predigten nicht tatsächliche Dateien sind:
	        Vector objects = new Vector();
	        objects.addElement(new Date((new java.util.Date()).getTime()));
	        for (int i = 1; i < DB.getInstance().getPredigtTableColumnCount()-1; i++) {
	            objects.addElement("");
	        }
	        objects.addElement("neu");
	        data.addElement(objects);
        }
        
        model = new DefaultTableModel(data, headers) {
            public boolean isCellEditable(int row, int col) {
                if (col==getColumnCount()-1) {
                    return false;
                } else if (NullSafeUtils.safeEquals(DB.getProperty(DB.PROPERTY_NAMES.ITEM_SOURCE), "files")) {
                    return col==4;
                } else {
                	return true;
                }
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
        model.addTableModelListener(this);
        table = new JTable(model);
        scrollpane = new JScrollPane(table);
        
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
        
        final JTextField textfield = new JTextField();
        textfield.setBackground(Color.GREEN);
        
        if (!NullSafeUtils.safeEquals(DB.getProperty(DB.PROPERTY_NAMES.ITEM_SOURCE), "files")) {
        	// nur wenn die Quelle der Predigten nicht tatsächliche Dateien sind:
	        popup = new JPopupMenu();
	        JMenuItem titel = new JMenuItem("mögliche Aktionen");
	        titel.setEnabled(false);
	        titel.updateUI();
	        popup.add(titel);
	        popup.addSeparator();
	        
	        JMenuItem copy = new JMenuItem("markierte Predigt duplizieren");
	        copy.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent ae) {
	                int copyreally = JOptionPane.showConfirmDialog(PredigtGUI.this, "Wirklich diese Predigt" + ( !String.valueOf(model.getValueAt(table.getSelectedRow(), 0)).equals("null") ? " vom " + String.valueOf(model.getValueAt(table.getSelectedRow(), 0)) : "") + " duplizieren?", "Sicherheitsabfrage", JOptionPane.OK_CANCEL_OPTION);
	                if (copyreally == JOptionPane.OK_OPTION) {
	                    int selrow = getSelectedRow();
	                    DB.getInstance().insertPredigt(String.valueOf(model.getValueAt(selrow, 0)), String.valueOf(model.getValueAt(selrow, 1)), String.valueOf(model.getValueAt(selrow, 2)), String.valueOf(model.getValueAt(selrow, 3)), String.valueOf(model.getValueAt(selrow, 4)), String.valueOf(model.getValueAt(selrow, 5)));
	                    reloadData();
	                    setSelectedID(-2);
	                }
	            }
	        });
	        popup.add(copy);
	        
	        JMenuItem remove = new JMenuItem("markierte Predigt löschen");
	        remove.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent ae) {
	                int deletereally = JOptionPane.showConfirmDialog(PredigtGUI.this, "Wirklich diese Predigt" + ( !String.valueOf(model.getValueAt(table.getSelectedRow(), 0)).equals("null") ? " vom " + String.valueOf(model.getValueAt(table.getSelectedRow(), 0)) : "") + " unwiderruflich löschen?", "Sicherheitsabfrage", JOptionPane.OK_CANCEL_OPTION);
	                if (deletereally == JOptionPane.OK_OPTION) {
	                    int selrow = getSelectedRow();
	                    DB.getInstance().deletePredigt(Integer.valueOf(String.valueOf(model.getValueAt(table.getSelectedRow(), model.getColumnCount()-1))).intValue());
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
        }
	    
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.getTableHeader().setReorderingAllowed(false);
        
        content.add(scrollpane, BorderLayout.CENTER);
        
        if (NullSafeUtils.safeEquals(DB.getProperty(DB.PROPERTY_NAMES.ITEM_SOURCE), "files")) {
        	// nur wenn die Quelle der Predigten die tatsächlichen Dateien sind:
	        Timer periodicReloader = new Timer(30000, new ActionListener() {
	            public void actionPerformed(ActionEvent ae) {
	                periodicReload();
	            }
	        });
	        periodicReloader.start();
        }
        
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
    
    protected synchronized void periodicReload() {
        if (!table.isEditing()) {
            int selrow = getSelectedRow();
            reloadData();
            setSelectedRow(selrow);
        }
    }
    
    private void reloadData() {
        // Layout speichern
        breite = new int[table.getColumnModel().getColumnCount()];
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            breite[i] = table.getColumnModel().getColumn(i).getWidth();
        }
        // Daten holen
        data = DB.getInstance().getPredigten();
        if (!NullSafeUtils.safeEquals(DB.getProperty(DB.PROPERTY_NAMES.ITEM_SOURCE), "files")) {
        	// nur wenn die Quelle der Predigten nicht tatsächliche Dateien sind:
	        Vector objects = new Vector();
	        objects.addElement(new Date((new java.util.Date()).getTime()));
	        for (int i = 1; i < DB.getInstance().getPredigtTableColumnCount()-1; i++) {
	            objects.addElement("");
	        }
	        objects.addElement("neu");
	        data.addElement(objects);
        }
        model = new DefaultTableModel(data, headers) {
            public boolean isCellEditable(int row, int col) {
            	if (col==getColumnCount()-1) {
                    return false;
                } else if (NullSafeUtils.safeEquals(DB.getProperty(DB.PROPERTY_NAMES.ITEM_SOURCE), "files")) {
                    return col==4;
                } else {
                	return true;
                }
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
        model.addTableModelListener(this);
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
        
        // Breiten wieder setzen
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumn(headers.elementAt(i)).setPreferredWidth(breite[i]);
        }
    }
    
    public void tableChanged(TableModelEvent tme) {
        int row = tme.getFirstRow();
        int column = tme.getColumn();
        String columnName = model.getColumnName(column);
        String id = String.valueOf(model.getValueAt(row, model.getColumnCount()-1));
        
        String datum = String.valueOf(model.getValueAt(row, 0));
        String klasse = String.valueOf(model.getValueAt(row, 1));
        String sprecher = String.valueOf(model.getValueAt(row, 2));
        String name = String.valueOf(model.getValueAt(row, 3));
        String thema = String.valueOf(model.getValueAt(row, 4));
        String bemerkung = String.valueOf(model.getValueAt(row, 5));
        
        if (id.equalsIgnoreCase("neu")) {
            // Einfügen
            DB.getInstance().insertPredigt(datum, klasse, sprecher, name, thema, bemerkung);
            reloadData();
            setSelectedID(-2);
        } else {
            // Aktualisieren
            DB.getInstance().updatePredigt(Integer.valueOf(id).intValue(), datum, klasse, sprecher, name, thema, bemerkung);
            int selid = getSelectedID();
            reloadData();
            setSelectedID(selid);
        }
        
    }
    
    private int getSelectedID() {
        return Integer.valueOf(String.valueOf(model.getValueAt(table.getSelectedRow(), model.getColumnCount()-1))).intValue();
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
