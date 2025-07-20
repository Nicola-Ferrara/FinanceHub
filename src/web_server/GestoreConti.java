package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import java.util.Map;
import controller.Controller;
import dto.*;

public class GestoreConti extends BaseGestorePagina {
    
    public GestoreConti(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return uri.startsWith("/conti") || 
               uri.startsWith("/api/conto/");
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
                return serveContoSpecificPage();
            } else {
                Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
                response.addHeader("Location", "/home");
                return addNoCacheHeaders(response);
            }
        } else if (uri.startsWith("/api/conto/") && method == Method.GET) {
            return handleContoAPI(uri);
        } else if (uri.startsWith("/api/conto/") && method == Method.PUT) {
            return handleUpdateConto(uri, session);
        } else if (uri.startsWith("/api/conto/") && method == Method.DELETE) {
            return handleDeleteConto(uri, session);
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }

    private Response handleDeleteConto(String uri, IHTTPSession session) throws Exception {
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoIdStr = parts[3];
                int contoId = Integer.parseInt(contoIdStr);
                
                // Verifica che il conto esista
                Conto conto = controller.getContoById(contoId);
                if (conto == null) {
                    return createResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Conto non trovato\"}");
                }
                
                // Usa il metodo del controller per eliminare il conto
                controller.modificaConto(conto.getID(), conto.getNome(), false, conto.getTipo());
                
                return createResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"Conto eliminato con successo\"}");
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"Endpoint non valido\"}");
        } catch (NumberFormatException e) {
            System.out.println("DEBUG: Errore NumberFormat: " + e.getMessage());
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"ID conto non valido\"}");
        } catch (Exception e) {
            System.out.println("DEBUG: Errore durante eliminazione: " + e.getMessage());
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }

    private Response handleUpdateConto(String uri, IHTTPSession session) throws Exception {
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoIdStr = parts[3];
                int contoId = Integer.parseInt(contoIdStr);
                
                if (controller.getContoById(contoId) == null) {
                    return createResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Conto non trovato\"}");
                }
                
                String body = null;
                int contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
                
                if (contentLength > 0) {
                    byte[] buffer = new byte[contentLength];
                    session.getInputStream().read(buffer, 0, contentLength);
                    body = new String(buffer);
                }
                
                if (body == null || body.isEmpty()) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"Body della richiesta vuoto\"}");
                }
                
                String nome = extractJsonValue(body, "nome");
                String tipo = extractJsonValue(body, "tipo");
                
                if (nome == null || tipo == null) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"Nome e tipo sono obbligatori\"}");
                }
                
                controller.modificaConto(contoId, nome, true, tipo);
                
                return createResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"Conto modificato con successo\"}");
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"Endpoint non valido\"}");
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"ID conto non valido\"}");
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
    
    private Response serveContoSpecificPage() {
        if (!controller.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            String filePath = "./sito/html/conto.html";
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }
    
    private Response handleContoAPI(String uri) {
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoId = parts[3];
                
                if (parts.length == 4) {
                    return getContoData(contoId);
                } else if (parts.length == 5 && "operazioni".equals(parts[4])) {
                    return getContoOperazioni(contoId);
                }
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"Endpoint non valido\"}");
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }
    
    private Response getContoData(String contoIdStr) {
        try {
            int contoId = Integer.parseInt(contoIdStr);
            
            Conto conto = controller.getContoById(contoId);
            
            if (conto == null) {
                return createResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Conto non trovato\"}");
            }
            
            String json = String.format(java.util.Locale.US,
                "{\"id\": %d, \"nome\": \"%s\", \"tipo\": \"%s\", \"saldo\": %.2f}",
                conto.getID(), conto.getNome(), conto.getTipo(), conto.getSaldo());
            
            Response response = createResponse(Response.Status.OK, "application/json", json);
            return addNoCacheHeaders(response);
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"ID conto non valido\"}");
        }
    }
    
    private Response getContoOperazioni(String contoIdStr) {
        try {
            int contoId = Integer.parseInt(contoIdStr);
            
            Conto conto = controller.getContoById(contoId);
            if (conto == null) {
                return createResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Conto non trovato\"}");
            }
            
            Operazione[] operazioni = controller.getOperazioniConto(contoId);
            
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
                
                String dataFormattata = operazione.getData().toString().substring(0, 10);
                
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
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"ID conto non valido\"}");
        }
    }
}