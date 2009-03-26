package org.zephyrsoft.tdb;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;

import javax.swing.*;

import org.zephyrsoft.util.*;

import sun.reflect.ReflectionFactory.*;

/**
 * Singleton-Design-Pattern. Diese Klasse
 * verwaltet die Datenbank-Verbindung.
 * 
 * @author Mathis Dirksen-Thedens
 *
 */
public class DB {

	// falls nötig, die Datenbank um hinzugekommene Spalten erweitern:
    // - "ALTER TABLE `abo` ADD `medium` VARCHAR( 25 ) NOT NULL DEFAULT 'Kassette';"
	// falls nötig, DB-Spalten ändern:
	// - ALTER TABLE `predigt` CHANGE `sprecher` `sprecher` TEXT CHARACTER SET latin1 COLLATE latin1_german2_ci NOT NULL, CHANGE `name` `name` TEXT CHARACTER SET latin1 COLLATE latin1_german2_ci NULL DEFAULT NULL 
    
	private static final DB _instance = new DB();
    private static ResourceBundle _resource_bundle;
    
    private Connection conn = null;
    
    public static ResourceBundle getProperties() {
    	if (_resource_bundle==null) {
    		try {
                _resource_bundle = ResourceBundle.getBundle("db");
            } catch(MissingResourceException ex) {
            	_resource_bundle = null;
            }
    	}
		return _resource_bundle;
	}
    
