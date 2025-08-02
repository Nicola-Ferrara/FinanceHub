package dao;

import db_connection.DBConnection;
import exception.EccezioniDatabase;
import java.sql.*;
import java.io.IOException;

public class DAOFactory {
    
    private static volatile DAOFactory instance = null;
    private static final Object lock = new Object();
    private DBConnection dbConnection;
    
    // DAO instances (lazy initialization)
    private UtenteDAO utenteDAO;
    private CategoriaDAO categoriaDAO;
    private ContoDAO contoDAO;
    private LogOperazioniDAO logOperazioniDAO;
    private TransazioneDAO transazioneDAO;
    private TrasferimentoDAO trasferimentoDAO;
    
    private DAOFactory() {
        this.dbConnection = DBConnection.getDBConnection();
    }
    
    public static DAOFactory getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DAOFactory();
                }
            }
        }
        return instance;
    }
    
    // ✅ METODO GENERICO CON RETRY AUTOMATICO
    public <T> T executeWithRetry(DatabaseOperation<T> operation, String errorMessage) throws EccezioniDatabase {
        int maxRetries = 3;
        int currentTry = 0;
        
        while (currentTry < maxRetries) {
            try {
                Connection conn = dbConnection.getConnection();
                return operation.execute(conn);
                
            } catch (SQLException | IOException e) {
                currentTry++;
                System.err.println("Errore database (tentativo " + currentTry + "): " + e.getMessage());
                
                if (isConnectionError(e) && currentTry < maxRetries) {
                    System.out.println("Errore di connessione. Forzo nuova connessione...");
                    try {
                        // Forza chiusura e riconnessione
                        dbConnection.forceReconnect();
                        Thread.sleep(1000);
                    } catch (Exception retryError) {
                        System.err.println("Errore nel retry: " + retryError.getMessage());
                    }
                } else {
                    throw new EccezioniDatabase(errorMessage + " (dopo " + currentTry + " tentativi)");
                }
            }
        }
        
        throw new EccezioniDatabase(errorMessage + " (esauriti tutti i " + maxRetries + " tentativi)");
    }
    
    private boolean isConnectionError(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;
        
        return message.contains("Connection reset") ||
               message.contains("closed") ||
               message.contains("timeout") ||
               message.contains("scaduto") ||
               message.contains("I/O") ||
               e instanceof java.net.SocketException;
    }
    
    // ✅ GETTER PER I DAO (lazy initialization)
    public UtenteDAO getUtenteDAO() {
        if (utenteDAO == null) {
            utenteDAO = new UtenteDAO();
        }
        return utenteDAO;
    }
    
    public CategoriaDAO getCategoriaDAO() {
        if (categoriaDAO == null) {
            categoriaDAO = new CategoriaDAO();
        }
        return categoriaDAO;
    }
    
    public ContoDAO getContoDAO() {
        if (contoDAO == null) {
            contoDAO = new ContoDAO();
        }
        return contoDAO;
    }
    
    public LogOperazioniDAO getLogOperazioniDAO() {
        if (logOperazioniDAO == null) {
            logOperazioniDAO = new LogOperazioniDAO();
        }
        return logOperazioniDAO;
    }
    
    public TransazioneDAO getTransazioneDAO() {
        if (transazioneDAO == null) {
            transazioneDAO = new TransazioneDAO();
        }
        return transazioneDAO;
    }
    
    public TrasferimentoDAO getTrasferimentoDAO() {
        if (trasferimentoDAO == null) {
            trasferimentoDAO = new TrasferimentoDAO();
        }
        return trasferimentoDAO;
    }
    
    // ✅ INTERFACCIA FUNZIONALE
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute(Connection conn) throws SQLException, IOException;
    }
}