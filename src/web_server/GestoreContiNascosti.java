package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;

import java.util.List;

import controller.Controller;
import dto.Conto;

public class GestoreContiNascosti extends BaseGestorePagina {
    
    public GestoreContiNascosti(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return "/conti_nascosti".equals(uri) ||
            "/api/conti-nascosti".equals(uri);
    }
    
    @Override
    public Response handle(IHTTPSession session) throws Exception {
        String uri = session.getUri();
        Method method = session.getMethod();
        
        if ("/conti_nascosti".equals(uri) && method == Method.GET) {
            return serveListaContiPage(session);
        } else if ("/api/conti-nascosti".equals(uri) && method == Method.GET) {
            return handleAccounts(session);  // <-- Fix 1: passa session
        }

        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }
    
    private Response serveListaContiPage(IHTTPSession session) {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        if (!sessionController.getVisibilità()) {  // <-- Fix 2: usa sessionController
            // Se l'utente ha già autorizzato la visibilità dei conti nascosti, reindirizza alla pagina home
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/home");
            return addNoCacheHeaders(response);
        }
        
        try {
            String htmlPath = "conti_nascosti.html";  // <-- Fix 3: usa leggiFile
            String content = leggiFile(htmlPath);
            
            if (content == null) {
                return createResponse(Response.Status.INTERNAL_ERROR, "text/html", 
                    "<h1>Errore durante il caricamento della pagina</h1>");
            }
            
            sessionController.setVisibilità(false);  // <-- Fix 4: usa sessionController
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }

    private Response handleAccounts(IHTTPSession session) {  // <-- Fix 1: aggiungi parametro
        Controller sessionController = getSessionController(session);  // <-- Fix 5: recupera sessionController
        
        if (sessionController == null || !sessionController.isUtenteLogged()) {  // <-- Fix 5: aggiungi check
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            List<Conto> conti = sessionController.getContiNascosti();  // <-- Fix 6: usa sessionController
            
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
}