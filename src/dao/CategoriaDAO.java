package dao;

import java.sql.*;
import java.util.LinkedList;

import dto.Categoria;
import exception.EccezioniDatabase;

public class CategoriaDAO {

    // Costruttore
    public CategoriaDAO() {}

    public LinkedList<Categoria> getCategorie(String email_utente) throws EccezioniDatabase {
        return DAOFactory.getInstance().executeWithRetry((conn) -> {
            LinkedList<Categoria> categorie = new LinkedList<>();
            String sql = "SELECT * FROM Categoria WHERE email_utente = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email_utente);
                ResultSet resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    categorie.add(new Categoria(resultSet.getInt("id"), resultSet.getString("nome"), Categoria.TipoCategoria.valueOf(resultSet.getString("tipo"))));
                }
            }
            return categorie;
        }, "ERRORE DURANTE L'ACCESSO AL DATABASE PER IL RECUPERO DELLE CATEGORIE");
    }

    public Categoria getCategoria(int id) throws EccezioniDatabase {
        return DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "SELECT * FROM Categoria WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ResultSet resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    return new Categoria(resultSet.getInt("id"), resultSet.getString("nome"), Categoria.TipoCategoria.valueOf(resultSet.getString("tipo")));
                }
            }
            return null;
        }, "ERRORE DURANTE L'ACCESSO AL DATABASE PER IL RECUPERO DELLA CATEGORIA");
    }

    public void deleteCategoria(int id) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "DELETE FROM Categoria WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE L'ELIMINAZIONE DELLA CATEGORIA");
    }   

    public void saveCategoria(Categoria categoria, String email_utente) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "INSERT INTO Categoria (id, nome, tipo, email_utente) VALUES (?, ?, ?::TipoCategoria, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, categoria.getID());
                ps.setString(2, categoria.getNome());
                ps.setString(3, categoria.getTipo().name());
                ps.setString(4, email_utente);
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE IL SALVATAGGIO DELLA CATEGORIA");
    }

    public void updateCategoria(Categoria categoria) throws EccezioniDatabase {
        DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "UPDATE Categoria SET nome = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, categoria.getNome());
                ps.setInt(2, categoria.getID());
                ps.executeUpdate();
            }
            return null;
        }, "ERRORE DURANTE L'AGGIORNAMENTO DELLA CATEGORIA");
    }

    public int newID() throws EccezioniDatabase {
        return DAOFactory.getInstance().executeWithRetry((conn) -> {
            String sql = "SELECT MAX(id) AS max_id FROM Categoria";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_id") + 1;
                }
                return 1;
            }
        }, "ERRORE DURANTE IL RECUPERO DEL NUOVO ID CATEGORIA");
    }
}
