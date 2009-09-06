package org.zephyrsoft.tdb;

import javax.swing.*;

import org.zephyrsoft.util.*;

public class Start {

    public static void main(String[] args) {
        SelectGUI selgui = new SelectGUI();
        for (int a = 0; a < args.length; a++) {
            String arg = args[a];
            
            if (arg.equalsIgnoreCase("--abo")) {
                // Abos bearbeiten
                selgui.abo();
            } else if (arg.equalsIgnoreCase("--predigt")) {
                // Predigten bearbeiten
                selgui.predigt();
            } else if (arg.equalsIgnoreCase("--sammlung")) {
                // Sammlungen bearbeiten
                selgui.sammlung();
            } else if (arg.equalsIgnoreCase("--auftrag")) {
                // Aufträge hinzufügen/bearbeiten
                selgui.auftrag();
            } else if (arg.equalsIgnoreCase("--abarbeitung")) {
                // Aufträge abarbeiten
                selgui.abarbeitung();
            } else if (arg.equalsIgnoreCase("--maximize")) {
                // alle Tabellenfenster sollen maximiert sein
                selgui.setMaximize(true);
            }
        }
    }
    
}
