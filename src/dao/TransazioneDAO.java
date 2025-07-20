package dao;

import java.sql.*;
import java.util.LinkedList;

import dto.Transazione;

public class TransazioneDAO {
    
    // Attributi
    private Connection connection;

    // Costruttore
    public TransazioneDAO(Connection connection) {
        this.connection = connection;
    }

    public LinkedList<Transazione> getTransazioni(int id_conto) throws SQLException {
        LinkedList<Transazione> transazioni = new LinkedList<>();
        String sql = "SELECT * FROM Transazione WHERE id_conto = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id_conto);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                Transazione t = new Transazione(resultSet.getInt("id"), resultSet.getDouble("importo"), resultSet.getTimestamp("data"), resultSet.getString("descrizione"), null);
                t.setIdCategoria(resultSet.getInt("id_categoria"));
                transazioni.add(t);
            }
        }
        return transazioni;
    }

    public void saveTransazione(Transazione transazione, int id_conto, int id_categoria) throws SQLException {
        String sql = "INSERT INTO Transazione (id, importo, data, descrizione, id_conto, id_categoria) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, transazione.getID());
            ps.setDouble(2, transazione.getImporto());
            ps.setTimestamp(3, transazione.getData());
            ps.setString(4, transazione.getDescrizione());
            ps.setInt(5, id_conto);
            ps.setInt(6, id_categoria);
            ps.executeUpdate();
        }
    }

    public int newID() throws SQLException {
        String sql = "SELECT MAX(id) AS max_id FROM Transazione";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("max_id") + 1;
            }
            return 1;
        }
    }
}
