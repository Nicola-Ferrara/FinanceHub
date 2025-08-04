package dto;

import java.sql.Timestamp;

public class Trasferimento extends Operazione {
    
    // Attributi specifici di Trasferimento
    private int id_conto_mittente;
    private int id_conto_destinatario;
    private String nome_conto_eliminato;

    // Costruttore
    public Trasferimento(int ID, double importo, Timestamp data, String descrizione, int id_conto_mittente, int id_conto_destinatario) {
        super(ID, importo, data, descrizione);
        this.id_conto_mittente = id_conto_mittente;
        this.id_conto_destinatario = id_conto_destinatario;
    }
    public Trasferimento(int ID, double importo, Timestamp data, String descrizione, int id_conto_mittente, int id_conto_destinatario, String nome_conto_eliminato) {
        super(ID, importo, data, descrizione);
        this.id_conto_mittente = id_conto_mittente;
        this.id_conto_destinatario = id_conto_destinatario;
        this.nome_conto_eliminato = nome_conto_eliminato;
    }

    // Getters
    public int getIdContoMittente() {
        return id_conto_mittente;
    }
    public int getIdContoDestinatario() {
        return id_conto_destinatario;
    }
    public String getNomeContoEliminato() {
        return nome_conto_eliminato;
    }

    // Setters
    public void setIdContoMittente(int id_conto_mittente) {
        this.id_conto_mittente = id_conto_mittente;
    }
    public void setIdContoDestinatario(int id_conto_destinatario) {
        this.id_conto_destinatario = id_conto_destinatario;
    }
    public void setNomeContoEliminato(String nome_conto_eliminato) {
        this.nome_conto_eliminato = nome_conto_eliminato;
    }
    
    // Implementazione del metodo astratto
    @Override
    public String getTipoOperazione() {
        return "TRASFERIMENTO";
    }
}