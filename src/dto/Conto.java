package dto;

import java.util.LinkedList;

public class Conto {
    
    // Attributi
    private int ID;
    private String nome;
    private String tipo;
    private double saldo_iniziale;
    private double saldo_attuale;
    private boolean visibilità;
    private LinkedList<Transazione> transazioni = new LinkedList<Transazione>();
    private LinkedList<Trasferimento> trasferimenti = new LinkedList<Trasferimento>();

    // Costruttori
    public Conto(int ID, String nome, String tipo, double saldo_iniziale, boolean visibilità, LinkedList<Transazione> transazioni, LinkedList<Trasferimento> trasferimenti) {
        this.ID = ID;
        this.nome = nome;
        this.tipo = tipo;
        this.saldo_iniziale = saldo_iniziale;
        this.saldo_attuale = saldo_iniziale;
        this.visibilità = visibilità;
        this.transazioni = (transazioni != null) ? transazioni : new LinkedList<>();
        this.trasferimenti = (trasferimenti != null) ? trasferimenti : new LinkedList<>();
    }
    public Conto(int ID, String nome, String tipo, double saldo_iniziale, double saldo_attuale, boolean visibilità, LinkedList<Transazione> transazioni, LinkedList<Trasferimento> trasferimenti) {
        this.ID = ID;
        this.nome = nome;
        this.tipo = tipo;
        this.saldo_iniziale = saldo_iniziale;
        this.saldo_attuale = saldo_attuale;
        this.visibilità = visibilità;
        this.transazioni = (transazioni != null) ? transazioni : new LinkedList<>();
        this.trasferimenti = (trasferimenti != null) ? trasferimenti : new LinkedList<>();
    }

    // Getters
    public int getID() {
        return ID;
    }
    public String getNome() {
        return nome;
    }
    public String getTipo() {
        return tipo;
    }
    public double getSaldo_iniziale() {
        return saldo_iniziale;
    }
    public double getSaldo_attuale() {
        return saldo_attuale;
    }
    public boolean getVisibilità() {
        return visibilità;
    }
    public LinkedList<Transazione> getTransazioni() {
        return transazioni;
    }
    public LinkedList<Trasferimento> getTrasferimenti() {
        return trasferimenti;
    }

    // Setters
    public void setID(int ID) {
        this.ID = ID;
    }
    public void setNome(String nome) {
        this.nome = nome;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    public void setSaldo_iniziale(double saldo) {
        this.saldo_iniziale = saldo;
    }
    public void setSaldo_attuale(double saldo) {
        this.saldo_attuale = saldo;
    }
    public void setVisibilità(boolean visibilità) {
        this.visibilità = visibilità;
    }
    public void setTransazioni(LinkedList<Transazione> transazioni) {
        this.transazioni = transazioni;
    }
    public void setTrasferimenti(LinkedList<Trasferimento> trasferimenti) {
        this.trasferimenti = trasferimenti;
    }

    // Aggiungi una transazione
    public void addTransazione(Transazione transazione) {
        if (transazione != null) {
            this.transazioni.add(transazione);
        }
    }
    // Rimuovi una transazione
    public void removeTransazione(Transazione transazione) {
        if (transazione != null) {
            this.transazioni.remove(transazione);
        }
    }

    // Aggiungi un trasferimento
    public void addTrasferimento(Trasferimento trasferimento) {
        if (trasferimento != null) {
            this.trasferimenti.add(trasferimento);
        }
    }
    // Rimuovi un trasferimento
    public void removeTrasferimento(Trasferimento trasferimento) {
        if (trasferimento != null) {
            this.trasferimenti.remove(trasferimento);
        }
    }

    // Ricalcola il saldo attuale
    public void ricalcolaSaldo() {
        saldo_attuale = saldo_iniziale;
        for (Transazione transazione : transazioni) {
            if (transazione.getCategoria().getTipo() == Categoria.TipoCategoria.Guadagno) {
                saldo_attuale += transazione.getImporto();
            } else {
                saldo_attuale -= transazione.getImporto();
            }
        }
        for (Trasferimento trasferimento : trasferimenti) {
            if (trasferimento.getIdContoDestinatario() == this.ID) {
                saldo_attuale += trasferimento.getImporto();
            } else if (trasferimento.getIdContoMittente() == this.ID) {
                saldo_attuale -= trasferimento.getImporto();
            }
        }
    }
}
