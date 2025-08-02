package dao;

import java.sql.*;
import java.util.LinkedList;

import dto.LogOperazione;
import exception.EccezioniDatabase;

public class LogOperazioniDAO {
    
    public LogOperazioniDAO() {}

    public LinkedList<LogOperazione> getLogOperazioni(String email_utente) throws EccezioniDatabase {
        return DAOFactory.getInstance().executeWithRetry((conn) -> {
            LinkedList<LogOperazione> logOperazioni = new LinkedList<>();
            String sql = "SELECT * FROM LogOperazione WHERE email_utente = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email_utente);
                ResultSet resultSet = ps.executeQuery();
                while(resultSet.next()) {
                    logOperazioni.add(new LogOperazione(resultSet.getInt("id"), LogOperazione.TipoLog.valueOf(resultSet.getString("tipo")), resultSet.getString("operazione"), resultSet.getTimestamp("data")));
                }
            }
            return logOperazioni;
        }, "ERRORE DURANTE IL RECUPERO DEL LOG OPERAZIONI");
    }

    public void saveLogOperazione(LogOperazione log, String email_utente) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "INSERT INTO LogOperazioni (id, tipo, operazione, data, email_utente) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, log.getID());
                ps.setString(2, log.getTipo().name());
                ps.setString(3, log.getDescrizione());
                ps.setTimestamp(4, log.getData());
                ps.setString(5, email_utente);
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE IL SALVATAGGIO DEL LOG OPERAZIONE");
    }

    public int newID() throws EccezioniDatabase {
        return DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "SELECT MAX(id) AS max_id FROM LogOperazioni";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_id") + 1;
                }
                return 1;
            }
        }, "ERRORE DURANTE IL RECUPERO DEL NUOVO ID LOG OPERAZIONE");
    }
}