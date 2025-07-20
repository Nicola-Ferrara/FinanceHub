package dao;

import java.sql.*;
import dto.Utente;

public class UtenteDAO {
    
    // Attributi
    private Connection connection;

    // Costruttore
    public UtenteDAO(Connection connection) {
        this.connection = connection;
    }

    public Utente getUtente(String email, String password) throws SQLException {
        String sql = "SELECT * FROM Utente WHERE email = ? AND password = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Utente(rs.getString("nome"), rs.getString("cognome"), rs.getString("email"), rs.getString("password"), rs.getString("telefono"), null, null, null, null);
            }
            return null;
        }
    }

    public void saveUtente(Utente utente) throws SQLException {
        String sql = "INSERT INTO Utente (nome, cognome, email, password, telefono) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, utente.getNome());
            ps.setString(2, utente.getCognome());
            ps.setString(3, utente.getEmail());
            ps.setString(4, utente.getPassword());
            ps.setString(5, utente.getNumeroTel());
            ps.executeUpdate();
        }
    }
    
}
