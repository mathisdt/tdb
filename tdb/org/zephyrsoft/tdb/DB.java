package org.zephyrsoft.tdb;

import java.sql.*;
import java.util.*;

import javax.swing.*;

/**
 * Singleton-Design-Pattern. Diese Klasse
 * verwaltet die Datenbank-Verbindung.
 * 
 * @author Mathis Dirksen-Thedens
 *
 */
public class DB {
    private static final DB _instance = new DB();
    private ResourceBundle RESOURCE_BUNDLE = null;
    private Connection conn = null;
    
    public ResultSet getItem(String was) {
        if (was.substring(0, 1).equalsIgnoreCase("p")) {
            return select("select datum,klasse,sprecher,name,thema,bemerkung,id from predigt where id=" + was.substring(1));
        } else {
            return select("select datum,klasse,sprecher,name,thema,bemerkung,id from sammlung where id=" + was.substring(1));
        }
    }
    public String getItemShort(String was) {
        try {
            if (was.substring(0, 1).equalsIgnoreCase("p")) {
                ResultSet rs = select("select datum,klasse,sprecher,name,thema,bemerkung,id from predigt where id=" + was.substring(1));
                rs.beforeFirst();
                rs.next();
                return rs.getString(2) + " / " + rs.getString(1);
            } else {
                ResultSet rs = select("select datum,klasse,sprecher,name,thema,bemerkung,id from sammlung where id=" + was.substring(1));
                rs.beforeFirst();
                rs.next();
                return "Sammlung " + rs.getString(2) + " / " + rs.getString(1);
            }
        } catch(SQLException e) {
            return "";
        }
    }
    
    public ResultSet getAbos() {
        return select("select erstellung,besteller,klasse,ist_aktiv,bemerkung,id from abo order by besteller,klasse DESC");
    }
    public ResultSet getAktiveAbos() {
        return select("select erstellung,besteller,klasse,ist_aktiv,bemerkung,id from abo where ist_aktiv=1 order by besteller,klasse");
    }
    public int insertAbo(String besteller, String klasse, String bemerkung) {
        return insert_update_delete("insert into abo (erstellung, besteller, klasse, ist_aktiv, bemerkung) values (now(), '" + besteller + "', '" + klasse + "', 1, '" + bemerkung + "')");
    }
    public int updateAbo(int id, String besteller, String klasse, boolean ist_aktiv, String bemerkung) {
        return insert_update_delete("update abo set besteller='" + besteller + "', klasse='" + klasse + "', ist_aktiv=" + (ist_aktiv ? "1" : "0") + ", bemerkung='" + bemerkung + "' where id=" + id);
    }
    public int deleteAbo(int id) {
        return insert_update_delete("delete from abo where id=" + id);
    }
    
    public ResultSet getPredigten() {
        return select("select datum,klasse,sprecher,name,thema,bemerkung,id from predigt order by datum,klasse DESC,name,sprecher");
    }
    public int insertPredigt(String datum, String klasse, String sprecher, String name, String thema, String bemerkung) {
        if (datum==null || datum.trim().equalsIgnoreCase("heute") || datum.trim().equalsIgnoreCase("today") || datum.trim().equalsIgnoreCase("now") || datum.trim().equalsIgnoreCase("now()") || datum.trim().equalsIgnoreCase("")) {
            return insert_update_delete("insert into predigt (datum, klasse, sprecher, name, thema, bemerkung) values (now(), '" + klasse + "', '" + sprecher + "', '" + name + "', '" + thema + "', '" + bemerkung + "')");
        } else {
            return insert_update_delete("insert into predigt (datum, klasse, sprecher, name, thema, bemerkung) values ('" + datum + "', '" + klasse + "', '" + sprecher + "', '" + name + "', '" + thema + "', '" + bemerkung + "')");
        }
    }
    public int updatePredigt(int id, String datum, String klasse, String sprecher, String name, String thema, String bemerkung) {
        if (datum.trim().equalsIgnoreCase("heute") || datum.trim().equalsIgnoreCase("today") || datum.trim().equalsIgnoreCase("now") || datum.trim().equalsIgnoreCase("now()")) {
            return insert_update_delete("update predigt set datum=now(), klasse='" + klasse + "', sprecher='" + sprecher + "', name='" + name + "', thema='" + thema + "', bemerkung='" + bemerkung + "' where id=" + id);
        } else {
            return insert_update_delete("update predigt set datum='" + datum + "', klasse='" + klasse + "', sprecher='" + sprecher + "', name='" + name + "', thema='" + thema + "', bemerkung='" + bemerkung + "' where id=" + id);
        }
    }
    public int deletePredigt(int id) {
        return insert_update_delete("delete from predigt where id=" + id);
    }
    
