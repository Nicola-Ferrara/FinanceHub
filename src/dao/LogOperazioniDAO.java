package dao;

import java.sql.*;
import java.util.LinkedList;

import dto.LogOperazione;

public class LogOperazioniDAO {
    
    // Attributi
    private Connection connection;

    // Costruttore
    public LogOperazioniDAO(Connection connection) {
        this.connection = connection;
    }

    public LinkedList<LogOperazione> getLogOperazioni(String email_utente) throws SQLException {
        LinkedList<LogOperazione> logOperazioni = new LinkedList<>();
        String sql = "SELECT * FROM LogOperazione WHERE email_utente = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email_utente);
            ResultSet resultSet = ps.executeQuery();
            while(resultSet.next()) {
                logOperazioni.add(new LogOperazione(resultSet.getInt("id"), LogOperazione.TipoLog.valueOf(resultSet.getString("tipo")), resultSet.getString("operazione"), resultSet.getTimestamp("data")));
            }
        }
        return logOperazioni;
    }

    public void saveLogOperazione(LogOperazione log, String email_utente) throws SQLException {
        String sql = "INSERT INTO LogOperazioni (id, tipo, operazione, data, email_utente) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, log.getID());
            ps.setString(2, log.getTipo().name());
            ps.setString(3, log.getDescrizione());
            ps.setTimestamp(4, log.getData());
            ps.setString(5, email_utente);
            ps.executeUpdate();
        }
    }

    public int newID() throws SQLException {
        String sql = "SELECT MAX(id) AS max_id FROM LogOperazioni";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("max_id") + 1;
            }
            return 1;
        }
    }
}
