package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import controller.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestoreLogin extends BaseGestorePagina {
    
    public GestoreLogin(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return ("/login".equals(uri) || "/".equals(uri)) && ("POST".equals(method) || "GET".equals(method));
    }
    
    @Override
    public Response handle(IHTTPSession session) throws Exception {
        if (session.getMethod() == Method.GET) {
            return serveLoginPage();
        } else if (session.getMethod() == Method.POST) {
            return handleLoginAttempt(session);
        }
        
        return createResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Metodo non supportato");
    }
    
    private Response serveLoginPage() {
        try {
            String filePath = "./sito/html/login.html";
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }
    
    private Response handleLoginAttempt(IHTTPSession session) throws Exception {
        // Estrai i parametri dalla richiesta POST
        Map<String, String> postData = new HashMap<>();
        session.parseBody(postData);
        Map<String, List<String>> parameters = session.getParameters();
        String email = parameters.get("email") != null ? parameters.get("email").get(0) : null;
        String password = parameters.get("password") != null ? parameters.get("password").get(0) : null;

        // Usa il Controller per gestire il login
        boolean loginSuccess = controller.effettuaLogin(email, password);

        if (loginSuccess) {
            // Reindirizza a /home
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/home");
            return addNoCacheHeaders(response);
        } else {
            // Restituisci uno stato HTTP 401 per credenziali non valide
            return createResponse(Response.Status.UNAUTHORIZED, "text/html", "Credenziali non valide");
        }
    }
}