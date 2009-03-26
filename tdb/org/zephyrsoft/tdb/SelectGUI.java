package org.zephyrsoft.tdb;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.zephyrsoft.util.*;

/**
 * Möglichkeiten:
 *   - Abos bearbeiten
 *   - Predigten bearbeiten
 *   - Sammlungen bearbeiten
 *   - Aufträge erstellen/bearbeiten (Büchertisch)
 *   - Aufträge abarbeiten (Glaskasten)
 * 
 * @author Mathis Dirksen-Thedens
 *
 */
public class SelectGUI extends JFrame {
    
    private boolean maximize = false;

    public SelectGUI() {
        super("Tape Database");
        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createEmptyBorder(15, 35, 15, 35));
        setContentPane(content);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        JButton button4 = new JButton("Aufträge erstellen");
        button4.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                auftrag();
            }
        });
        JPanel drum = new JPanel(new FlowLayout(FlowLayout.CENTER));
        drum.add(button4);
        getContentPane().add(drum);
        JButton button5 = new JButton("Aufträge abarbeiten");
        button5.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                abarbeitung();
            }
        });
        drum = new JPanel(new FlowLayout(FlowLayout.CENTER));
	    drum.add(button5);
        getContentPane().add(drum);
        JButton button2 = new JButton("Predigten bearbeiten");
        button2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                predigt();
            }
        });
        drum = new JPanel(new FlowLayout(FlowLayout.CENTER));
        drum.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
        drum.add(button2);
        getContentPane().add(drum);
        JButton button3 = new JButton("Sammlungen bearbeiten");
        button3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                sammlung();
            }
        });
        drum = new JPanel(new FlowLayout(FlowLayout.CENTER));
        drum.add(button3);
        getContentPane().add(drum);
        JButton button1 = new JButton("Abos bearbeiten");
        button1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                abo();
            }
        });
        drum = new JPanel(new FlowLayout(FlowLayout.CENTER));
        drum.add(button1);
        getContentPane().add(drum);
        JButton button6 = new JButton("BEENDEN");
        button6.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                exit();
            }
        });
        drum = new JPanel(new FlowLayout(FlowLayout.CENTER));
        drum.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
        drum.add(button6);
        getContentPane().add(drum);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                exit();
            }
        });
        setLocation(50, 50);
        setSize(400, 400);
        pack();
        show();
    }
    
    public void setMaximize(boolean what) {
        maximize = what;
    }
    
    public boolean getMaximize() {
        return maximize;
    }
    
    public void auftrag() {
        // starte Maske zum Erstellen und Bearbeiten von Aufträgen
        new AuftragGUI(getMaximize());
    }
    
    public void abarbeitung() {
        // starte Maske zum Abarbeiten von Aufträgen
        new AbarbeitungGUI(getMaximize());
    }
    
    public void predigt() {
        // starte Predigten-Maske
        new PredigtGUI(getMaximize());
    }
    
    public void sammlung() {
        // starte Sammlungen-Maske
        new SammlungGUI(getMaximize());
    }
    
    public void abo() {
        // starte Abo-Maske
        new AboGUI(getMaximize());
    }
    
    public void exit() {
        // beendet das Programm
        try {
            DB.getInstance().close();
        } catch(NullPointerException npe) {
            // tja...
        }
        System.exit(0);
    }
    
    
}
