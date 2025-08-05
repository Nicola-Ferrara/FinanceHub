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
                    return new Utente(rs.getString("nome"), rs.getString("cognome"), rs.getString("email"), rs.getString("password"), rs.getString("telefono"), rs.getDate("data_iscrizione").toLocalDate(), null, null);
                }
                return null;
            }
        }, "ERRORE DURANTE L'ACCESSO AL DATABASE PER IL RECUPERO DI TUTTI I DATI RIGUARDANTI L'UTENTE");
    }

    public void saveUtente(Utente utente) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "INSERT INTO Utente (nome, cognome, email, password, telefono, data_iscrizione) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, utente.getNome());
                ps.setString(2, utente.getCognome());
                ps.setString(3, utente.getEmail());
                ps.setString(4, utente.getPassword());
                ps.setString(5, utente.getNumeroTel());
                ps.setDate(6, java.sql.Date.valueOf(utente.getDataIscrizione()));
                ps.executeUpdate();
                return null;
            }
        }, "ERRORE DURANTE L'ACCESSO AL DATABASE PER IL SALVATAGGIO DEI DATI DELL'UTENTE");
    }

    public void updateUtente(Utente utente) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "UPDATE Utente SET nome = ?, cognome = ?, telefono = ?, password = ? WHERE email = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, utente.getNome());
                ps.setString(2, utente.getCognome());
                ps.setString(3, utente.getNumeroTel());
                ps.setString(4, utente.getPassword());
                ps.setString(5, utente.getEmail());
                ps.executeUpdate();
                return null;
            }
        }, "ERRORE DURANTE L'ACCESSO AL DATABASE PER L'AGGIORNAMENTO DEI DATI DELL'UTENTE");
    }

    public void deleteUtente(String email) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "DELETE FROM Utente WHERE email = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ps.executeUpdate();
                return null;
            }
        }, "ERRORE DURANTE L'ACCESSO AL DATABASE PER L'ELIMINAZIONE DELL'UTENTE");
    }
    
}
