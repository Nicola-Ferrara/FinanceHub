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
            return serveBilanciPage();
        } else if ("/api/bilanci".equals(uri) && method == Method.GET) {
            return handleBilanciData();
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }
    
    private Response serveBilanciPage() {
        if (!controller.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            String filePath = "./sito/html/bilanci.html";
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }
    
    private Response handleBilanciData() {
        try {
            if (!controller.isUtenteLogged()) {
                return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\": \"Utente non autorizzato\"}");
            }
            
            // Ottieni data iscrizione utente
            LocalDate dataIscrizione = controller.getDataIscrizioneUtente();
            LocalDate oggi = LocalDate.now();
            
            // Crea mappa per bilanci mensili
            Map<String, BilancioMensile> bilanciMensili = new TreeMap<>();
            
            // Genera tutti i mesi dall'iscrizione ad oggi
            LocalDate current = dataIscrizione.withDayOfMonth(1); // Primo giorno del mese di iscrizione
            while (!current.isAfter(oggi.withDayOfMonth(1))) {
                String chiave = current.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                bilanciMensili.put(chiave, new BilancioMensile(current.getYear(), current.getMonthValue()));
                current = current.plusMonths(1);
            }
            
            // Calcola entrate e uscite per ogni mese
            var operazioni = controller.getTutteOperazioni();
            
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
                    // Note: I trasferimenti non influenzano il bilancio generale
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
    
    // Classe helper per bilancio mensile
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