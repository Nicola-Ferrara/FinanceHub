package dto;

import java.sql.Timestamp;

public class Transazione {
    
    // Attributi
    private int ID;
    private double importo;
    private Timestamp data;
    private String descrizione;
    private Categoria categoria;
    private int idCategoria;

    // Costruttore
    public Transazione(int ID, double importo, Timestamp data, String descrizione, Categoria categoria) {
        this.ID = ID;
        this.importo = importo;
        this.data = data;
        this.descrizione = descrizione;
        this.categoria = categoria;
    }

    // Getters
    public int getID() {
        return ID;
    }
    public double getImporto() {
        return importo;
    }
    public Timestamp getData() {
        return data;
    }
    public String getDescrizione() {
        return descrizione;
    }
    public Categoria getCategoria() {
        return categoria;
    }
    public int getIdCategoria() {
        return idCategoria;
    }

    // Setters
    public void setID(int ID) {
        this.ID = ID;
    }
    public void setImporto(double importo) {
        this.importo = importo;
    }
    public void setData(Timestamp data) {
        this.data = data;
    }
    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }
}
