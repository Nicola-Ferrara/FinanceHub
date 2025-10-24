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

public class GestoreTrasferimento extends BaseGestorePagina {
    
    public GestoreTrasferimento(Controller controller) {
        super(controller);
    }

    @Override
    public boolean canHandle(String uri, String method) {
        return "/trasferimento".equals(uri) 
            || "/api/trasferimento".equals(uri);
    }

    @Override
    public Response handle(IHTTPSession session) throws Exception {
        String uri = session.getUri();
        Method method = session.getMethod();
        
        if ("/trasferimento".equals(uri) && method == Method.GET) {
            return serveTransferPage(session);
        } else if ("/api/trasferimento".equals(uri)) {
            switch (method) {
                case GET:
                    return handleGetTransfer(session);
                case PUT:
                    return handleUpdateTransfern(session);
                case DELETE:
                    return handleDeleteTransfer(session);
                default:
                    return createResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", 
                        "{\"error\": \"Metodo non supportato\"}");
            }
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }

    private Response serveTransferPage(IHTTPSession session) {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            String htmlPath = "trasferimento.html";  // <-- Fix 1: usa leggiFile
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

    private Response handleGetTransfer(IHTTPSession session) {
        try {
            Controller sessionController = getSessionController(session);
            if (sessionController == null || !sessionController.isUtenteLogged()) {
                return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\": \"Utente non autorizzato\"}");
            }
            
            Map<String, List<String>> params = session.getParameters();
            String idStr = getFirstParameter(params, "id");
            
            if (idStr == null || idStr.isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID trasferimento mancante\"}");
            }
            
            int transferId;
            try {
                transferId = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID trasferimento non valido\"}");
            }
            
            // Cerca il trasferimento in tutti i conti
            Trasferimento trasferimento = null;
            String nomeContoMittente = "Conto sconosciuto";
            String nomeContoDestinatario = "Conto sconosciuto";
            int idContoMittente = 0;
            int idContoDestinatario = 0;
            
            for (Conto conto : sessionController.getTuttiConti()) {  // <-- Fix 2: usa sessionController
                if (conto.getTrasferimenti() != null) {
                    for (Trasferimento t : conto.getTrasferimenti()) {
                        if (t.getID() == transferId) {
                            trasferimento = t;
                            if (conto.getID() == trasferimento.getIdContoMittente()) {
                                nomeContoMittente = conto.getNome();
                                idContoMittente = conto.getID();
                            }
                            if (conto.getID() == trasferimento.getIdContoDestinatario()) {
                                nomeContoDestinatario = conto.getNome();
                                idContoDestinatario = conto.getID();
                            }
                        }
                    }
                }
            }

            if (trasferimento == null) {
                return createResponse(Response.Status.NOT_FOUND, "application/json",
                    "{\"error\": \"Trasferimento non trovato\"}");
            }
            if (trasferimento.getIdContoDestinatario() == 0) {
                nomeContoDestinatario = trasferimento.getNomeContoEliminato() + " (Eliminato)";
                idContoDestinatario = 0;
            }
            if (trasferimento.getIdContoMittente() == 0) {
                nomeContoMittente = trasferimento.getNomeContoEliminato() + " (Eliminato)";
                idContoMittente = 0;
            }
            
            // Costruisci JSON response con null safety
            String descrizione = trasferimento.getDescrizione() != null ? trasferimento.getDescrizione() : "";
            String jsonResponse = String.format(java.util.Locale.US,
                "{\"id\": %d, \"importo\": %.2f, \"data\": \"%s\", " +
                "\"descrizione\": \"%s\", " +
                "\"contoMittente\": \"%s\", \"idContoMittente\": %d, " +
                "\"contoDestinatario\": \"%s\", \"idContoDestinatario\": %d}",
                trasferimento.getID(),
                trasferimento.getImporto(),
                trasferimento.getData().toString(),
                escapeJson(descrizione),
                escapeJson(nomeContoMittente),
                idContoMittente,
                escapeJson(nomeContoDestinatario),
                idContoDestinatario
            );
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonResponse);
            return addNoCacheHeaders(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero del trasferimento: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Response handleUpdateTransfern(IHTTPSession session) {
        try {
            Controller sessionController = getSessionController(session);
            if (sessionController == null || !sessionController.isUtenteLogged()) {
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
            
            try {
                id = extractIntFromJson(body, "id");
                importo = extractDoubleFromJson(body, "importo");
                data = extractStringFromJson(body, "data");
                descrizione = extractStringFromJson(body, "descrizione");
            } catch (Exception e) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Errore nel parsing dei dati JSON\"}");
            }
            
            // VALIDAZIONI COMPLETE LATO SERVER
            
            // Validazione ID
            if (id <= 0) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID trasferimento non valido\"}");
            }
            
            // Validazione importo
            if (importo <= 0) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"L'importo deve essere maggiore di zero\"}");
            }
            
            // Controllo limite massimo importo (PostgreSQL NUMERIC(15,2))
            if (importo > 9999999999999.99) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"L'importo non può superare i 9.999.999.999.999,99 €\"}");
            }
            
            // Validazione data
            if (data == null || data.isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Data mancante\"}");
            }
            
            // Validazione descrizione obbligatoria
            if (descrizione == null || descrizione.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"La descrizione è obbligatoria\"}");
            }
            
            // PULISCI la descrizione
            descrizione = descrizione.trim();
            
            // Validazione lunghezza minima descrizione
            if (descrizione.length() < 3) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"La descrizione deve essere di almeno 3 caratteri\"}");
            }
            
            // Validazione lunghezza massima descrizione
            if (descrizione.length() > 500) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"La descrizione non può superare i 500 caratteri\"}");
            }
            
            // Validazione formato data
            Timestamp dataTrasferimento;
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
                dataTrasferimento = Timestamp.from(instant);
            } catch (Exception e) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Formato data non valido. Usa: YYYY-MM-DDTHH:MM\"}");
            }
            
            // Modifica il trasferimento
            sessionController.modificaTrasferimento(id, importo, descrizione, dataTrasferimento);  // <-- Fix 3: usa sessionController
            
            Response response = createResponse(Response.Status.OK, "application/json", 
                "{\"success\": true, \"message\": \"Trasferimento modificato con successo\"}");
            return addNoCacheHeaders(response);
            
        } catch (EccezioniDatabase e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore database durante la modifica: " + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante la modifica del trasferimento: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Response handleDeleteTransfer(IHTTPSession session) {
        try {
            Controller sessionController = getSessionController(session);
            if (sessionController == null || !sessionController.isUtenteLogged()) {
                return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\": \"Utente non autorizzato\"}");
            }
            
            Map<String, List<String>> params = session.getParameters();
            String idStr = getFirstParameter(params, "id");
            
            if (idStr == null || idStr.isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID trasferimento mancante\"}");
            }
            
            int transferId;
            try {
                transferId = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID non valido\"}");
            }
            
            if (transferId <= 0) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID deve essere maggiore di zero\"}");
            }
            
            // Elimina il trasferimento
            sessionController.eliminaTrasferimento(transferId);  // <-- Fix 4: usa sessionController
            
            Response response = createResponse(Response.Status.OK, "application/json", 
                "{\"success\": true, \"message\": \"Trasferimento eliminato con successo\"}");
            return addNoCacheHeaders(response);
            
        } catch (EccezioniDatabase e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore database durante l'eliminazione: " + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante l'eliminazione del trasferimento: " + escapeJson(e.getMessage()) + "\"}");
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
    
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
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
}