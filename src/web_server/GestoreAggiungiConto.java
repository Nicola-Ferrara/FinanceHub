package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import controller.Controller;

public class GestoreAggiungiConto extends BaseGestorePagina {
    
    public GestoreAggiungiConto(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return "/aggiungiConto".equals(uri) || 
               ("/api/conto".equals(uri) && "POST".equals(method));
    }
    
    @Override
    public Response handle(IHTTPSession session) throws Exception {
        String uri = session.getUri();
        Method method = session.getMethod();
        
        if ("/aggiungiConto".equals(uri) && method == Method.GET) {
            return serveAggiungiContoPage(session);
        } else if ("/api/conto".equals(uri) && method == Method.POST) {
            return handleAddConto(session);
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }
    
    private Response serveAggiungiContoPage(IHTTPSession session) {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        String htmlPath = "aggiungiConto.html";  // <-- Fix 1: usa leggiFile
        String html = leggiFile(htmlPath);
        
        if (html == null) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/html", 
                "<h1>Errore durante il caricamento della pagina</h1>");
        }
        
        Response response = createResponse(Response.Status.OK, "text/html", html);
        return addNoCacheHeaders(response);
    }
    
    private Response handleAddConto(IHTTPSession session) {
        Controller sessionController = getSessionController(session);  // <-- Fix 2: recupera sessionController
        
        if (sessionController == null || !sessionController.isUtenteLogged()) {  // <-- Fix 2: aggiungi check
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            // Leggi il body dalla richiesta POST
            String contentLengthHeader = session.getHeaders().get("content-length");
            if (contentLengthHeader == null) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Body della richiesta vuoto\"}");
            }
            
            int contentLength = Integer.parseInt(contentLengthHeader);
            byte[] buffer = new byte[contentLength];
            int bytesRead = session.getInputStream().read(buffer, 0, contentLength);
            String requestBody = new String(buffer, 0, bytesRead, "UTF-8");
            
            String nome = extractJsonValue(requestBody, "nome");
            String tipo = extractJsonValue(requestBody, "tipo");
            String saldoStr = extractJsonValue(requestBody, "saldo");
            String visibilitàStr = extractJsonValue(requestBody, "visibilita");
            
            if (nome == null || tipo == null || saldoStr == null || visibilitàStr == null) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Dati mancanti\"}");
            }
            
            double saldo = Double.parseDouble(saldoStr);
            boolean visibilità = Boolean.parseBoolean(visibilitàStr);
            
            sessionController.aggiungiConto(nome, tipo, saldo, visibilità);  // <-- Fix 3: usa sessionController
            
            return createResponse(Response.Status.OK, "application/json", 
                "{\"success\": true, \"message\": \"Conto aggiunto con successo\"}");
                
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Saldo non valido\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante l'aggiunta del conto: " + e.getMessage() + "\"}");
        }
    }

    private String extractJsonValue(String json, String key) {
        if (json == null) return null;
        
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int valueStart = json.indexOf(":", keyIndex) + 1;
        if (valueStart == 0) return null;
        
        while (valueStart < json.length() && 
            (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '\t')) {
            valueStart++;
        }
        
        if (valueStart >= json.length()) return null;
        
        if (json.charAt(valueStart) == '"') {
            valueStart++;
            int valueEnd = json.indexOf("\"", valueStart);
            if (valueEnd == -1) return null;
            return json.substring(valueStart, valueEnd);
        } else {
            int valueEnd = valueStart;
            while (valueEnd < json.length()) {
                char c = json.charAt(valueEnd);
                if (Character.isDigit(c) || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E') {
                    valueEnd++;
                } else {
                    break;
                }
            }
            
            if (valueEnd <= valueStart) return null;
            return json.substring(valueStart, valueEnd);
        }
    }
}