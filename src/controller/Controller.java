package controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import db_connection.*;
import exception.*;
import web_server.*;
import dao.*;
import dto.*;

public class Controller {

    // Attributi
    private UtenteDAO utenteDAO;
    private CategoriaDAO categoriaDAO;
    private ContoDAO contoDAO;
    private LogOperazioniDAO logOperazioniDAO;
    private TransazioneDAO transazioneDAO;
    private TrasferimentoDAO trasferimentoDAO;

    private Utente utente;

    // Costruttore
    public Controller(Connection conn) {
        this.utenteDAO = new UtenteDAO(conn);
        this.categoriaDAO = new CategoriaDAO(conn);
        this.contoDAO = new ContoDAO(conn);
        this.logOperazioniDAO = new LogOperazioniDAO(conn);
        this.transazioneDAO = new TransazioneDAO(conn);
        this.trasferimentoDAO = new TrasferimentoDAO(conn);
    }

    public boolean effettuaLogin(String email, String password) {
        try {
            utente = utenteDAO.getUtente(email, password);
            if (utente == null) {
                return false;
            }
            utente.setLogOperazioni(logOperazioniDAO.getLogOperazioni(utente.getEmail()));
            utente.setConti(contoDAO.getConti(utente.getEmail()));
            utente.setCategorie(categoriaDAO.getCategorie(utente.getEmail()));
            LinkedList<Trasferimento> trasferimenti = trasferimentoDAO.getTrasferimenti(utente.getEmail());
            for (Conto conto : utente.getConti()) {
                for (Trasferimento trasferimento : trasferimenti) {
                    if ((trasferimento.getIdContoMittente() == conto.getID()) || (trasferimento.getIdContoDestinatario() == conto.getID())) {
                        conto.addTrasferimento(trasferimento);
                    }
                }
            }
            for (Conto conto : utente.getConti()) {
                LinkedList<Transazione> transazioni = transazioneDAO.getTransazioni(conto.getID());
                for (Transazione transazione : transazioni) {
                    transazione.setCategoria(categoriaDAO.getCategoria(transazione.getIdCategoria()));
                }
                conto.setTransazioni(transazioni);
            }
            return true;
        } catch (SQLException e) {
            throw new EccezioniDatabase("ERRORE DURANTE L'ACCESSO AL DATABASE PER IL RECUPERO DI TUTTI I DATI RIGUARDANTI L'UTENTE", e);
        }
    }

    public boolean effettuaRegistrazione(String nome, String cognome, String telefono, String email, String password) {
        try {
            utente = new Utente(nome, cognome, email, password, telefono, null, null, null);
            utenteDAO.saveUtente(utente);
        } catch (SQLException e) {
			if (e.getSQLState().equals("23505")) {
			    return false;
			}
			throw new EccezioniDatabase("ERRORE DURANTE L'ACCESSO AL DATABASE PER INSERIRE UN NUOVO UTENTE", e);
		}
        return true;
    }

    public void clearUtente() {
        utente = null;
    }

    public String getNomeCognome() {
        return utente.getNome() + " " + utente.getCognome();
    }

    public boolean isUtenteLogged() {
        return utente != null;
    }

    public double calcolaEntrate() {
    double entrate = 0.0;
    int meseCorrente = (new Timestamp(System.currentTimeMillis())).toLocalDateTime().getMonthValue();
    int annoCorrente = (new Timestamp(System.currentTimeMillis())).toLocalDateTime().getYear();
    for (Conto conto: getConti()) { 
        for (Transazione transazione : conto.getTransazioni()) {
            if (transazione.getCategoria().getTipo() == Categoria.TipoCategoria.Guadagno) {
                int meseTransazione = transazione.getData().toLocalDateTime().getMonthValue();
                int annoTransazione = transazione.getData().toLocalDateTime().getYear();
                if (meseTransazione == meseCorrente && annoTransazione == annoCorrente) {
                    entrate += transazione.getImporto();
                }
            }
        }
    }
    return entrate;
}

    public double calcolaUscite() {
        double uscite = 0.0;
        int meseCorrente = (new Timestamp(System.currentTimeMillis())).toLocalDateTime().getMonthValue();
        int annoCorrente = (new Timestamp(System.currentTimeMillis())).toLocalDateTime().getYear();
        for (Conto conto: getConti()) { 
            for (Transazione transazione : conto.getTransazioni()) {
                if (transazione.getCategoria().getTipo() == Categoria.TipoCategoria.Spesa) {
                    int meseTransazione = transazione.getData().toLocalDateTime().getMonthValue();
                    int annoTransazione = transazione.getData().toLocalDateTime().getYear();
                    if (meseTransazione == meseCorrente && annoTransazione == annoCorrente) {
                        uscite += transazione.getImporto();
                    }
                }
            }
        }
        return uscite;
    }

    public LinkedList<Conto> getConti() {
        LinkedList<Conto> contiAttivi = new LinkedList<>();
        for (Conto conto : utente.getConti()) {
            if (conto.getAttivo()) { // ✅ Filtra solo attivi per la UI
                contiAttivi.add(conto);
            }
        }
        return contiAttivi;
    }

    public LinkedList<Conto> getTuttiConti() {
        return utente.getConti();
    }

