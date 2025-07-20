package db_connection;

import java.io.*;
import java.sql.*;

public class DBConnection {
    
	// Attributi
	private static DBConnection dbcon = null;
    private Connection conn = null;
    
    // Costruttore
    private DBConnection(){}

    // Metodo per avere una sola connessione al database
    public static DBConnection getDBConnection() {
        if (dbcon == null) {
            dbcon = new DBConnection();
        }
        return dbcon;
    }
    
    // Metodo per ottenere la connessione al database
    public Connection getConnection() throws SQLException, IOException, NullPointerException {
        if (conn == null || conn.isClosed()) {
        	try (InputStream is = getClass().getResourceAsStream("/pwdfile"); BufferedReader b = new BufferedReader(new InputStreamReader(is))) {
                String pwd = b.readLine();
                String s_url = "jdbc:postgresql://localhost:5432/FinanceHub?currentSchema=View_FinanceHub";
                conn = DriverManager.getConnection(s_url, "postgres", pwd);
            }
        }
        return conn;
    }
}