package dao;

import java.sql.*;
import java.util.LinkedList;

import dto.Conto;

public class ContoDAO {
    
    // Attributi
    private Connection connection;

    // Costruttore
    public ContoDAO(Connection connection) {
        this.connection = connection;
    }

    public LinkedList<Conto> getConti(String email_utente) throws SQLException {
        LinkedList<Conto> conti = new LinkedList<>();
        String sql = "SELECT * FROM Conto WHERE email_utente = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email_utente);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                conti.add(new Conto(rs.getInt("id"), rs.getString("nome"), rs.getString("tipo"), rs.getDouble("saldo"), rs.getBoolean("attivo"), null, null));
            }
        }
        return conti;
    }

    public void updateConto(Conto conto) throws SQLException {
        String sql = "UPDATE Conto SET nome = ?, tipo = ?, attivo = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conto.getNome());
            ps.setString(2, conto.getTipo());
            ps.setBoolean(3, conto.getAttivo());
            ps.setInt(4, conto.getID());
            ps.executeUpdate();
        }
    }
    
    public void saveConto(Conto conto, String email_utente) throws SQLException {
        String sql = "INSERT INTO Conto (id, nome, tipo, saldo, attivo, email_utente) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, conto.getID());
            ps.setString(2, conto.getNome());
            ps.setString(3, conto.getTipo());
            ps.setDouble(4, conto.getSaldo());
            ps.setBoolean(5, conto.getAttivo());
            ps.setString(6, email_utente);
            ps.executeUpdate();
        }
    }

    public int newID() throws SQLException {
        String sql = "SELECT MAX(id) AS max_id FROM Conto";
        try (PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("max_id") + 1;
            }
            return 1;
        }
    }
}
