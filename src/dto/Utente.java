package dto;

import java.util.*;
import java.time.LocalDate;

public class Utente {
	
	// Attributi
	private String nome;
	private String cognome;
	private String email;
	private String password;
	private String numero_tel;
    private LocalDate data_iscrizione;
	private LinkedList<Conto> conti = new LinkedList<Conto>();
    private LinkedList<Categoria> categorie = new LinkedList<Categoria>();
    private LinkedList<LogOperazione> logOperazioni = new LinkedList<LogOperazione>();
	
	// Costruttore
	public Utente(String nome, String cognome, String email, String password, String numero_tel, LocalDate data_iscrizione, LinkedList<Conto> conti, LinkedList<Categoria> categorie, LinkedList<LogOperazione> logOperazioni) {
	    this.nome = nome;
	    this.cognome = cognome;
	    this.email = email;
	    this.password = password;
	    this.numero_tel = numero_tel;
	    this.data_iscrizione = data_iscrizione;
	    this.conti = (conti != null) ? conti : new LinkedList<>();
        this.categorie = (categorie != null) ? categorie : new LinkedList<>();
        this.logOperazioni = (logOperazioni != null) ? logOperazioni : new LinkedList<>();
	}
	
	// Getters
    public String getNome() {
        return nome;
    }
    public String getCognome() {
        return cognome;
    }
    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }
    public String getNumeroTel() {
        return numero_tel;
    }
    public LocalDate getDataIscrizione() {
        return data_iscrizione;
    }   
    public LinkedList<Conto> getConti() {
		return conti;
	}
    public LinkedList<Categoria> getCategorie() {
        return categorie;
    }
    public LinkedList<LogOperazione> getLogOperazioni() {
        return logOperazioni;
    }

    // Setters
    public void setNome(String nome) {
        this.nome = nome;
    }
    public void setCognome(String cognome) {
        this.cognome = cognome;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setNumeroTel(String numero_tel) {
        this.numero_tel = numero_tel;
    }
    public void setDataIscrizione(LocalDate data_iscrizione) {
        this.data_iscrizione = data_iscrizione;
    }
    public void setConti(LinkedList<Conto> conti) {
		this.conti = conti;
	}
    public void setCategorie(LinkedList<Categoria> categorie) {
        this.categorie = categorie;
    }
    public void setLogOperazioni(LinkedList<LogOperazione> logOperazioni) {
        this.logOperazioni = logOperazioni;
    }
    
    // Aggiungi un conto
    public void addConto(Conto conto) {
		if (conto != null) {
			this.conti.add(conto);
		}
	}
    // Rimuovi un conto
    public void removeConto(Conto conto) {
        if (conto != null) {
            this.conti.remove(conto);
        }
    }
    
    // Aggiungi una categoria
    public void addCategoria(Categoria categoria) {
        if (categoria != null) {
            this.categorie.add(categoria);
        }
    }
    // Rimuovi una categoria
    public void removeCategoria(Categoria categoria) {
        if (categoria != null) {
            this.categorie.remove(categoria);
        }
    }

    // Aggiungi un log operazione
    public void addLogOperazione(LogOperazione logOperazione) {
        if (logOperazione != null) {
            this.logOperazioni.add(logOperazione);
        }
    }
    // Rimuovi un log operazione
    public void removeLogOperazione(LogOperazione logOperazione) {
        if (logOperazione != null) {
            this.logOperazioni.remove(logOperazione);
        }
    }

}
