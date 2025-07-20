package dao;

import java.sql.*;
import java.util.LinkedList;

import dto.Categoria;

public class CategoriaDAO {
    
    // Attributi
    private Connection connection;

    // Costruttore
    public CategoriaDAO(Connection connection) {
        this.connection = connection;
    }

    public LinkedList<Categoria> getCategorie(String email_utente) throws SQLException, IllegalArgumentException {
        LinkedList<Categoria> categorie = new LinkedList<>();
        String sql = "SELECT * FROM Categoria WHERE email_utente = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email_utente);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                categorie.add(new Categoria(resultSet.getInt("id"), resultSet.getString("nome"), Categoria.TipoCategoria.valueOf(resultSet.getString("tipo"))));
            }
        }
        return categorie;
    }

    public Categoria getCategoria(int id) throws SQLException {
        String sql = "SELECT * FROM Categoria WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                return new Categoria(
                    resultSet.getInt("id"), 
                    resultSet.getString("nome"), 
                    Categoria.TipoCategoria.valueOf(resultSet.getString("tipo"))
                );
            }
        }
        return null;
    }

    public void deleteCategoria(int id) throws SQLException {
        String sql = "DELETE FROM Categoria WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void saveCategoria(Categoria categoria, String email_utente) throws SQLException {
        String sql = "INSERT INTO Categoria (id, nome, tipo, email_utente) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, categoria.getID());
            ps.setString(2, categoria.getNome());
            ps.setString(3, categoria.getTipo().name());
            ps.setString(4, email_utente);
            ps.executeUpdate();
        }
    }

    public int newID() throws SQLException {
        String sql = "SELECT MAX(id) AS max_id FROM Categoria";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("max_id") + 1;
            }
            return 1;
        }
    }
}
