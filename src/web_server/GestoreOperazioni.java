package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import controller.Controller;
import dto.*;

public class GestoreOperazioni extends BaseGestorePagina {
    
    public GestoreOperazioni(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return "/operazioni".equals(uri) || "/api/operazioni".equals(uri);
    }
    
    @Override
    public Response handle(IHTTPSession session) throws Exception {
        String uri = session.getUri();
        Method method = session.getMethod();
        
        if ("/operazioni".equals(uri) && method == Method.GET) {
            return serveOperazioniPage(session);
        } else if ("/api/operazioni".equals(uri) && method == Method.GET) {
            return handleOperazioniData(session);
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }
    
    private Response serveOperazioniPage(IHTTPSession session) {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            String htmlPath = "operazioni.html";  // <-- Fix 1: usa leggiFile
            String content = leggiFile(htmlPath);
            
            if (content == null) {
                return createResponse(Response.Status.INTERNAL_ERROR, "text/html", 
                    "<h1>Errore durante il caricamento della pagina</h1>");
            }
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }
    
    private Response handleOperazioniData(IHTTPSession session) {
        Controller sessionController = getSessionController(session);  // <-- Fix 2: sposta all'inizio
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Utente non autorizzato\"}");
        }
        
        try {
            // Ottieni tutte le operazioni
            Operazione[] operazioni = sessionController.getTutteOperazioni();  // <-- Fix 3: usa sessionController
            
            // Costruisci JSON response
            StringBuilder jsonBuilder = new StringBuilder("[");
            
            for (int i = 0; i < operazioni.length; i++) {
                if (i > 0) {
                    jsonBuilder.append(",");
                }
                
                Operazione operazione = operazioni[i];
                
                if (operazione instanceof Transazione) {
                    Transazione transazione = (Transazione) operazione;
                    
                    // Trova il nome del conto
                    String nomeConto = "Conto sconosciuto";
                    try {
                        var conti = sessionController.getTuttiConti();  // <-- Fix 4: usa sessionController
                        for (var conto : conti) {
                            if (conto.getTransazioni() != null) {
                                for (var t : conto.getTransazioni()) {
                                    if (t.getID() == transazione.getID()) {
                                        nomeConto = conto.getNome();
                                        break;
                                    }
                                }
                            }
                            if (!nomeConto.equals("Conto sconosciuto")) break;
                        }
                    } catch (Exception e) {
                        // Ignora errori nella ricerca del conto
                    }
                    
                    jsonBuilder.append(String.format(java.util.Locale.US,
                        "{\"tipo\": \"transazione\", \"id\": %d, \"importo\": %.2f, " +
                        "\"data\": \"%s\", \"descrizione\": \"%s\", " +
                        "\"categoria\": \"%s\", \"tipoCategoria\": \"%s\", \"conto\": \"%s\"}",
                        transazione.getID(),
                        transazione.getImporto(),
                        transazione.getData().toString(),
                        escapeJson(transazione.getDescrizione()),
                        escapeJson(transazione.getCategoria().getNome()),
                        transazione.getCategoria().getTipo().toString(),
                        escapeJson(nomeConto)
                    ));
                    
                } else if (operazione instanceof Trasferimento) {
                    Trasferimento trasferimento = (Trasferimento) operazione;
                    
                    // Trova i nomi dei conti
                    String nomeContoMittente = "Conto eliminato";
                    String nomeContoDestinatario = "Conto eliminato";
                    
                    // Gestisci conti eliminati
                    if (trasferimento.getNomeContoEliminato() != null) {
                        if (trasferimento.getIdContoMittente() == 0) {
                            nomeContoMittente = trasferimento.getNomeContoEliminato();
                        } else if (trasferimento.getIdContoDestinatario() == 0) {
                            nomeContoDestinatario = trasferimento.getNomeContoEliminato();
                        }
                    }
                    
                    // Trova conti esistenti
                    try {
                        var conti = sessionController.getTuttiConti();  // <-- Fix 5: usa sessionController
                        for (var conto : conti) {
                            if (conto.getID() == trasferimento.getIdContoMittente()) {
                                nomeContoMittente = conto.getNome();
                            }
                            if (conto.getID() == trasferimento.getIdContoDestinatario()) {
                                nomeContoDestinatario = conto.getNome();
                            }
                        }
                    } catch (Exception e) {
                        // Ignora errori nella ricerca dei conti
                    }
                    
                    jsonBuilder.append(String.format(java.util.Locale.US,
                        "{\"tipo\": \"trasferimento\", \"id\": %d, \"importo\": %.2f, " +
                        "\"data\": \"%s\", \"descrizione\": \"%s\", " +
                        "\"contoMittente\": \"%s\", \"contoDestinatario\": \"%s\"}",
                        trasferimento.getID(),
                        trasferimento.getImporto(),
                        trasferimento.getData().toString(),
                        escapeJson(trasferimento.getDescrizione()),
                        escapeJson(nomeContoMittente),
                        escapeJson(nomeContoDestinatario)
                    ));
                }
            }
            
            jsonBuilder.append("]");
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonBuilder.toString());
            return addNoCacheHeaders(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero delle operazioni: " + e.getMessage() + "\"}");
        }
    }
    
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}