package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import controller.Controller;

import java.util.HashMap;
import java.util.Map;

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
            return serveAggiungiContoPage();
        } else if ("/api/conto".equals(uri) && method == Method.POST) {
            return handleAddConto(session);
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }
    
    private Response serveAggiungiContoPage() {
        if (!controller.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            String filePath = "./sito/html/aggiungiConto.html";
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }
    
    private Response handleAddConto(IHTTPSession session) {
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String requestBody = body.get("postData");
            
            String nome = extractJsonValue(requestBody, "nome");
            String tipo = extractJsonValue(requestBody, "tipo");
            String saldoStr = extractJsonValue(requestBody, "saldo");
            
            if (nome == null || tipo == null || saldoStr == null) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Dati mancanti\"}");
            }
            
            double saldo = Double.parseDouble(saldoStr);
            
            controller.aggiungiConto(nome, tipo, saldo);
            
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
        
        while (valueStart < json.length() && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '\t')) {
            valueStart++;
        }
        
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') return null;
        valueStart++;
        
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd == -1) return null;
        
        return json.substring(valueStart, valueEnd);
    }
}