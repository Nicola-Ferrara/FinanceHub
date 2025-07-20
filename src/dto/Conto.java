package dto;

import java.util.LinkedList;

public class Conto {
    
    // Attributi
    private int ID;
    private String nome;
    private String tipo;
    private double saldo;
    private LinkedList<Transazione> transazioni = new LinkedList<Transazione>();
    private LinkedList<Trasferimento> trasferimenti = new LinkedList<Trasferimento>();

    // Costruttore
    public Conto(int ID, String nome, String tipo, double saldo, LinkedList<Transazione> transazioni, LinkedList<Trasferimento> trasferimenti) {
        this.ID = ID;
        this.nome = nome;
        this.tipo = tipo;
        this.saldo = saldo;
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
    public double getSaldo() {
        return saldo;
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
    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }
    public void setTransazioni(LinkedList<Transazione> transazioni) {
        this.transazioni = transazioni;
        for (Transazione transazione : transazioni) {
            if(transazione.getCategoria().getTipo() == Categoria.TipoCategoria.Guadagno) {
                saldo += transazione.getImporto();
            } else if(transazione.getCategoria().getTipo() == Categoria.TipoCategoria.Spesa) {
                saldo -= transazione.getImporto();
            }
        }
    }
    public void setTrasferimenti(LinkedList<Trasferimento> trasferimenti) {
        this.trasferimenti = trasferimenti;
        for (Trasferimento trasferimento : trasferimenti) {
            if(trasferimento.getIdContoDestinatario() == this.ID) {
                saldo += trasferimento.getImporto();
            } else if(trasferimento.getIdContoMittente() == this.ID) {
                saldo -= trasferimento.getImporto();
            }
        }
    }

    // Aggiungi una transazione
    public void addTransazione(Transazione transazione) {
        if (transazione != null) {
            this.transazioni.add(transazione);
        }
        if (transazione.getCategoria().getTipo() == Categoria.TipoCategoria.Guadagno) {
            saldo += transazione.getImporto();
        } else if (transazione.getCategoria().getTipo() == Categoria.TipoCategoria.Spesa) {
            saldo -= transazione.getImporto();
        }
    }
    // Rimuovi una transazione
    public void removeTransazione(Transazione transazione) {
        if (transazione != null) {
            this.transazioni.remove(transazione);
        }
        if (transazione.getCategoria().getTipo() == Categoria.TipoCategoria.Guadagno) {
            saldo -= transazione.getImporto();
        } else if (transazione.getCategoria().getTipo() == Categoria.TipoCategoria.Spesa) {
            saldo += transazione.getImporto();
        }
    }

    // Aggiungi un trasferimento
    public void addTrasferimento(Trasferimento trasferimento) {
        if (trasferimento != null) {
            this.trasferimenti.add(trasferimento);
        }
        if (trasferimento.getIdContoDestinatario() == this.ID) {
            saldo += trasferimento.getImporto();
        } else if (trasferimento.getIdContoMittente() == this.ID) {
            saldo -= trasferimento.getImporto();
        }
    }
    // Rimuovi un trasferimento
    public void removeTrasferimento(Trasferimento trasferimento) {
        if (trasferimento != null) {
            this.trasferimenti.remove(trasferimento);
        }
        if (trasferimento.getIdContoDestinatario() == this.ID) {
            saldo -= trasferimento.getImporto();
        } else if (trasferimento.getIdContoMittente() == this.ID) {
            saldo += trasferimento.getImporto();
        }
    }
}