    public Operazione[] getUltimeOperazioni() {
        ArrayList<Operazione> tutteLeOperazioni = new ArrayList<>();
        ArrayList<Integer> trasferimentiGiaAggiunti = new ArrayList<>();
        for (Conto conto : getConti()) {
            if (conto.getTransazioni() != null) {
                tutteLeOperazioni.addAll(conto.getTransazioni());
            }
            if (conto.getTrasferimenti() != null) {
                for (Trasferimento trasferimento : conto.getTrasferimenti()) {
                    if (!trasferimentiGiaAggiunti.contains(trasferimento.getID())) {
                        tutteLeOperazioni.add(trasferimento);
                        trasferimentiGiaAggiunti.add(trasferimento.getID());
                    }
                }
            }
        }
        Collections.sort(tutteLeOperazioni, new Comparator<Operazione>() {
            @Override
            public int compare(Operazione o1, Operazione o2) {
                return o2.getData().compareTo(o1.getData());
            }
        });
        int dimensione = Math.min(5, tutteLeOperazioni.size());
        Operazione[] ultimeDieciOperazioni = new Operazione[dimensione];
        for (int i = 0; i < dimensione; i++) {
            ultimeDieciOperazioni[i] = tutteLeOperazioni.get(i);
        }
        return ultimeDieciOperazioni;
    }

    public Conto getContoById(int id) {
        for (Conto conto : utente.getConti()) {
            if (conto.getID() == id) {
                return conto;
            }
        }
        return null;
    }

    public Operazione[] getOperazioniConto(int contoId) {
        Conto conto = getContoById(contoId);
        if (conto == null) {
            return new Operazione[0];
        }    
        ArrayList<Operazione> operazioni = new ArrayList<>();
        if (conto.getTransazioni() != null) {
            operazioni.addAll(conto.getTransazioni());
        }
        if (conto.getTrasferimenti() != null) {
            operazioni.addAll(conto.getTrasferimenti());
        }
        Collections.sort(operazioni, new Comparator<Operazione>() {
            @Override
            public int compare(Operazione o1, Operazione o2) {
                return o2.getData().compareTo(o1.getData());
            }
        });
        return operazioni.toArray(new Operazione[0]);
    }

    public void modificaConto(int id, String nome, boolean attivo, String tipo) {
        for (Conto conto : utente.getConti()) {
            if (conto.getID() == id) {
                conto.setNome(nome);
                conto.setTipo(tipo);
                conto.setAttivo(attivo);
                try {
                    contoDAO.updateConto(conto);
                } catch (SQLException e) {
                    throw new EccezioniDatabase("ERRORE DURANTE L'ACCESSO AL DATABASE PER MODIFICARE UN CONTO", e);
                }
                return;
            }
        }
    }

    public void aggiungiConto(String nome, String tipo, double saldo) {
        try {
            int id = contoDAO.newID();
            Conto nuovoConto = new Conto(id, nome, tipo, saldo, true, null, null);
            contoDAO.saveConto(nuovoConto, utente.getEmail());
            utente.addConto(nuovoConto);
        } catch (SQLException e) {
            throw new EccezioniDatabase("ERRORE DURANTE L'ACCESSO AL DATABASE PER OTTENERE UN NUOVO ID PER IL CONTO", e);
        }
    }

    public void aggiungiTransazione(double importo, String descrizione, Categoria categoria, int idConto) {
        try{
            int id = transazioneDAO.newID();
            Timestamp data = new Timestamp(System.currentTimeMillis());
            Transazione transazione = new Transazione(id, importo, data, descrizione, categoria);
            transazioneDAO.saveTransazione(transazione, idConto, categoria.getID());
            for (Conto conto : utente.getConti()) {
                if (conto.getID() == idConto) {
                    conto.addTransazione(transazione);
                    break;
                }
            }
        } catch (SQLException e) {
            throw new EccezioniDatabase("ERRORE DURANTE L'ACCESSO AL DATABASE PER INSERIRE UNA NUOVA TRANSAZIONE", e);
        }
    }

    public LinkedList<Categoria> getCategoriaGuadagno() {
        LinkedList<Categoria> categorieGuadagno = new LinkedList<>();
        for (Categoria categoria : utente.getCategorie()) {
            if (categoria.getTipo() == Categoria.TipoCategoria.Guadagno) {
                categorieGuadagno.add(categoria);
            }
        }
        return categorieGuadagno;
    }

    public LinkedList<Categoria> getCategoriaSpesa() {
        LinkedList<Categoria> categorieSpesa = new LinkedList<>();
        for (Categoria categoria : utente.getCategorie()) {
            if (categoria.getTipo() == Categoria.TipoCategoria.Spesa) {
                categorieSpesa.add(categoria);
            }
        }
        return categorieSpesa;
    }

    public Categoria getCategoriaById(int id) {
    for (Categoria categoria : utente.getCategorie()) {
        if (categoria.getID() == id) {
            return categoria;
        }
    }
    return null;
}

    public static void main(String[] args) {
        try {
            // Inizializza la connessione al database
            DBConnection dbConnection = DBConnection.getDBConnection();
            Connection conn = dbConnection.getConnection();
            Controller controller = new Controller(conn);

            // Avvia il WebServer
            new WebServer(controller);
        } catch (NullPointerException e) {
            EccezioneNull.errorePWD(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}