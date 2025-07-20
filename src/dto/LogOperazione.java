package dto;

import java.sql.Timestamp;

public class LogOperazione {

    public enum TipoLog {
        CREAZIONE_UTENTE("Creazione Utente"),
        MODIFICA_NOME_UTENTE("Modifica Nome Utente"),
        MODIFICA_COGNOME_UTENTE("Modifica Cognome Utente"),
        MODIFICA_EMAIL_UTENTE("Modifica Email Utente"),
        MODIFICA_PASSWORD_UTENTE("Modifica Password Utente"),
        MODIFICA_NUMERO_DI_TELEFONO_UTENTE("Modifica Numero di Telefono Utente"),
        ELIMINAZIONE_UTENTE("Eliminazione Utente"),

        CREAZIONE_CONTO("Creazione Conto"),
        MODIFICA_NOME_CONTO("Modifica Nome Conto"),
        MODIFICA_TIPO_CONTO("Modifica Tipo Conto"),
        MODIFICA_SALDO_INIZIALE_CONTO("Modifica Saldo Iniziale Conto"),
        ELIMINAZIONE_CONTO("Eliminazione Conto"),

        CREAZIONE_TRANSAZIONE("Creazione Transazione"),
        ANNULLAMENTO_TRANSAZIONE("Annullamento Transazione"),

        CREAZIONE_TRASFERIMENTO("Creazione Trasferimento"),
        ANNULLAMENTO_TRASFERIMENTO("Annullamento Trasferimento"),

        CREAZIONE_CATEGORIA("Creazione Categoria"),
        ELIMINAZIONE_CATEGORIA("Eliminazione Categoria");

        private final String tipo;

        TipoLog(String tipo) {
            this.tipo = tipo;
        }

        public String getDescrizione() {
            return tipo;
        }
    }
    
    // Attributi
    private int ID;
    private TipoLog tipo;
    private String descrizione;
    private Timestamp data;

    // Costruttore
    public LogOperazione(int ID, TipoLog tipo, String descrizione, Timestamp data) {
        this.ID = ID;
        this.tipo = tipo;
        this.descrizione = descrizione;
        this.data = data;
    }

    // Getters
    public int getID() {
        return ID;
    }
    public TipoLog getTipo() {
        return tipo;
    }
    public String getDescrizione() {
        return descrizione;
    }
    public Timestamp getData() {
        return data;
    }

    // Setters
    public void setID(int ID) {
        this.ID = ID;
    }
    public void setTipo(TipoLog tipo) {
        this.tipo = tipo;
    }
    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
    public void setData(Timestamp data) {
        this.data = data;
    }
}
