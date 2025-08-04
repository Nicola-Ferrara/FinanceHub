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
                    int idContoMittente = 0;
                    if (resultSet.getObject("id_conto_mittente") != null) {
                        idContoMittente = resultSet.getInt("id_conto_mittente");
                    }
                    int idContoDestinatario = 0;
                    if (resultSet.getObject("id_conto_destinatario") != null) {
                        idContoDestinatario = resultSet.getInt("id_conto_destinatario");
                    }
                    trasferimenti.add(new Trasferimento(resultSet.getInt("id"), resultSet.getDouble("importo"), resultSet.getTimestamp("data"), resultSet.getString("descrizione"), idContoMittente, idContoDestinatario, resultSet.getString("nome_conto_eliminato")));
                }
            }
            return trasferimenti;
        }, "ERRORE DURANTE IL RECUPERO DEI TRASFERIMENTI");
    }

    public void saveTrasferimento(Trasferimento trasferimento, String email_utente) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "INSERT INTO Trasferimento (id, importo, data, descrizione, email_utente, id_conto_mittente, id_conto_destinatario, nome_conto_eliminato) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, trasferimento.getID());
                ps.setDouble(2, trasferimento.getImporto());
                ps.setTimestamp(3, trasferimento.getData());
                ps.setString(4, trasferimento.getDescrizione());
                ps.setString(5, email_utente);
                
                if (trasferimento.getIdContoMittente() == 0) {
                    ps.setNull(6, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(6, trasferimento.getIdContoMittente());
                }
                if (trasferimento.getIdContoDestinatario() == 0) {
                    ps.setNull(7, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(7, trasferimento.getIdContoDestinatario());
                }
                
                ps.setString(8, trasferimento.getNomeContoEliminato());
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE IL SALVATAGGIO DEL TRASFERIMENTO");
    }

    public void deleteTrasferimento(int id) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "DELETE FROM Trasferimento WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE L'ELIMINAZIONE DEL TRASFERIMENTO");
    }

    public void updateTrasferimento(Trasferimento trasferimento) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "UPDATE Trasferimento SET id_conto_mittente = ?, id_conto_destinatario = ?, nome_conto_eliminato = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (trasferimento.getIdContoMittente() == 0) {
                    ps.setNull(1, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(1, trasferimento.getIdContoMittente());
                }
                
                if (trasferimento.getIdContoDestinatario() == 0) {
                    ps.setNull(2, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(2, trasferimento.getIdContoDestinatario());
                }
                ps.setString(3, trasferimento.getNomeContoEliminato());
                ps.setInt(4, trasferimento.getID());
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE L'AGGIORNAMENTO DEL TRASFERIMENTO");
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