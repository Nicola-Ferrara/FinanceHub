package controller;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.time.LocalDate;

import exception.*;
import web_server.*;
import dao.*;
import dto.*;

public class Controller {

    // Attributi
    private DAOFactory daoFactory;
    private Utente utente;

    // Costruttore
    public Controller() {
        this.daoFactory = DAOFactory.getInstance();
    }

    public boolean effettuaLogin(String email, String password) throws EccezioniDatabase {
        try {
            utente = daoFactory.getUtenteDAO().getUtente(email, password);
            if (utente == null) {
                return false;
            }
            utente.setLogOperazioni(daoFactory.getLogOperazioniDAO().getLogOperazioni(utente.getEmail()));
            utente.setConti(daoFactory.getContoDAO().getConti(utente.getEmail()));
            utente.setCategorie(daoFactory.getCategoriaDAO().getCategorie(utente.getEmail()));
            LinkedList<Trasferimento> trasferimenti = daoFactory.getTrasferimentoDAO().getTrasferimenti(utente.getEmail());
            for (Conto conto : utente.getConti()) {
                for (Trasferimento trasferimento : trasferimenti) {
                    if ((trasferimento.getIdContoMittente() == conto.getID()) || (trasferimento.getIdContoDestinatario() == conto.getID())) {
                        conto.addTrasferimento(trasferimento);
                    }
                }
            }
            for (Conto conto : utente.getConti()) {
                LinkedList<Transazione> transazioni = daoFactory.getTransazioneDAO().getTransazioni(conto.getID());
                for (Transazione transazione : transazioni) {
                    transazione.setCategoria(daoFactory.getCategoriaDAO().getCategoria(transazione.getIdCategoria()));
                }
                conto.setTransazioni(transazioni);
            }

            ricalcolaTuttiISaldi();
            return true;
        } catch (EccezioniDatabase e) {
            throw e;
        }
    }

    public void ricalcolaTuttiISaldi() {
        for (Conto conto : utente.getConti()) {
            conto.ricalcolaSaldo();
            try {
                daoFactory.getContoDAO().updateConto(conto);
            } catch (EccezioniDatabase e) {
                e.printStackTrace();
            }
        }
    }

    public boolean effettuaRegistrazione(String nome, String cognome, String telefono, String email, String password) throws EccezioniDatabase {
        try {
            utente = new Utente(nome, cognome, email, password, telefono, LocalDate.now(), null, null, null);
            daoFactory.getUtenteDAO().saveUtente(utente);
        } catch (EccezioniDatabase e) {
            if (e.getMessage().contains("23505")) {
                return false;
            }
            throw e;
        }
        setCategorieBase();
        return true;
    }

    public void setCategorieBase() throws EccezioniDatabase {
        try {
            Categoria categoria1 = new Categoria(daoFactory.getCategoriaDAO().newID(), "Guadagno", Categoria.TipoCategoria.Guadagno);
            daoFactory.getCategoriaDAO().saveCategoria(categoria1, utente.getEmail());
            utente.addCategoria(categoria1);
            Categoria categoria2 = new Categoria(daoFactory.getCategoriaDAO().newID(), "Spesa", Categoria.TipoCategoria.Spesa);
            daoFactory.getCategoriaDAO().saveCategoria(categoria2, utente.getEmail());
            utente.addCategoria(categoria2);
        } catch (EccezioniDatabase e) {
            throw e;
        }
    }

    public void clearUtente() {
        utente = null;
    }

    public String getNomeCognome() {
        return utente.getNome() + " " + utente.getCognome();
    }

