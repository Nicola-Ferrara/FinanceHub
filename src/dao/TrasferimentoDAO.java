package dao;

import java.sql.*;
import java.util.LinkedList;

import dto.Trasferimento;

public class TrasferimentoDAO {
    
    // Attributi
    private Connection connection;

    // Costruttore
    public TrasferimentoDAO(Connection connection) {
        this.connection = connection;
    }

    public LinkedList<Trasferimento> getTrasferimenti(String email_utente) throws SQLException {
        LinkedList<Trasferimento> trasferimenti = new LinkedList<>();
        String sql = "SELECT * FROM Trasferimento WHERE email_utente = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email_utente);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                trasferimenti.add(new Trasferimento(resultSet.getInt("id"), resultSet.getDouble("importo"), resultSet.getTimestamp("data"), resultSet.getString("descrizione"), resultSet.getInt("id_conto_mittente"), resultSet.getInt("id_conto_destinatario")));
            }
        }
        return trasferimenti;
    }

    public void saveTrasferimento(Trasferimento trasferimento, String email_utente) throws SQLException {
        String sql = "INSERT INTO Trasferimento (id, importo, data, descrizione, email_utente, id_conto_mittente, id_conto_destinatario) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, trasferimento.getID());
            ps.setDouble(2, trasferimento.getImporto());
            ps.setTimestamp(3, trasferimento.getData());
            ps.setString(4, trasferimento.getDescrizione());
            ps.setString(5, email_utente);
            ps.setInt(6, trasferimento.getIdContoMittente());
            ps.setInt(7, trasferimento.getIdContoDestinatario());
            ps.executeUpdate();
        }
    }

    public int newID() throws SQLException {
        String sql = "SELECT MAX(id) AS max_id FROM Trasferimento";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("max_id") + 1;
            }
            return 1;
        }
    }
}
