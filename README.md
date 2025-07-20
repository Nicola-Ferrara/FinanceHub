# FinanceHub

Un'applicazione web Java per la gestione delle finanze personali.

## Descrizione

FinanceHub è un'applicazione web sviluppata in Java che permette agli utenti di gestire le proprie finanze personali attraverso un'interfaccia web intuitiva. L'applicazione utilizza NanoHTTPD come server web embedded e PostgreSQL come database.

## Struttura del Progetto

- `src/` - Codice sorgente Java
  - `controller/` - Controller dell'applicazione
  - `dao/` - Data Access Objects per l'accesso al database
  - `dto/` - Data Transfer Objects (modelli dati)
  - `db_connection/` - Gestione connessioni database
  - `exception/` - Eccezioni personalizzate
  - `web_server/` - Server web e gestori delle pagine
- `sito/` - File statici del frontend (HTML, CSS, JS)
- `lib/` - Librerie JAR esterne
- `bin/` - File compilati
- `run.bat` - Script per avviare l'applicazione

## Tecnologie Utilizzate

- **Java**: Linguaggio di programmazione principale
- **NanoHTTPD**: Server web embedded
- **PostgreSQL**: Database relazionale
- **HTML/CSS/JavaScript**: Frontend

## Funzionalità

- Gestione utenti (registrazione, login)
- Gestione conti bancari
- Gestione transazioni
- Categorizzazione delle spese
- Trasferimenti tra conti
- Log delle operazioni

## Come Avviare

1. Assicurati di avere Java installato
2. Configura il database PostgreSQL
3. Compila il progetto:
   ```bash
   javac -cp "lib/*;pwd_DB" -d bin src/**/*.java
   ```
4. Avvia l'applicazione:
   ```bash
   run.bat
   ```
5. Apri il browser e vai su http://localhost:8080

## Dipendenze

- NanoHTTPD 2.3.1
- PostgreSQL JDBC Driver 42.7.6

## Struttura Database

L'applicazione utilizza le seguenti entità principali:
- Utenti
- Conti
- Transazioni
- Categorie
- Trasferimenti
- Log delle operazioni

## Contributi

Questo è un progetto di gestione delle finanze personali. Sentiti libero di contribuire con miglioramenti e nuove funzionalità.
