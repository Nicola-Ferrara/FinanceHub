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
    
    public Connection getConnection() throws SQLException, IOException, NullPointerException {
    try {
        // ✅ TESTA LA CONNESSIONE PRIMA DI USARLA
        if (conn != null && !conn.isClosed() && conn.isValid(2)) {
            return conn;
        }
    } catch (SQLException e) {
        // Connessione non valida, ricrea
    }
    
    // ✅ RICREA LA CONNESSIONE
    return createNewConnection();
}

private Connection createNewConnection() throws SQLException, IOException {
    String pwd = System.getenv("SUPABASE_PASSWORD");
    
    if (pwd == null || pwd.isEmpty()) {
        try (InputStream is = getClass().getResourceAsStream("/pwdfile"); 
             BufferedReader b = new BufferedReader(new InputStreamReader(is))) {
            pwd = b.readLine();
        }
    }
    
    String s_url = "jdbc:postgresql://aws-0-eu-central-2.pooler.supabase.com:5432/postgres?sslmode=require&tcpKeepAlive=true";
    String username = "postgres.essksmuwvtgqubgzokal";
    
    conn = DriverManager.getConnection(s_url, username, pwd);
    System.out.println("🔄 Nuova connessione database creata");
    return conn;
}
}