package dao;

import java.sql.*;
import java.util.LinkedList;
import dto.Trasferimento;
import exception.EccezioniDatabase;

public class TrasferimentoDAO {
    
    public TrasferimentoDAO() {}

    public LinkedList<Trasferimento> getTrasferimenti(String email_utente) throws EccezioniDatabase {
        return DAOFactory.getInstance().executeWithRetry((conn) -> {
            LinkedList<Trasferimento> trasferimenti = new LinkedList<>();
            String sql = "SELECT * FROM Trasferimento WHERE email_utente = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email_utente);
                ResultSet resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    trasferimenti.add(new Trasferimento(resultSet.getInt("id"), resultSet.getDouble("importo"), resultSet.getTimestamp("data"), resultSet.getString("descrizione"), resultSet.getInt("id_conto_mittente"), resultSet.getInt("id_conto_destinatario")));
                }
            }
            return trasferimenti;
        }, "ERRORE DURANTE IL RECUPERO DEI TRASFERIMENTI");
    }

    public void saveTrasferimento(Trasferimento trasferimento, String email_utente) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "INSERT INTO Trasferimento (id, importo, data, descrizione, email_utente, id_conto_mittente, id_conto_destinatario) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, trasferimento.getID());
                ps.setDouble(2, trasferimento.getImporto());
                ps.setTimestamp(3, trasferimento.getData());
                ps.setString(4, trasferimento.getDescrizione());
                ps.setString(5, email_utente);
                ps.setInt(6, trasferimento.getIdContoMittente());
                ps.setInt(7, trasferimento.getIdContoDestinatario());
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE IL SALVATAGGIO DEL TRASFERIMENTO");
    }

    public int newID() throws EccezioniDatabase {
        return DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "SELECT MAX(id) AS max_id FROM Trasferimento";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_id") + 1;
                }
                return 1;
            }
        }, "ERRORE DURANTE IL RECUPERO DEL NUOVO ID TRASFERIMENTO");
    }
}