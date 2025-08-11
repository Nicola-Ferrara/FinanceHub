package web_server;

import fi.iki.elonen.NanoHTTPD.*;
import controller.Controller;
import dto.*;
import exception.EccezioniDatabase;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GestoreTransazione extends BaseGestorePagina {
    
    public GestoreTransazione(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return "/transazione".equals(uri) 
            || "/api/transazione".equals(uri)
            || "/api/transazione-categorie".equals(uri);
    }
    
    @Override
    public Response handle(IHTTPSession session) throws Exception {
        String uri = session.getUri();
        Method method = session.getMethod();
        
        if ("/transazione".equals(uri) && method == Method.GET) {
            return serveTransactionPage();
        } else if ("/api/transazione".equals(uri)) {
            switch (method) {
                case GET:
                    return handleGetTransaction(session);
                case PUT:
                    return handleUpdateTransaction(session);
                case DELETE:
                    return handleDeleteTransaction(session);
                default:
                    return createResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", 
                        "{\"error\": \"Metodo non supportato\"}");
            }
        } else if ("/api/transazione-categorie".equals(uri) && method == Method.GET) {
            return handleGetCategories();
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }
    
    private Response serveTransactionPage() {
        if (!controller.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            String filePath = "./sito/html/transazione.html";
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (IOException e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }
    
    private Response handleGetTransaction(IHTTPSession session) {
        try {
            if (!controller.isUtenteLogged()) {
                return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\": \"Utente non autorizzato\"}");
            }
            
            Map<String, List<String>> params = session.getParameters();
            String idStr = getFirstParameter(params, "id");
            
            if (idStr == null || idStr.isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID transazione mancante\"}");
            }
            
            int transactionId;
            try {
                transactionId = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID transazione non valido\"}");
            }
            
            // Cerca la transazione in tutti i conti
            Transazione transazione = null;
            String nomeConto = "Conto sconosciuto";
            int idConto = 0;
            
            for (Conto conto : controller.getTuttiConti()) {
                if (conto.getTransazioni() != null) {
                    for (Transazione t : conto.getTransazioni()) {
                        if (t.getID() == transactionId) {
                            transazione = t;
                            nomeConto = conto.getNome();
                            idConto = conto.getID();
                            break;
                        }
                    }
                }
                if (transazione != null) break;
            }
            
            if (transazione == null) {
                return createResponse(Response.Status.NOT_FOUND, "application/json", 
                    "{\"error\": \"Transazione non trovata\"}");
            }
            
            // Costruisci JSON response con null safety
            String descrizione = transazione.getDescrizione() != null ? transazione.getDescrizione() : "";
            String categoria = transazione.getCategoria() != null ? transazione.getCategoria().getNome() : "Senza categoria";
            String tipoCategoria = transazione.getCategoria() != null ? transazione.getCategoria().getTipo().toString() : "Sconosciuto";
            
            String jsonResponse = String.format(java.util.Locale.US,
                "{\"id\": %d, \"importo\": %.2f, \"data\": \"%s\", " +
                "\"descrizione\": \"%s\", \"categoria\": \"%s\", " +
                "\"tipoCategoria\": \"%s\", \"idCategoria\": %d, " +
                "\"conto\": \"%s\", \"idConto\": %d}",
                transazione.getID(),
                transazione.getImporto(),
                transazione.getData().toInstant().toString(),
                escapeJson(descrizione),
                escapeJson(categoria),
                tipoCategoria,
                transazione.getIdCategoria(),
                escapeJson(nomeConto),
                idConto
            );
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonResponse);
            return addNoCacheHeaders(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero della transazione: " + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private Response handleUpdateTransaction(IHTTPSession session) {
        try {
            if (!controller.isUtenteLogged()) {
                return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\": \"Utente non autorizzato\"}");
            }
            
            // Leggi il body della richiesta
            String body = null;
            
            try {
                String contentLengthHeader = session.getHeaders().get("content-length");
                int contentLength = 0;
                
                if (contentLengthHeader != null) {
                    contentLength = Integer.parseInt(contentLengthHeader);
                }
                
                if (contentLength > 0) {
                    java.io.InputStream inputStream = session.getInputStream();
                    byte[] buffer = new byte[contentLength];
                    
                    int totalBytesRead = 0;
                    while (totalBytesRead < contentLength) {
                        int bytesRead = inputStream.read(buffer, totalBytesRead, contentLength - totalBytesRead);
                        if (bytesRead == -1) break;
                        totalBytesRead += bytesRead;
                    }
                    
                    body = new String(buffer, 0, totalBytesRead, "UTF-8");
                } else {
                    Map<String, String> files = new java.util.HashMap<>();
                    session.parseBody(files);
                    body = files.get("postData");
                }
                
            } catch (Exception e) {
                // Ignora errori di lettura e continua
            }
            
            if (body == null || body.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Dati mancanti\"}");
            }
            
            // Parse JSON
            int id = 0;
            double importo = 0.0;
            String data = "";
            String descrizione = "";
            int idCategoria = 0;
            int idConto = 0;
            
            try {
                id = extractIntFromJson(body, "id");
                importo = extractDoubleFromJson(body, "importo");
                data = extractStringFromJson(body, "data");
                descrizione = extractStringFromJson(body, "descrizione");
                idCategoria = extractIntFromJson(body, "idCategoria");
                idConto = extractIntFromJson(body, "idConto");
                
            } catch (Exception e) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Errore nel parsing dei dati JSON\"}");
            }
            
            // ✅ VALIDAZIONI COMPLETE LATO SERVER
            
            // Validazione ID
            if (id <= 0) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID transazione non valido\"}");
            }
            
            // Validazione importo
            if (importo <= 0) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"L'importo deve essere maggiore di zero\"}");
            }
            
            // ✅ NUOVO - Controllo limite massimo importo (PostgreSQL NUMERIC(15,2))
            if (importo > 9999999999999.99) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"L'importo non può superare i 9.999.999.999.999,99 €\"}");
            }
            
            // Validazione data
            if (data == null || data.isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Data mancante\"}");
            }
            
            // ✅ NUOVO - Validazione descrizione obbligatoria
            if (descrizione == null || descrizione.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"La descrizione è obbligatoria\"}");
            }
            
            // ✅ PULISCI la descrizione
            descrizione = descrizione.trim();
            
            // ✅ NUOVO - Validazione lunghezza minima descrizione
            if (descrizione.length() < 3) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"La descrizione deve essere di almeno 3 caratteri\"}");
            }
            
            // ✅ NUOVO - Validazione lunghezza massima descrizione
            if (descrizione.length() > 500) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"La descrizione non può superare i 500 caratteri\"}");
            }
            
            // ✅ NUOVO - Validazione categoria obbligatoria
            if (idCategoria <= 0) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Seleziona una categoria valida\"}");
            }
            
            // Validazione formato data
            Timestamp dataTransazione;
            try {
                String dataStrNorm = data;
                if (dataStrNorm.length() == 16) {
                    dataStrNorm += ":00";
                }
                OffsetDateTime odt;
                if (dataStrNorm.endsWith("Z")) {
                    odt = OffsetDateTime.parse(dataStrNorm);
                } else {
                    odt = LocalDateTime.parse(dataStrNorm).atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();
                }
                Instant instant = odt.toInstant();
                dataTransazione = Timestamp.from(instant);
            } catch (Exception e) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Formato data non valido. Usa: YYYY-MM-DDTHH:MM\"}");
            }
            
            // Trova la categoria
            Categoria categoria = controller.getCategoriaById(idCategoria);
            if (categoria == null) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Categoria non trovata\"}");
            }
            
            // Modifica la transazione
            controller.modificaTransazione(id, importo, descrizione, categoria, idConto, dataTransazione);
            
            Response response = createResponse(Response.Status.OK, "application/json", 
                "{\"success\": true, \"message\": \"Transazione modificata con successo\"}");
            return addNoCacheHeaders(response);
            
        } catch (EccezioniDatabase e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore database durante la modifica: " + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante la modifica della transazione: " + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private Response handleDeleteTransaction(IHTTPSession session) {
        try {
            if (!controller.isUtenteLogged()) {
                return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\": \"Utente non autorizzato\"}");
            }
            
            Map<String, List<String>> params = session.getParameters();
            String idStr = getFirstParameter(params, "id");
            String idContoStr = getFirstParameter(params, "idConto");
            
            if (idStr == null || idStr.isEmpty() || idContoStr == null || idContoStr.isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID transazione o conto mancante\"}");
            }
            
            int transactionId;
            int accountId;
            try {
                transactionId = Integer.parseInt(idStr);
                accountId = Integer.parseInt(idContoStr);
            } catch (NumberFormatException e) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID non validi\"}");
            }
            
            if (transactionId <= 0 || accountId <= 0) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID devono essere maggiori di zero\"}");
            }
            
            // Elimina la transazione
            controller.eliminaTransazione(transactionId, accountId);
            
            Response response = createResponse(Response.Status.OK, "application/json", 
                "{\"success\": true, \"message\": \"Transazione eliminata con successo\"}");
            return addNoCacheHeaders(response);
            
        } catch (EccezioniDatabase e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore database durante l'eliminazione: " + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante l'eliminazione della transazione: " + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private Response handleGetCategories() {
        if (!controller.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Utente non autorizzato\"}");
        }
        
        try {
            List<Categoria> categorie = controller.getTutteCategorie();
            
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < categorie.size(); i++) {
                Categoria cat = categorie.get(i);
                if (i > 0) json.append(",");
                
                json.append(String.format(
                    "{\"id\": %d, \"nome\": \"%s\", \"tipo\": \"%s\"}",
                    cat.getID(),
                    escapeJson(cat.getNome()),
                    cat.getTipo().toString()
                ));
            }
            json.append("]");
            
            Response response = createResponse(Response.Status.OK, "application/json", json.toString());
            return addNoCacheHeaders(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero delle categorie\"}");
        }
    }
    
    // Utility methods
    private String getFirstParameter(Map<String, List<String>> params, String key) {
        List<String> values = params.get(key);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return null;
    }
    
    private int extractIntFromJson(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }
    
    private double extractDoubleFromJson(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*([\\d.]+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return 0.0;
    }
    
    private String extractStringFromJson(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*?)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }
    
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}