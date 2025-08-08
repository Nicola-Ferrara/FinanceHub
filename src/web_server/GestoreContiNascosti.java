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
            return serveListaContiPage();
        } else if ("/api/conti-nascosti".equals(uri) && method == Method.GET) {
            return handleAccounts();
        }

        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }
    
    private Response serveListaContiPage() {
        // Verifica che l'utente sia loggato
        if (!controller.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            // Carica il file HTML
            String filePath = "./sito/html/conti_nascosti.html";
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }

    private Response handleAccounts() {
        try {
            List<Conto> conti = controller.getContiNascosti();
            
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