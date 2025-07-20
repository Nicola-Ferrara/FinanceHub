package dto;

import java.sql.Timestamp;

public class Trasferimento extends Operazione {
    
    // Attributi specifici di Trasferimento
    private int id_conto_mittente;
    private int id_conto_destinatario;

    // Costruttore
    public Trasferimento(int ID, double importo, Timestamp data, String descrizione, int id_conto_mittente, int id_conto_destinatario) {
        super(ID, importo, data, descrizione);
        this.id_conto_mittente = id_conto_mittente;
        this.id_conto_destinatario = id_conto_destinatario;
    }

    // Getters
    public int getIdContoMittente() {
        return id_conto_mittente;
    }
    public int getIdContoDestinatario() {
        return id_conto_destinatario;
    }

    // Setters
    public void setIdContoMittente(int id_conto_mittente) {
        this.id_conto_mittente = id_conto_mittente;
    }
    public void setIdContoDestinatario(int id_conto_destinatario) {
        this.id_conto_destinatario = id_conto_destinatario;
    }
    
    // Implementazione del metodo astratto
    @Override
    public String getTipoOperazione() {
        return "TRASFERIMENTO";
    }
}