    public ResultSet getSammlungen() {
        return select("select datum,klasse,sprecher,name,thema,bemerkung,id from sammlung order by datum,klasse DESC,sprecher,name");
    }
    public int insertSammlung(String datum, String klasse, String sprecher, String name, String thema, String bemerkung) {
        if (datum.trim().equalsIgnoreCase("heute") || datum.trim().equalsIgnoreCase("today") || datum.trim().equalsIgnoreCase("now") || datum.trim().equalsIgnoreCase("now()")) {
            return insert_update_delete("insert into sammlung (datum, klasse, sprecher, name, thema, bemerkung) values (now(), '" + klasse + "', '" + sprecher + "', '" + name + "', '" + thema + "', '" + bemerkung + "')");
        } else {
            return insert_update_delete("insert into sammlung (datum, klasse, sprecher, name, thema, bemerkung) values ('" + datum + "', '" + klasse + "', '" + sprecher + "', '" + name + "', '" + thema + "', '" + bemerkung + "')");
        }
    }
    public int updateSammlung(int id, String datum, String klasse, String sprecher, String name, String thema, String bemerkung) {
        return insert_update_delete("update sammlung set datum='" + datum + "', klasse='" + klasse + "', sprecher='" + sprecher + "', name='" + name + "', thema='" + thema + "', bemerkung='" + bemerkung + "' where id=" + id);
    }
    public int deleteSammlung(int id) {
        return insert_update_delete("delete from sammlung where id=" + id);
    }
    
    public ResultSet getAuftraege() {
        return select("(select erstellung,besteller,medium,was,fertigstellung,bemerkung,id from auftrag) " +
                "UNION " +
                "(select NOW(),abo.besteller,'Kassette',CONCAT('P',predigt.id),null,'ABO-AUFTRAG',-3 " +
                    "from abo,predigt where " +
                        "abo.klasse=predigt.klasse AND " +
                        "abo.ist_aktiv=true AND " +
                        "DATEDIFF(abo.erstellung,predigt.datum)<=0 AND " +
                        "NOT EXISTS " +
                            "(select * from auftrag " +
                            "where auftrag.fertigstellung IS NOT null AND auftrag.fertigstellung<>'' " +
                            "AND auftrag.was=CONCAT('P',predigt.id)" +
                            "AND abo.besteller=auftrag.besteller)) " +
                "order by isnull(fertigstellung), fertigstellung, id");
    }
    public int insertAuftrag(String besteller, String bemerkung, String medium, String was) {
        return insert_update_delete("insert into auftrag (erstellung, besteller, bemerkung, medium, was) values (now(), '" + besteller + "', '" + bemerkung + "', '" + medium + "', '" + was + "')");
    }
    public int updateAuftrag(int id, String besteller, String bemerkung, String medium, String was) {
        return insert_update_delete("update auftrag set besteller='" + besteller + "', bemerkung='" + bemerkung + "', medium='" + medium + "', was='" + was + "' where id=" + id);
    }
    public int setAuftragErledigt(int id) {
        return insert_update_delete("update auftrag set fertigstellung=now() where id=" + id);
    }
    public int setAboAuftragErledigt(String besteller, String bemerkung, String medium, String was) {
        return insert_update_delete("insert into auftrag (erstellung, besteller, bemerkung, medium, was, fertigstellung) values (now(), '" + besteller + "', '" + bemerkung + "', '" + medium + "', '" + was + "', now())");
    }
    public int setAuftragUnerledigt(int id) {
        return insert_update_delete("update auftrag set fertigstellung=null where id=" + id);
    }
    public int deleteAuftrag(int id) {
        return insert_update_delete("delete from auftrag where id=" + id);
    }
    
    private ResultSet select(String string) {
        try {
            Statement st = conn.createStatement();
            return st.executeQuery(string);
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Die folgende Anfrage konnte nicht ausgeführt werden:\n" + string,"Fehler", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
    
    private int insert_update_delete(String string) {
        try {
            Statement st = conn.createStatement();
            return st.executeUpdate(string);
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Die folgende Anfrage konnte nicht ausgeführt werden:\n" + string,"Fehler", JOptionPane.ERROR_MESSAGE);
            return 0;
        }
    }
    
    public DB() {
        // Daten holen
        try {
            RESOURCE_BUNDLE = ResourceBundle.getBundle("db");
        } catch(MissingResourceException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Die Zugangsdaten für die Datenbank können nicht gefunden werden!\nBitte stellen Sie sicher, dass sich die Datei db.properties lesbar ist.","Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Treiber laden
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch(ClassNotFoundException cnfex) {
            cnfex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Der Datenbanktreiber wurde nicht gefunden.","Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Daten zur Verfügung stellen
        String server = RESOURCE_BUNDLE.getString("server");
        int port = 3306;
        String db = RESOURCE_BUNDLE.getString("db");
        String user = RESOURCE_BUNDLE.getString("user");
        String password = RESOURCE_BUNDLE.getString("password");
        // Verbindung herstellen
        try {
            conn = DriverManager.getConnection("jdbc:mysql://" + server + "/" + db, user, password);
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Die Verbindung zur Datenbank auf " + RESOURCE_BUNDLE.getString("server") + " konnte nicht\nhergestellt werden. Bitte überprüfen Sie die Zugangsdaten!","Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static DB getInstance() {
        return _instance;
    }

    public void close() {
        try {
            conn.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Die Verbindung zur Datenbank auf " + RESOURCE_BUNDLE.getString("server") + " konnte nicht geschlossen werden.","Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }
}
