package dto;

import java.sql.Timestamp;

public class Trasferimento {
    
    // Attributi
    private int ID;
    private double importo;
    private Timestamp data;
    private String descrizione;
    private int id_conto_mittente;
    private int id_conto_destinatario;

    // Costruttore
    public Trasferimento(int ID, double importo, Timestamp data, String descrizione, int id_conto_mittente, int id_conto_destinatario) {
        this.ID = ID;
        this.importo = importo;
        this.data = data;
        this.descrizione = descrizione;
        this.id_conto_mittente = id_conto_mittente;
        this.id_conto_destinatario = id_conto_destinatario;
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
    public int getIdContoMittente() {
        return id_conto_mittente;
    }
    public int getIdContoDestinatario() {
        return id_conto_destinatario;
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
    public void setIdContoMittente(int id_conto_mittente) {
        this.id_conto_mittente = id_conto_mittente;
    }
    public void setIdContoDestinatario(int id_conto_destinatario) {
        this.id_conto_destinatario = id_conto_destinatario;
    }
}
