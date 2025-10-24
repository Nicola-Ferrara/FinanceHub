package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import controller.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

public class GestoreBilanci extends BaseGestorePagina {
    
    public GestoreBilanci(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return "/bilanci".equals(uri) || "/api/bilanci".equals(uri);
    }
    
    @Override
    public Response handle(IHTTPSession session) throws Exception {
        String uri = session.getUri();
        Method method = session.getMethod();
        
        if ("/bilanci".equals(uri) && method == Method.GET) {
            return serveBilanciPage(session);
        } else if ("/api/bilanci".equals(uri) && method == Method.GET) {
            return handleBilanciData(session);
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }
    
    private Response serveBilanciPage(IHTTPSession session) {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        String htmlPath = "bilanci.html";  // <-- Fix: usa leggiFile
        String html = leggiFile(htmlPath);
        
        if (html == null) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/html", 
                "<h1>Errore durante il caricamento della pagina</h1>");
        }
        
        Response response = createResponse(Response.Status.OK, "text/html", html);
        return addNoCacheHeaders(response);
    }
    
    private Response handleBilanciData(IHTTPSession session) {
        Controller sessionController = getSessionController(session);  // <-- Fix: recupera all'inizio
        
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Utente non autorizzato\"}");
        }
        
        try {
            // Ottieni data iscrizione utente
            LocalDate dataIscrizione = sessionController.getDataIscrizioneUtente();  // <-- Fix: usa sessionController
            LocalDate oggi = LocalDate.now();
            
            // Crea mappa per bilanci mensili
            Map<String, BilancioMensile> bilanciMensili = new TreeMap<>();
            
            // Genera tutti i mesi dall'iscrizione ad oggi
            LocalDate current = dataIscrizione.withDayOfMonth(1);
            while (!current.isAfter(oggi.withDayOfMonth(1))) {
                String chiave = current.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                bilanciMensili.put(chiave, new BilancioMensile(current.getYear(), current.getMonthValue()));
                current = current.plusMonths(1);
            }
            
            // Calcola entrate e uscite per ogni mese
            var operazioni = sessionController.getTutteOperazioni();  // <-- Fix: usa sessionController
            
            for (var operazione : operazioni) {
                LocalDate dataOperazione = operazione.getData().toLocalDateTime().toLocalDate();
                String chiave = dataOperazione.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                
                BilancioMensile bilancio = bilanciMensili.get(chiave);
                if (bilancio != null) {
                    if (operazione instanceof dto.Transazione) {
                        dto.Transazione transazione = (dto.Transazione) operazione;
                        if (transazione.getCategoria().getTipo() == dto.Categoria.TipoCategoria.Guadagno) {
                            bilancio.entrate += transazione.getImporto();
                        } else {
                            bilancio.uscite += transazione.getImporto();
                        }
                        bilancio.transazioni++;
                    }
                }
            }
            
            // Costruisci JSON response
            StringBuilder jsonBuilder = new StringBuilder("[");
            boolean first = true;
            
            for (BilancioMensile bilancio : bilanciMensili.values()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                first = false;
                
                jsonBuilder.append(String.format(java.util.Locale.US,
                    "{\"anno\": %d, \"mese\": %d, \"entrate\": %.2f, \"uscite\": %.2f, \"transazioni\": %d}",
                    bilancio.anno, bilancio.mese, bilancio.entrate, bilancio.uscite, bilancio.transazioni));
            }
            
            jsonBuilder.append("]");
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonBuilder.toString());
            return addNoCacheHeaders(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero dei bilanci: " + e.getMessage() + "\"}");
        }
    }
    
    private static class BilancioMensile {
        int anno;
        int mese;
        double entrate = 0.0;
        double uscite = 0.0;
        int transazioni = 0;
        
        BilancioMensile(int anno, int mese) {
            this.anno = anno;
            this.mese = mese;
        }
    }
}