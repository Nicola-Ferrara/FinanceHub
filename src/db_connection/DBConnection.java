package db_connection;

import java.io.*;
import java.sql.*;
import java.util.Properties;

public class DBConnection {
    
    private static volatile DBConnection dbcon = null;
    private static final Object lock = new Object();
    private Connection conn = null;
    private long lastUsed = 0;
    private static final long CONNECTION_TIMEOUT = 300000; // 5 minuti
    
    private DBConnection(){}

    public static DBConnection getDBConnection() {
        if (dbcon == null) {
            synchronized (lock) {
                if (dbcon == null) {
                    dbcon = new DBConnection();
                }
            }
        }
        return dbcon;
    }
    
    public synchronized Connection getConnection() throws SQLException, IOException, NullPointerException {
        long currentTime = System.currentTimeMillis();
        
        // ✅ RIUSA LA CONNESSIONE SE È VALIDA E RECENTE
        if (conn != null && isConnectionValid(conn) && 
            (currentTime - lastUsed) < CONNECTION_TIMEOUT) {
            
            lastUsed = currentTime;
            return conn;
        }
        
        // ✅ CHIUDI E RICREA SOLO SE NECESSARIO
        closeConnection();
        conn = createNewConnection();
        lastUsed = currentTime;
        return conn;
    }
    
    private boolean isConnectionValid(Connection connection) {
        try {
            return connection != null && 
                   !connection.isClosed() && 
                   connection.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }
    
    public synchronized void forceReconnect() throws SQLException, IOException {
        closeConnection();
        // ✅ NON RICREARE SUBITO - LASCIA CHE getConnection() LO FACCIA
    }
    
    private synchronized void closeConnection() {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Errore nella chiusura connessione: " + e.getMessage());
            } finally {
                conn = null;
            }
        }
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
        
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", pwd);
        props.setProperty("ssl", "true");
        props.setProperty("sslmode", "require");
        props.setProperty("tcpKeepAlive", "true");
        props.setProperty("loginTimeout", "30");
        props.setProperty("socketTimeout", "60");
        
        Connection newConnection = DriverManager.getConnection(s_url, props);
        return newConnection;
    }
}