package controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
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
        //utente.setTrasferimenti(trasferimentoDAO.getTrasferimenti(utente.getEmail()));
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
            utente = new Utente(nome, cognome, email, password, telefono, null, null, null, null);
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
        for (Conto conto: utente.getConti()) {
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
        for (Conto conto: utente.getConti()) {
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
        return utente.getConti();
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