    public static String getProperty(String name) {
    	if (getProperties()==null) {
    		return null;
    	} else {
    		return getProperties().getString(name);
    	}
    }

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
        return select("select erstellung,besteller,klasse,ist_aktiv,bemerkung,id,medium from abo order by besteller,klasse DESC");
    }
    public ResultSet getAktiveAbos() {
        return select("select erstellung,besteller,klasse,ist_aktiv,bemerkung,id,medium from abo where ist_aktiv=1 order by besteller,klasse");
    }
    public int insertAbo(String besteller, String klasse, String bemerkung, String medium) {
        return insert_update_delete("insert into abo (erstellung, besteller, klasse, ist_aktiv, bemerkung, medium) values (now(), '" + besteller + "', '" + klasse + "', 1, '" + bemerkung + "', '" + medium + "')");
    }
    public int updateAbo(int id, String besteller, String klasse, boolean ist_aktiv, String bemerkung, String medium) {
        return insert_update_delete("update abo set besteller='" + besteller + "', klasse='" + klasse + "', ist_aktiv=" + (ist_aktiv ? "1" : "0") + ", bemerkung='" + bemerkung + "', medium='" + medium + "' where id=" + id);
    }
    public int deleteAbo(int id) {
        return insert_update_delete("delete from abo where id=" + id);
    }
    
    public void updatePredigtenVonDateien() {
    	if (NullSafeUtils.safeEquals(DB.getProperty(DB.PROPERTY_NAMES.ITEM_SOURCE), "files")) {
    		// Predigten werden von existierenden Dateien abgeleitet
    		ResultSet rs = null;
    		File dir = new File(DB.getProperty(DB.PROPERTY_NAMES.ITEM_DIRECTORY));
    		String[] children = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return (name!=null && name.toLowerCase().endsWith("kbps.mp3"));
				}
    		});
    	    if (children != null) {
    	    	Arrays.sort(children);
    	        for (int i=0; i<children.length; i++) {
    	            String filename = children[i];
    	            filename = filename.replaceAll("-..kbps\\.mp3$", "");
    	            StringTokenizer tok = new StringTokenizer(filename, "-");
    	            String date = tok.nextToken() + "-" + tok.nextToken() + "-" + tok.nextToken();
    	            String type = tok.nextToken();
    	            // SEM (Seminar) wenn nichts anderes passt
    	            String shortType = "SEM";
    	            if (type.equals("Morgengottesdienst")) {
    	            	shortType = "GD-M";
    	            } else if (type.equals("Abendgottesdienst")) {
    	            	shortType = "GD-A";
    	            } else if (type.equals("Bibelstunde")) {
    	            	shortType = "BS";
    	            } else if (type.equals("Maennerfruehstueck")) {
    	            	shortType = "FR-M";
    	            } else if (type.equals("Frauenfruehstueck")) {
    	            	shortType = "FR-R";
    	            }
    	            String speaker = "";
    	            if (tok.hasMoreTokens()) {
    	            	speaker = tok.nextToken();
    	            }
    	            while (tok.hasMoreTokens()) {
    	            	speaker += "-" + tok.nextToken();
    	            }
    	            // wenn noch kein passender DB-Eintrag vorhanden ist, muss er erzeugt werden
    	            try {
    	            	rs = select("select id from predigt where datum='" + date + "' and klasse='" + shortType + "' and sprecher='" + speaker + "' and bemerkung='" + type + "'");;
    	            	if (!rs.next()) {
    	            		insert_update_delete("insert into predigt (datum, klasse, sprecher, name, thema, bemerkung) values ('" + date + "', '" + shortType + "', '" + speaker + "', '', '', '" + type + "')");
						}
	    	        } catch(SQLException sqlex) {
    		            sqlex.printStackTrace();
    		            JOptionPane.showMessageDialog(null, "Ein Fehler trat beim Einlesen der Predigten auf.", "Fehler", JOptionPane.ERROR_MESSAGE);
    		        } finally {
    		        	DB.closeResultSet(rs);
    		        }
    	        }
    	    }
    	}
    }
    
    public Vector getPredigten() {
    	if (NullSafeUtils.safeEquals(DB.getProperty(DB.PROPERTY_NAMES.ITEM_SOURCE), "files")) {
    		// Predigten werden von existierenden Dateien abgeleitet
    		Vector data = new Vector();
    		ResultSet rs = null;
    		File dir = new File(DB.getProperty(DB.PROPERTY_NAMES.ITEM_DIRECTORY));
    		String[] children = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return (name!=null && name.toLowerCase().endsWith("kbps.mp3"));
				}
    		});
    	    if (children != null) {
    	    	Arrays.sort(children);
    	        for (int i=0; i<children.length; i++) {
    	            String filename = children[i];
    	            filename = filename.replaceAll("-..kbps\\.mp3$", "");
    	            StringTokenizer tok = new StringTokenizer(filename, "-");
    	            String date = tok.nextToken() + "-" + tok.nextToken() + "-" + tok.nextToken();
    	            String type = tok.nextToken();
    	            // SEM (Seminar) wenn nichts anderes passt
    	            String shortType = "SEM";
    	            if (type.equals("Morgengottesdienst")) {
    	            	shortType = "GD-M";
    	            } else if (type.equals("Abendgottesdienst")) {
    	            	shortType = "GD-A";
    	            } else if (type.equals("Bibelstunde")) {
    	            	shortType = "BS";
    	            } else if (type.equals("Maennerfruehstueck")) {
    	            	shortType = "FR-M";
    	            } else if (type.equals("Frauenfruehstueck")) {
    	            	shortType = "FR-R";
    	            }
    	            String speaker = "";
    	            if (tok.hasMoreTokens()) {
    	            	speaker = tok.nextToken();
    	            }
    	            while (tok.hasMoreTokens()) {
    	            	speaker += "-" + tok.nextToken();
    	            }
    	            int id = -1;
    	            // wenn noch kein passender DB-Eintrag vorhanden ist, muss er erzeugt werden
    	            try {
    	            	rs = select("select id,name,thema from predigt where datum='" + date + "' and klasse='" + shortType + "' and sprecher='" + speaker + "' and bemerkung='" + type + "'");;
    	            	if (rs.next()) {
    	            		id = rs.getInt(1);
    	            	} else {
    	            		insert_update_delete("insert into predigt (datum, klasse, sprecher, name, thema, bemerkung) values ('" + date + "', '" + shortType + "', '" + speaker + "', '', '', '" + type + "')");
    	            		DB.closeResultSet(rs);
    	            		rs = select("select id,name,thema from predigt where datum='" + date + "' and klasse='" + shortType + "' and sprecher='" + speaker + "' and bemerkung='" + type + "'");;
    	            		rs.next();
        	            	id = rs.getInt(1);
						}
	    	            Vector objects = new Vector();
	    	            // datum
		                objects.addElement(date);
						// klasse
						objects.addElement(shortType);
						// sprecher
						objects.addElement(speaker.replaceAll("_", " "));
						// name
						objects.addElement(rs.getString(2));
						// thema
						objects.addElement(rs.getString(3));
						// bemerkung
						objects.addElement(type);
						// id
						objects.addElement(Integer.valueOf(id));
		                data.addElement(objects);
	    	        } catch(SQLException sqlex) {
    		            sqlex.printStackTrace();
    		            JOptionPane.showMessageDialog(null, "Ein Fehler trat beim Einlesen der Predigten auf.", "Fehler", JOptionPane.ERROR_MESSAGE);
    		            data = null;
    		        } finally {
    		        	DB.closeResultSet(rs);
    		        }
    	        }
    	    }
    		return data;
    	} else {
    		ResultSet rs = null;
    		Vector data = new Vector();
    		try {
	    		rs = select("select datum,klasse,sprecher,name,thema,bemerkung,id from predigt order by datum,klasse DESC,name,sprecher");;
	    		ResultSetMetaData rsmd;
				rsmd = rs.getMetaData();
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
	            data = null;
	        } finally {
	        	DB.closeResultSet(rs);
	        }
	        return data;
    	}
    }
    public int getPredigtTableColumnCount() {
    	return 7;
    }
    public int insertPredigt(String datum, String klasse, String sprecher, String name, String thema, String bemerkung) {
    	if (NullSafeUtils.safeEquals(DB.getProperty(DB.PROPERTY_NAMES.ITEM_SOURCE), "files")) {
    		throw new IllegalStateException("Predigten können nicht neu eingefügt werden, weil sie von existierenden Dateien abgeleitet werden!");
    	}
        if (datum==null || datum.trim().equalsIgnoreCase("heute") || datum.trim().equalsIgnoreCase("today") || datum.trim().equalsIgnoreCase("now") || datum.trim().equalsIgnoreCase("now()") || datum.trim().equalsIgnoreCase("")) {
            return insert_update_delete("insert into predigt (datum, klasse, sprecher, name, thema, bemerkung) values (now(), '" + klasse + "', '" + sprecher + "', '" + name + "', '" + thema + "', '" + bemerkung + "')");
        } else {
            return insert_update_delete("insert into predigt (datum, klasse, sprecher, name, thema, bemerkung) values ('" + datum + "', '" + klasse + "', '" + sprecher + "', '" + name + "', '" + thema + "', '" + bemerkung + "')");
        }
    }
    public int updatePredigt(int id, String datum, String klasse, String sprecher, String name, String thema, String bemerkung) {
    	if (NullSafeUtils.safeEquals(DB.getProperty(DB.PROPERTY_NAMES.ITEM_SOURCE), "files")) {
    		return insert_update_delete("update predigt set thema='" + thema + "' where id=" + id);
    	} else if (datum.trim().equalsIgnoreCase("heute") || datum.trim().equalsIgnoreCase("today") || datum.trim().equalsIgnoreCase("now") || datum.trim().equalsIgnoreCase("now()")) {
            return insert_update_delete("update predigt set datum=now(), klasse='" + klasse + "', sprecher='" + sprecher + "', name='" + name + "', thema='" + thema + "', bemerkung='" + bemerkung + "' where id=" + id);
        } else {
            return insert_update_delete("update predigt set datum='" + datum + "', klasse='" + klasse + "', sprecher='" + sprecher + "', name='" + name + "', thema='" + thema + "', bemerkung='" + bemerkung + "' where id=" + id);
        }
    }
    public int deletePredigt(int id) {
    	if (NullSafeUtils.safeEquals(DB.getProperty(DB.PROPERTY_NAMES.ITEM_SOURCE), "files")) {
    		throw new IllegalStateException("Predigten können nicht gelöscht werden, weil sie von existierenden Dateien abgeleitet werden!");
    	}
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
    	DB.getInstance().updatePredigtenVonDateien();
        return select("(select erstellung,besteller,medium,was,fertigstellung,bemerkung,id from auftrag) " +
                "UNION " +
                "(select NOW(),abo.besteller,medium,CONCAT('P',predigt.id),null,'ABO-AUFTRAG',-3 " +
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
        if (getProperties()==null) {
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
        String server = getProperties().getString("server");
        int port = 3306;
        String db = getProperties().getString("db");
        String user = getProperties().getString("user");
        String password = getProperties().getString("password");
        // Verbindung herstellen
        try {
            conn = DriverManager.getConnection("jdbc:mysql://" + server + "/" + db, user, password);
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Die Verbindung zur Datenbank auf " + getProperties().getString("server") + " konnte nicht\nhergestellt werden. Bitte überprüfen Sie die Zugangsdaten!","Fehler", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(null, "Die Verbindung zur Datenbank auf " + getProperties().getString("server") + " konnte nicht geschlossen werden.","Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void closeResultSet(ResultSet rs) {
    	try {
    		rs.getStatement().close();
    		rs.close();
		} catch (SQLException e) {
			System.err.println(SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM).format(new Date()) + ": Could not close result set properly!");
		}
    }
    
    public class PROPERTY_NAMES {
    	public static final String DATABASE_SERVER = "server";
    	public static final String DATABASE_NAME = "db";
    	public static final String DATABASE_USER = "user";
    	public static final String DATABASE_PASSWORD = "password";
    	public static final String ITEM_SOURCE = "source";
    	public static final String ITEM_DIRECTORY = "directory";
    }
}