    public LocalDate getDataIscrizioneUtente() {
        return utente.getDataIscrizione();
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
            contiAttivi.add(conto);
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

    public dto.Operazione[] getTutteOperazioni() {
        if (utente == null) return new dto.Operazione[0];
        
        java.util.List<Operazione> tutteOperazioni = new ArrayList<>();
        
        for (Conto conto : utente.getConti()) {
            tutteOperazioni.addAll(conto.getTransazioni());
            tutteOperazioni.addAll(conto.getTrasferimenti());
        }
        
        // Ordina per data (più recenti prima)
        tutteOperazioni.sort((a, b) -> b.getData().compareTo(a.getData()));
        
        return tutteOperazioni.toArray(new dto.Operazione[0]);
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

    public void modificaConto(int id, String nome, String tipo, double saldo_iniziale) throws EccezioniDatabase {
        for (Conto conto : utente.getConti()) {
            if (conto.getID() == id) {
                conto.setNome(nome);
                conto.setTipo(tipo);
                conto.setSaldo_iniziale(saldo_iniziale);
                conto.ricalcolaSaldo();
                try {
                    daoFactory.getContoDAO().updateConto(conto);
                } catch (EccezioniDatabase e) {
                    throw e;
                }
                return;
            }
        }
    }

    public void aggiungiConto(String nome, String tipo, double saldo) throws EccezioniDatabase {
        try {
            int id = daoFactory.getContoDAO().newID();
            Conto nuovoConto = new Conto(id, nome, tipo, saldo, null, null);
            daoFactory.getContoDAO().saveConto(nuovoConto, utente.getEmail());
            utente.addConto(nuovoConto);
        } catch (EccezioniDatabase e) {
            throw e;
        }
    }

    public void eliminaConto(int id) throws EccezioniDatabase {
        Conto del_conto = getContoById(id);
        if (del_conto == null) {
            return;
        }
        for (Conto conto : utente.getConti()) {
            for (Trasferimento trasferimento : conto.getTrasferimenti()) {
                if (trasferimento.getIdContoMittente() == id) {
                    trasferimento.setNomeContoEliminato(del_conto.getNome());
                    trasferimento.setIdContoMittente(0);
                    try {
                        daoFactory.getTrasferimentoDAO().updateTrasferimento(trasferimento);
                    } catch (EccezioniDatabase e) {
                        throw e;
                    }
                }
                if (trasferimento.getIdContoDestinatario() == id) {
                    trasferimento.setNomeContoEliminato(del_conto.getNome());
                    trasferimento.setIdContoDestinatario(0);
                    try {
                        daoFactory.getTrasferimentoDAO().updateTrasferimento(trasferimento);
                    } catch (EccezioniDatabase e) {
                        throw e;
                    }
                }
                if (trasferimento.getIdContoMittente() == 0 && trasferimento.getIdContoDestinatario() == 0) {
                    try {
                        daoFactory.getTrasferimentoDAO().deleteTrasferimento(trasferimento.getID());
                        conto.removeTrasferimento(trasferimento);
                    } catch (EccezioniDatabase e) {
                        throw e;
                    }
                }
            }
        }
        try {
            daoFactory.getContoDAO().deleteConto(id);
            utente.removeConto(del_conto);
        } catch (EccezioniDatabase e) {
            throw e;
        }
    }

    public void aggiungiTransazione(double importo, String descrizione, Categoria categoria, int idConto, Timestamp dataTransazione) throws EccezioniDatabase {
        try {
            int id = daoFactory.getTransazioneDAO().newID();
            Transazione transazione = new Transazione(id, importo, dataTransazione, descrizione, categoria);
            transazione.setIdCategoria(categoria.getID());
            daoFactory.getTransazioneDAO().saveTransazione(transazione, idConto, categoria.getID());
            for (Conto conto : utente.getConti()) {
                if (conto.getID() == idConto) {
                    conto.addTransazione(transazione);
                    
                    if (categoria.getTipo() == Categoria.TipoCategoria.Guadagno) {
                        conto.setSaldo_attuale(conto.getSaldo_attuale() + importo);
                    } else if (categoria.getTipo() == Categoria.TipoCategoria.Spesa) {
                        conto.setSaldo_attuale(conto.getSaldo_attuale() - importo);
                    }

                    try {
                        daoFactory.getContoDAO().updateConto(conto);
                    } catch (EccezioniDatabase e) {
                        throw e;
                    }
                    break;
                }
            }
        } catch (EccezioniDatabase e) {
            throw e;
        }
    }

    public void aggiungiTrasferimento(double importo, String descrizione, int idContoMittente, int idContoDestinatario) throws EccezioniDatabase {
        try {
            int id = daoFactory.getTrasferimentoDAO().newID();
            Timestamp data = new Timestamp(System.currentTimeMillis());
            Trasferimento trasferimento = new Trasferimento(id, importo, data, descrizione, idContoMittente, idContoDestinatario);
            daoFactory.getTrasferimentoDAO().saveTrasferimento(trasferimento, utente.getEmail());
            for (Conto conto : utente.getConti()) {
                if (conto.getID() == idContoMittente) {
                    conto.addTrasferimento(trasferimento);
                    conto.setSaldo_attuale(conto.getSaldo_attuale() - importo);
                    try {
                        daoFactory.getContoDAO().updateConto(conto);
                    } catch (EccezioniDatabase e) {
                        throw e;
                    }
                } else if (conto.getID() == idContoDestinatario) {
                    conto.addTrasferimento(trasferimento);
                    conto.setSaldo_attuale(conto.getSaldo_attuale() + importo);
                    try {
                        daoFactory.getContoDAO().updateConto(conto);
                    } catch (EccezioniDatabase e) {
                        throw e;
                    }
                }
            }
        } catch (EccezioniDatabase e) {
            throw e;
        }
    }

    public LinkedList<Conto> getSomeConti(int idConto) {
        LinkedList<Conto> someConti = new LinkedList<>();
        for (Conto conto : utente.getConti()) {
            if (conto.getID() != idConto) {
                someConti.add(conto);
            }
        }
        return someConti;
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

    public void aggiungiCategoria(String nome, Categoria.TipoCategoria tipo) throws EccezioniDatabase {
        try {
            int id = daoFactory.getCategoriaDAO().newID();
            Categoria nuovaCategoria = new Categoria(id, nome, tipo);
            daoFactory.getCategoriaDAO().saveCategoria(nuovaCategoria, utente.getEmail());
            utente.addCategoria(nuovaCategoria);
        } catch (EccezioniDatabase e) {
            throw e;
        }
    }

    public boolean eliminaCategoria(int id) throws EccezioniDatabase {
        Categoria categoria = getCategoriaById(id);
        for (Conto conto : utente.getConti()) {
            for (Transazione transazione : conto.getTransazioni()) {
                if (transazione.getIdCategoria() == id) {
                    int nuovoId = 0;
                    if (categoria.getTipo() == Categoria.TipoCategoria.Spesa) {
                        for (Categoria cat : utente.getCategorie()) {
                            if (cat.getNome().equals("Spesa")) {
                                transazione.setCategoria(cat);
                                nuovoId = cat.getID();
                                transazione.setIdCategoria(nuovoId);
                                break;
                            }
                        }
                    } else if (categoria.getTipo() == Categoria.TipoCategoria.Guadagno) {
                        for (Categoria cat : utente.getCategorie()) {
                            if (cat.getNome().equals("Guadagno")) {
                                transazione.setCategoria(cat);
                                nuovoId = cat.getID();
                                transazione.setIdCategoria(nuovoId);
                                break;
                            }
                        }
                    }
                    try {
                        daoFactory.getTransazioneDAO().updateTransazione(transazione, nuovoId);
                    } catch (EccezioniDatabase e) {
                        throw e;
                    }
                }
            }
        }
        try {
            daoFactory.getCategoriaDAO().deleteCategoria(id);
            utente.removeCategoria(categoria);
        } catch (EccezioniDatabase e) {
            throw e;
        }
        return true;
    }

    public boolean modificaCategoria(int id, String nome) {
        Categoria categoria = getCategoriaById(id);
        if (categoria == null) {
            return false;
        }
        categoria.setNome(nome);
        try {
            daoFactory.getCategoriaDAO().updateCategoria(categoria);
        } catch (EccezioniDatabase e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        try {
            Controller controller = new Controller();
            new WebServer(controller);
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}