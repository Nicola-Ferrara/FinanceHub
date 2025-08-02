package dao;

import java.sql.*;
import java.util.LinkedList;

import dto.Transazione;
import exception.EccezioniDatabase;

public class TransazioneDAO {
    
    public TransazioneDAO() {}

    public LinkedList<Transazione> getTransazioni(int id_conto) throws EccezioniDatabase {
        return DAOFactory.getInstance().executeWithRetry((conn) -> {
            LinkedList<Transazione> transazioni = new LinkedList<>();
            String sql = "SELECT * FROM Transazione WHERE id_conto = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id_conto);
                ResultSet resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    Transazione t = new Transazione(resultSet.getInt("id"), resultSet.getDouble("importo"), resultSet.getTimestamp("data"), resultSet.getString("descrizione"), null);
                    t.setIdCategoria(resultSet.getInt("id_categoria"));
                    transazioni.add(t);
                }
            }
            return transazioni;
        }, "ERRORE DURANTE IL RECUPERO DELLE TRANSAZIONI");
    }

    public void saveTransazione(Transazione transazione, int id_conto, int id_categoria) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "INSERT INTO Transazione (id, importo, data, descrizione, id_conto, id_categoria) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, transazione.getID());
                ps.setDouble(2, transazione.getImporto());
                ps.setTimestamp(3, transazione.getData());
                ps.setString(4, transazione.getDescrizione());
                ps.setInt(5, id_conto);
                ps.setInt(6, id_categoria);
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE IL SALVATAGGIO DELLA TRANSAZIONE");
    }

    public void updateTransazione(Transazione transazione, int idCategoria) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "UPDATE Transazione SET id_categoria = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCategoria);
                ps.setInt(2, transazione.getID());
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE L'AGGIORNAMENTO DELLA TRANSAZIONE");
    }

    public int newID() throws EccezioniDatabase {
        return DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "SELECT MAX(id) AS max_id FROM Transazione";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_id") + 1;
                }
                return 1;
            }
        }, "ERRORE DURANTE IL RECUPERO DEL NUOVO ID TRANSAZIONE");
    }
}