package web_server;

import fi.iki.elonen.NanoHTTPD.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.sql.Timestamp;

import controller.Controller;
import dto.*;

public class GestoreConto extends BaseGestorePagina {
    
    public GestoreConto(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return "/conti".equals(uri) || 
               uri.startsWith("/api/conto/") ||
               uri.startsWith("/api/categorie");
    }

    @Override
    public Response handle(IHTTPSession session) throws Exception {
        String uri = session.getUri();
        Method method = session.getMethod();
        
        if ("/conti".equals(uri) && method == Method.GET) {
            Map<String, java.util.List<String>> params = session.getParameters();
            String contoId = null;
            
            if (params.containsKey("id") && !params.get("id").isEmpty()) {
                contoId = params.get("id").get(0);
            }
            
            if (contoId != null && !contoId.isEmpty()) {
                return serveContoSpecificPage(session);
            } else {
                Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
                response.addHeader("Location", "/home");
                return addNoCacheHeaders(response);
            }
        } else if (uri.matches("/api/conto/\\d+/conti-disponibili") && method == Method.GET) {
            return handleContiDisponibili(uri, session);  // <-- Aggiungi session
        } else if (uri.matches("/api/conto/\\d+/trasferimento") && method == Method.POST) {
            return handleAddTrasferimento(uri, session);
        } else if (uri.matches("/api/conto/\\d+/transazione") && method == Method.POST) {
            return handleAddTransazione(uri, session);
        } else if (uri.startsWith("/api/conto/") && method == Method.GET) {
            return handleContoAPI(uri, session);  // <-- Aggiungi session
        } else if (uri.startsWith("/api/conto/") && method == Method.PUT) {
            return handleUpdateConto(uri, session);
        } else if (uri.startsWith("/api/conto/") && method == Method.DELETE) {
            return handleDeleteConto(uri, session);
        } else if (uri.startsWith("/api/categorie/") && method == Method.GET) {
            return handleCategorieAPI(uri, session);  // <-- Aggiungi session
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }

    private Response handleDeleteConto(String uri, IHTTPSession session) throws Exception {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoIdStr = parts[3];
                int contoId = Integer.parseInt(contoIdStr);
                
                sessionController.eliminaConto(contoId);  // <-- Usa sessionController
                
                return createResponse(Response.Status.OK, "application/json", 
                    "{\"success\": true, \"message\": \"Conto eliminato con successo\"}");
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Endpoint non valido\"}");
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"ID conto non valido\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }

    private Response handleUpdateConto(String uri, IHTTPSession session) throws Exception {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoIdStr = parts[3];
                int contoId = Integer.parseInt(contoIdStr);
                
                if (sessionController.getContoById(contoId) == null) {  // <-- Usa sessionController
                    return createResponse(Response.Status.NOT_FOUND, "application/json", 
                        "{\"error\": \"Conto non trovato\"}");
                }
                
                String contentLengthHeader = session.getHeaders().get("content-length");
                if (contentLengthHeader == null) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Body della richiesta vuoto\"}");
                }
                
                int contentLength = Integer.parseInt(contentLengthHeader);
                byte[] buffer = new byte[contentLength];
                int bytesRead = session.getInputStream().read(buffer, 0, contentLength);
                String body = new String(buffer, 0, bytesRead, "UTF-8");
                
                String nome = extractJsonValue(body, "nome");
                String tipo = extractJsonValue(body, "tipo");
                String saldoInizialeStr = extractJsonValueNumber(body, "saldo_iniziale");
                String visibilitàStr = extractJsonValue(body, "visibilita");

