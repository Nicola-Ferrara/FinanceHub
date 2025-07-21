package db_connection;

import java.io.*;
import java.sql.*;
import java.util.Properties;

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
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {}
            conn = null;
        }
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
        String s_url = "jdbc:postgresql://aws-0-eu-central-2.pooler.supabase.com:5432/postgres?sslmode=require&tcpKeepAlive=true&socketTimeout=30000";
        String username = "postgres.essksmuwvtgqubgzokal";
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", pwd);
        props.setProperty("ssl", "true");
        props.setProperty("sslmode", "require");
        props.setProperty("tcpKeepAlive", "true");
        props.setProperty("loginTimeout", "10");
        props.setProperty("socketTimeout", "30"); 
        conn = DriverManager.getConnection(s_url, props);
        System.out.println("🔄 Nuova connessione database creata");
        return conn;
    }
}