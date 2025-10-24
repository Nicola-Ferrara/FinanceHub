package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import java.util.List;

import controller.Controller;
import dto.*;

public class GestoreHome extends BaseGestorePagina {
    
    public GestoreHome(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return "/home".equals(uri) || 
               "/api/bilancio".equals(uri) || 
               "/api/conti".equals(uri) ||
               "/api/operazioni-home".equals(uri) ||
               "/logout".equals(uri);
    }
    
    @Override
    public Response handle(IHTTPSession session) throws Exception {
        String uri = session.getUri();
        Method method = session.getMethod();
        
        if ("/home".equals(uri) && method == Method.GET) {
            return serveHomePage(session);
        } else if ("/api/bilancio".equals(uri) && method == Method.GET) {
            return handleBalance(session);  // <-- Passa session
        } else if ("/api/conti".equals(uri) && method == Method.GET) {
            return handleAccounts(session);  // <-- Passa session
        } else if ("/api/operazioni-home".equals(uri) && method == Method.GET) {
            return handleOperazioni(session);  // <-- Passa session
        } else if ("/logout".equals(uri) && method == Method.GET) {
            return handleLogout(session);
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }
    
    private Response serveHomePage(IHTTPSession session) {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            String htmlPath = "home.html";  // <-- Usa il metodo leggiFile
            String content = leggiFile(htmlPath);
            
            if (content == null) {
                return createResponse(Response.Status.INTERNAL_ERROR, "text/html", 
                    "<h1>Errore durante il caricamento della pagina</h1>");
            }
            
            // Sostituisci il segnaposto con il nome e cognome dell'utente
            String nomeCognome = sessionController.getNomeCognome();  // <-- Usa sessionController
            content = content.replace("{{nomeCognome}}", nomeCognome);
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }
    
    private Response handleBalance(IHTTPSession session) {  // <-- Aggiungi parametro
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            double entrate = sessionController.calcolaEntrate();  // <-- Usa sessionController
            double uscite = sessionController.calcolaUscite();

            String json = String.format(java.util.Locale.US, "{\"entrate\": %.2f, \"uscite\": %.2f}", entrate, uscite);

            Response response = createResponse(Response.Status.OK, "application/json", json);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il calcolo del bilancio: " + e.getMessage() + "\"}");
        }
    }
    
    private Response handleAccounts(IHTTPSession session) {  // <-- Aggiungi parametro
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            List<Conto> conti = sessionController.getContiVisibili();  // <-- Usa sessionController
            
            StringBuilder jsonBuilder = new StringBuilder("[");
            for (int i = 0; i < conti.size(); i++) {
                Conto conto = conti.get(i);
                jsonBuilder.append(String.format(java.util.Locale.US, 
                    "{\"id\": %d, \"nome\": \"%s\", \"tipo\": \"%s\", \"saldo\": %.2f}",
                    conto.getID(), conto.getNome(), conto.getTipo(), conto.getSaldo_attuale()));
                
                if (i < conti.size() - 1) {
                    jsonBuilder.append(",");
                }
            }
            jsonBuilder.append("]");
            
            String json = jsonBuilder.toString();
            
            Response response = createResponse(Response.Status.OK, "application/json", json);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero dei conti: " + e.getMessage() + "\"}");
        }
    }

    private Response handleOperazioni(IHTTPSession session) {  // <-- Aggiungi parametro
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            Operazione[] operazioni = sessionController.getUltimeOperazioni();  // <-- Usa sessionController
            
            StringBuilder jsonBuilder = new StringBuilder("[");
            for (int i = 0; i < operazioni.length; i++) {
                Operazione operazione = operazioni[i];
                
                String tipo;
                String categoria;
                
                if (operazione instanceof Transazione) {
                    Transazione transazione = (Transazione) operazione;
                    tipo = transazione.getCategoria().getTipo().toString();
                    categoria = transazione.getCategoria().getNome();
                    
                    // Trova il nome del conto per questa transazione
                    for (Conto conto : sessionController.getConti()) {  // <-- Usa sessionController
                        if (conto.getTransazioni().contains(transazione)) {
                            categoria += " - " + conto.getNome();
                            break;
                        }
                    }
                } else if (operazione instanceof Trasferimento) {
                    Trasferimento trasferimento = (Trasferimento) operazione;
                    tipo = "Trasferimento";
                    
                    // Trova i nomi dei conti mittente e destinatario
                    String nomeContoMittente = "Sconosciuto";
                    String nomeContoDestinatario = "Sconosciuto";

                    if (trasferimento.getIdContoDestinatario() == 0) {
                        nomeContoDestinatario = trasferimento.getNomeContoEliminato() + " (Eliminato)";
                    }
                    if (trasferimento.getIdContoMittente() == 0) {
                        nomeContoMittente = trasferimento.getNomeContoEliminato() + " (Eliminato)";
                    }
                    
                    for (Conto conto : sessionController.getTuttiConti()) {  // <-- Usa sessionController
                        if (conto.getID() == trasferimento.getIdContoMittente()) {
                            nomeContoMittente = conto.getNome();
                        }
                        if (conto.getID() == trasferimento.getIdContoDestinatario()) {
                            nomeContoDestinatario = conto.getNome();
                        }
                    }
                    
                    categoria = nomeContoDestinatario + " ← " + nomeContoMittente;
                } else {
                    tipo = "Sconosciuto";
                    categoria = "Sconosciuto";
                }
                
                // Formatta la data
                String dataFormattata = operazione.getData().toString();
                
                jsonBuilder.append(String.format(java.util.Locale.US, 
                    "{\"id\": %d, \"data\": \"%s\", \"categoria\": \"%s\", \"tipo\": \"%s\", \"importo\": %.2f, \"descrizione\": \"%s\"}",
                    operazione.getID(), dataFormattata, categoria, tipo, 
                    operazione.getImporto(), operazione.getDescrizione()));
                
                if (i < operazioni.length - 1) {
                    jsonBuilder.append(",");
                }
            }
            jsonBuilder.append("]");
            
            String json = jsonBuilder.toString();
            
            Response response = createResponse(Response.Status.OK, "application/json", json);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero delle operazioni: " + e.getMessage() + "\"}");
        }
    }

    private Response handleLogout(IHTTPSession session) {
        String cookieHeader = session.getHeaders().get("cookie");
        
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split("; ")) {
                if (cookie.startsWith("session_token=")) {
                    String sessionToken = cookie.substring("session_token=".length());
                    SessionManager.removeSession(sessionToken);
                    break;
                }
            }
        }
        
        Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
        response.addHeader("Location", "/login");
        response.addHeader("Set-Cookie", "session_token=; Path=/; Max-Age=0");  // <-- Rimuovi il cookie
        return addNoCacheHeaders(response);
    }
}