                if (nome == null || tipo == null || saldoInizialeStr == null || visibilitàStr == null) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Nome, tipo e saldo iniziale sono obbligatori\"}");
                }
                
                double saldoIniziale = Double.parseDouble(saldoInizialeStr);
                boolean visibilità = Boolean.parseBoolean(visibilitàStr);
                sessionController.modificaConto(contoId, nome, tipo, saldoIniziale, visibilità);  // <-- Usa sessionController
                
                return createResponse(Response.Status.OK, "application/json", 
                    "{\"success\": true, \"message\": \"Conto modificato con successo\"}");
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Endpoint non valido\"}");
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"ID conto non valido\"}");
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int valueStart = json.indexOf(":", keyIndex) + 1;
        if (valueStart == 0) return null;
        
        while (valueStart < json.length() && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '\t')) {
            valueStart++;
        }
        
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') return null;
        valueStart++;
        
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd == -1) return null;
        
        return json.substring(valueStart, valueEnd);
    }
    
    private Response serveContoSpecificPage(IHTTPSession session) {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            String htmlPath = "conto.html";  // <-- Fix: usa leggiFile
            String content = leggiFile(htmlPath);
            
            if (content == null) {
                return createResponse(Response.Status.INTERNAL_ERROR, "text/html", 
                    "<h1>Errore durante il caricamento della pagina</h1>");
            }
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }
    
    private Response handleContoAPI(String uri, IHTTPSession session) {  // <-- Aggiungi parametro
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoId = parts[3];
                
                if (parts.length == 4) {
                    return getContoData(contoId, sessionController);  // <-- Passa sessionController
                } else if (parts.length == 5 && "operazioni".equals(parts[4])) {
                    return getContoOperazioni(contoId, sessionController);  // <-- Passa sessionController
                }
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Endpoint non valido\"}");
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }
    
    private Response getContoData(String contoIdStr, Controller sessionController) {  // <-- Aggiungi parametro
        try {
            int contoId = Integer.parseInt(contoIdStr);
            
            Conto conto = sessionController.getContoById(contoId);  // <-- Usa sessionController
            
            if (conto == null) {
                return createResponse(Response.Status.NOT_FOUND, "application/json", 
                    "{\"error\": \"Conto non trovato\"}");
            }
            
            String json = String.format(java.util.Locale.US,
                "{\"id\": %d, \"nome\": \"%s\", \"tipo\": \"%s\", \"saldo\": %.2f, \"saldo_iniziale\": %.2f, \"visibilita\": %s}",
                conto.getID(), conto.getNome(), conto.getTipo(), conto.getSaldo_attuale(), 
                conto.getSaldo_iniziale(), conto.getVisibilità() ? "true" : "false");
            
            Response response = createResponse(Response.Status.OK, "application/json", json);
            return addNoCacheHeaders(response);
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"ID conto non valido\"}");
        }
    }
    
    private Response getContoOperazioni(String contoIdStr, Controller sessionController) {  // <-- Aggiungi parametro
        try {
            int contoId = Integer.parseInt(contoIdStr);
            
            Conto conto = sessionController.getContoById(contoId);  // <-- Usa sessionController
            if (conto == null) {
                return createResponse(Response.Status.NOT_FOUND, "application/json", 
                    "{\"error\": \"Conto non trovato\"}");
            }
            
            Operazione[] operazioni = sessionController.getOperazioniConto(contoId);  // <-- Usa sessionController
            
            StringBuilder jsonBuilder = new StringBuilder("[");
            
            for (int i = 0; i < operazioni.length; i++) {
                Operazione operazione = operazioni[i];
                
                String tipo;
                String categoria;
                boolean isIncoming = false;
                
                if (operazione instanceof Transazione) {
                    Transazione transazione = (Transazione) operazione;
                    tipo = transazione.getCategoria().getTipo().toString();
                    categoria = transazione.getCategoria().getNome();
                } else if (operazione instanceof Trasferimento) {
                    Trasferimento trasferimento = (Trasferimento) operazione;
                    tipo = "Trasferimento";
                    isIncoming = trasferimento.getIdContoDestinatario() == contoId;
                    categoria = isIncoming ? "Trasferimento ricevuto" : "Trasferimento inviato";
                } else {
                    tipo = "Sconosciuto";
                    categoria = "Sconosciuto";
                }
                
                String dataFormattata = operazione.getData().toString();
                
                jsonBuilder.append(String.format(java.util.Locale.US,
                    "{\"id\": %d, \"data\": \"%s\", \"categoria\": \"%s\", \"tipo\": \"%s\", \"importo\": %.2f, \"descrizione\": \"%s\", \"isIncoming\": %s}",
                    operazione.getID(), dataFormattata, categoria, tipo, 
                    operazione.getImporto(), operazione.getDescrizione(), 
                    tipo.equals("Trasferimento") ? Boolean.toString(isIncoming) : "false"));
                
                if (i < operazioni.length - 1) {
                    jsonBuilder.append(",");
                }
            }
            
            jsonBuilder.append("]");
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonBuilder.toString());
            return addNoCacheHeaders(response);
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"ID conto non valido\"}");
        }
    }

    private Response handleCategorieAPI(String uri, IHTTPSession session) {  // <-- Aggiungi parametro
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            if (uri.equals("/api/categorie/spesa")) {
                return getCategorieSpesa(sessionController);  // <-- Passa sessionController
            } else if (uri.equals("/api/categorie/guadagno")) {
                return getCategorieGuadagno(sessionController);  // <-- Passa sessionController
            }
            
            return createResponse(Response.Status.NOT_FOUND, "application/json", 
                "{\"error\": \"Endpoint non valido\"}");
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }

    private Response getCategorieSpesa(Controller sessionController) {  // <-- Aggiungi parametro
        try {
            LinkedList<Categoria> categorie = sessionController.getCategoriaSpesa();  // <-- Usa sessionController
            
            StringBuilder jsonBuilder = new StringBuilder("[");
            for (int i = 0; i < categorie.size(); i++) {
                Categoria categoria = categorie.get(i);
                jsonBuilder.append(String.format(
                    "{\"id\": %d, \"nome\": \"%s\", \"tipo\": \"%s\"}", 
                    categoria.getID(), categoria.getNome(), categoria.getTipo()));
                
                if (i < categorie.size() - 1) {
                    jsonBuilder.append(",");
                }
            }
            jsonBuilder.append("]");
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonBuilder.toString());
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero delle categorie spesa\"}");
        }
    }

    private Response getCategorieGuadagno(Controller sessionController) {  // <-- Aggiungi parametro
        try {
            LinkedList<Categoria> categorie = sessionController.getCategoriaGuadagno();  // <-- Usa sessionController
            
            StringBuilder jsonBuilder = new StringBuilder("[");
            for (int i = 0; i < categorie.size(); i++) {
                Categoria categoria = categorie.get(i);
                jsonBuilder.append(String.format(
                    "{\"id\": %d, \"nome\": \"%s\", \"tipo\": \"%s\"}", 
                    categoria.getID(), categoria.getNome(), categoria.getTipo()));
                
                if (i < categorie.size() - 1) {
                    jsonBuilder.append(",");
                }
            }
            jsonBuilder.append("]");
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonBuilder.toString());
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero delle categorie guadagno\"}");
        }
    }

    private Response handleAddTransazione(String uri, IHTTPSession session) {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoIdStr = parts[3];
                int contoId = Integer.parseInt(contoIdStr);
                
                String contentLengthHeader = session.getHeaders().get("content-length");
                if (contentLengthHeader == null) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Body della richiesta vuoto\"}");
                }
                
                int contentLength = Integer.parseInt(contentLengthHeader);
                byte[] buffer = new byte[contentLength];
                int bytesRead = session.getInputStream().read(buffer, 0, contentLength);
                String requestBody = new String(buffer, 0, bytesRead, "UTF-8");
                
                String importoStr = extractJsonValueNumber(requestBody, "importo");
                String descrizione = extractJsonValue(requestBody, "descrizione");
                String categoriaIdStr = extractJsonValueNumber(requestBody, "categoriaId");
                String dataStr = extractJsonValue(requestBody, "data");
                                
                if (importoStr == null || descrizione == null || categoriaIdStr == null) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Dati mancanti\"}");
                }
                
                double importo = Double.parseDouble(importoStr);
                int categoriaId = Integer.parseInt(categoriaIdStr);

                Timestamp dataTransazione;
                try {
                    String dataStrNorm = dataStr;
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
                
                Categoria categoria = sessionController.getCategoriaById(categoriaId);  // <-- Usa sessionController
                if (categoria == null) {
                    return createResponse(Response.Status.NOT_FOUND, "application/json", 
                        "{\"error\": \"Categoria non trovata con ID: " + categoriaId + "\"}");
                }
                
                sessionController.aggiungiTransazione(importo, descrizione, categoria, contoId, dataTransazione);  // <-- Usa sessionController
                
                return createResponse(Response.Status.OK, "application/json", 
                    "{\"success\": true, \"message\": \"Transazione aggiunta con successo\"}");
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Endpoint non valido\"}");
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Dati numerici non validi: " + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }

    private String extractJsonValueNumber(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int valueStart = json.indexOf(":", keyIndex) + 1;
        if (valueStart == 0) return null;
        
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        
        if (valueStart >= json.length()) return null;
        
        int valueEnd = valueStart;
        while (valueEnd < json.length() && 
            (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '.')) {
            valueEnd++;
        }
        
        while (valueEnd < json.length() && 
            json.charAt(valueEnd) != ',' && 
            json.charAt(valueEnd) != '}') {
            valueEnd++;
        }
        
        if (valueEnd > valueStart) {
            String value = json.substring(valueStart, valueEnd).trim();
            if (value.endsWith(",")) {
                value = value.substring(0, value.length() - 1);
            }
            return value;
        }
        
        return null;
    }

    private Response handleContiDisponibili(String uri, IHTTPSession session) {  // <-- Aggiungi parametro
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoIdStr = parts[3];
                int contoId = Integer.parseInt(contoIdStr);
                
                LinkedList<Conto> contiDisponibili = sessionController.getSomeConti(contoId);  // <-- Usa sessionController
                
                StringBuilder jsonBuilder = new StringBuilder("[");
                for (int i = 0; i < contiDisponibili.size(); i++) {
                    Conto conto = contiDisponibili.get(i);
                    jsonBuilder.append(String.format(java.util.Locale.US,
                        "{\"id\": %d, \"nome\": \"%s\", \"tipo\": \"%s\", \"saldo\": %.2f, \"visibilita\": \"%s\"}",
                        conto.getID(), conto.getNome(), conto.getTipo(), conto.getSaldo_attuale(), 
                        conto.getVisibilità() ? "true" : "false"));
                    
                    if (i < contiDisponibili.size() - 1) {
                        jsonBuilder.append(",");
                    }
                }
                jsonBuilder.append("]");
                
                Response response = createResponse(Response.Status.OK, "application/json", jsonBuilder.toString());
                return addNoCacheHeaders(response);
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Endpoint non valido\"}");
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"ID conto non valido\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }

    private Response handleAddTrasferimento(String uri, IHTTPSession session) {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoIdStr = parts[3];
                int contoMittente = Integer.parseInt(contoIdStr);
                
                String contentLengthHeader = session.getHeaders().get("content-length");
                if (contentLengthHeader == null) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Body della richiesta vuoto\"}");
                }
                
                int contentLength = Integer.parseInt(contentLengthHeader);
                byte[] buffer = new byte[contentLength];
                int bytesRead = session.getInputStream().read(buffer, 0, contentLength);
                String requestBody = new String(buffer, 0, bytesRead, "UTF-8");
                
                String importoStr = extractJsonValueNumber(requestBody, "importo");
                String descrizione = extractJsonValue(requestBody, "descrizione");
                String dataStr = extractJsonValue(requestBody, "data");
                String contoDestinatarioStr = extractJsonValueNumber(requestBody, "contoDestinatario");
                
                if (importoStr == null || descrizione == null ||  dataStr == null || contoDestinatarioStr == null) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Dati mancanti\"}");
                }
                
                double importo = Double.parseDouble(importoStr);
                int contoDestinatario = Integer.parseInt(contoDestinatarioStr);

                Timestamp dataTrasferimento;
                try {
                    String dataStrNorm = dataStr;
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
                
                Conto contoMit = sessionController.getContoById(contoMittente);  // <-- Usa sessionController
                Conto contoDest = sessionController.getContoById(contoDestinatario);  // <-- Usa sessionController
                
                if (contoMit == null || contoDest == null) {
                    return createResponse(Response.Status.NOT_FOUND, "application/json", 
                        "{\"error\": \"Uno dei conti non è stato trovato\"}");
                }
                
                if (contoMittente == contoDestinatario) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Non puoi trasferire denaro allo stesso conto\"}");
                }
                
                if (importo > contoMit.getSaldo_attuale()) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Fondi insufficienti\"}");
                }
                
                sessionController.aggiungiTrasferimento(importo, descrizione, contoMittente, contoDestinatario, dataTrasferimento);  // <-- Usa sessionController

                return createResponse(Response.Status.OK, "application/json", 
                    "{\"success\": true, \"message\": \"Trasferimento effettuato con successo\"}");
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Endpoint non valido\"}");
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Dati numerici non validi\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }
}