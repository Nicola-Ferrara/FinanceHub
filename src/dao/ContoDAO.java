package dao;

import java.sql.*;
import java.util.LinkedList;

import dto.Conto;
import exception.EccezioniDatabase;

public class ContoDAO {

    // Costruttore
    public ContoDAO() {}

    public LinkedList<Conto> getConti(String email_utente) throws EccezioniDatabase {
        return DAOFactory.getInstance().executeWithRetry((conn) -> {
            LinkedList<Conto> conti = new LinkedList<>();
            String sql = "SELECT * FROM Conto WHERE email_utente = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email_utente);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    conti.add(new Conto(rs.getInt("id"), rs.getString("nome"), rs.getString("tipo"), rs.getDouble("saldo_iniziale"), rs.getDouble("saldo_attuale"), rs.getBoolean("visibilita"),null, null));
                }
            }
            return conti;
        }, "ERRORE DURANTE IL RECUPERO DEI CONTI");
    }

    public void updateConto(Conto conto) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "UPDATE Conto SET nome = ?, tipo = ?, saldo_iniziale = ?, saldo_attuale = ?, visibilita = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, conto.getNome());
                ps.setString(2, conto.getTipo());
                ps.setDouble(3, conto.getSaldo_iniziale());
                ps.setDouble(4, conto.getSaldo_attuale());
                ps.setBoolean(5, conto.getVisibilità());
                ps.setInt(6, conto.getID());
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE L'AGGIORNAMENTO DEL CONTO");
    }
    
    public void saveConto(Conto conto, String email_utente) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "INSERT INTO Conto (id, nome, tipo, saldo_iniziale, saldo_attuale, visibilita, email_utente) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, conto.getID());
                ps.setString(2, conto.getNome());
                ps.setString(3, conto.getTipo());
                ps.setDouble(4, conto.getSaldo_iniziale());
                ps.setDouble(5, conto.getSaldo_attuale());
                ps.setBoolean(6, conto.getVisibilità());
                ps.setString(7, email_utente);
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE IL SALVATAGGIO DEL CONTO");
    }

    public void deleteConto(int id) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "DELETE FROM Conto WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE L'ELIMINAZIONE DEL CONTO");
    }

    public int newID() throws EccezioniDatabase {
        return DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "SELECT MAX(id) AS max_id FROM Conto";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_id") + 1;
                }
                return 1;
            }
        }, "ERRORE DURANTE IL RECUPERO DEL NUOVO ID CONTO");
    }
}
