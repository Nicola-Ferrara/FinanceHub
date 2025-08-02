package dao;

import java.sql.*;

import dto.Utente;
import exception.EccezioniDatabase;

public class UtenteDAO {

    // Costruttore
    public UtenteDAO() {}

    public Utente getUtente(String email, String password) throws EccezioniDatabase {
        return DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "SELECT * FROM Utente WHERE email = ? AND password = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new Utente(rs.getString("nome"), rs.getString("cognome"), rs.getString("email"), rs.getString("password"), rs.getString("telefono"), null, null, null);
                }
                return null;
            }
        }, "ERRORE DURANTE L'ACCESSO AL DATABASE PER IL RECUPERO DI TUTTI I DATI RIGUARDANTI L'UTENTE");
    }

    public void saveUtente(Utente utente) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "INSERT INTO Utente (nome, cognome, email, password, telefono) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, utente.getNome());
                ps.setString(2, utente.getCognome());
                ps.setString(3, utente.getEmail());
                ps.setString(4, utente.getPassword());
                ps.setString(5, utente.getNumeroTel());
                ps.executeUpdate();
                return null;
            }
        }, "ERRORE DURANTE L'ACCESSO AL DATABASE PER IL SALVATAGGIO DEI DATI DELL'UTENTE");
    }
    
}
