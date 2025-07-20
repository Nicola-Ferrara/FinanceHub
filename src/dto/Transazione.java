package dto;

import java.sql.Timestamp;

public class Transazione extends Operazione {
    
    // Attributi specifici di Transazione
    private Categoria categoria;
    private int idCategoria;

    // Costruttore
    public Transazione(int ID, double importo, Timestamp data, String descrizione, Categoria categoria) {
        super(ID, importo, data, descrizione);
        this.categoria = categoria;
    }
    
    // Costruttore alternativo con idCategoria
    public Transazione(int ID, double importo, Timestamp data, String descrizione, int idCategoria) {
        super(ID, importo, data, descrizione);
        this.idCategoria = idCategoria;
    }

    // Getters
    public Categoria getCategoria() {
        return categoria;
    }
    public int getIdCategoria() {
        return idCategoria;
    }

    // Setters
    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    @Override
    public String getTipoOperazione() {
        return "TRANSAZIONE";
    }
}