package dto;

import java.sql.Timestamp;

public abstract class Operazione {
    
    // Attributi comuni
    protected int ID;
    protected double importo;
    protected Timestamp data;
    protected String descrizione;

    // Costruttore
    public Operazione(int ID, double importo, Timestamp data, String descrizione) {
        this.ID = ID;
        this.importo = importo;
        this.data = data;
        this.descrizione = descrizione;
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
    // Metodo astratto che ogni sottoclasse deve implementare
    public abstract String getTipoOperazione();

}