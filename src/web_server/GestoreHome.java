package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import controller.Controller;
import dto.Conto;

import java.util.List;

public class GestoreHome extends BaseGestorePagina {
    
    public GestoreHome(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return "/home".equals(uri) || 
               "/api/bilancio".equals(uri) || 
               "/api/conti".equals(uri) ||
               "/logout".equals(uri);
    }
    
    @Override
    public Response handle(IHTTPSession session) throws Exception {
        String uri = session.getUri();
        Method method = session.getMethod();
        
        if ("/home".equals(uri) && method == Method.GET) {
            return serveHomePage();
        } else if ("/api/bilancio".equals(uri) && method == Method.GET) {
            return handleBalance();
        } else if ("/api/conti".equals(uri) && method == Method.GET) {
            return handleAccounts();
        } else if ("/logout".equals(uri) && method == Method.GET) {
            return handleLogout(session); // Aggiungi gestione richiesta logout
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }
    
    private Response serveHomePage() {
        if (!controller.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            String filePath = "./sito/html/home.html";
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            
            // Sostituisci il segnaposto con il nome e cognome dell'utente
            String nomeCognome = controller.getNomeCognome();
            content = content.replace("{{nomeCognome}}", nomeCognome);
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }
    
    private Response handleBalance() {
        try {
            double entrate = controller.calcolaEntrate();
            double uscite = controller.calcolaUscite();

            String json = String.format(java.util.Locale.US, "{\"entrate\": %.2f, \"uscite\": %.2f}", entrate, uscite);

            Response response = createResponse(Response.Status.OK, "application/json", json);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il calcolo del bilancio: " + e.getMessage() + "\"}");
        }
    }
    
    private Response handleAccounts() {
        try {
            List<Conto> conti = controller.getConti();
            
            StringBuilder jsonBuilder = new StringBuilder("[");
            for (int i = 0; i < conti.size(); i++) {
                Conto conto = conti.get(i);
                jsonBuilder.append(String.format(java.util.Locale.US, 
                    "{\"id\": %d, \"nome\": \"%s\", \"tipo\": \"%s\", \"saldo\": %.2f}",
                    conto.getID(), conto.getNome(), conto.getTipo(), conto.getSaldo()));
                
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

    private Response handleLogout(IHTTPSession session) {
        try {
            controller.clearUtente();
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Errore durante il logout: " + e.getMessage());
        }
    }

}