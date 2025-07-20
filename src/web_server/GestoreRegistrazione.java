package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import controller.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestoreRegistrazione extends BaseGestorePagina {
    
    public GestoreRegistrazione(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return "/register".equals(uri) && 
               ("POST".equals(method) || "GET".equals(method));
    }
    
    @Override
    public Response handle(IHTTPSession session) throws Exception {
        if (session.getMethod() == Method.GET) {
            // Serve la pagina di registrazione
            return serveRegisterPage();
        } else if (session.getMethod() == Method.POST) {
            // Gestisci il tentativo di registrazione
            return handleRegistrationAttempt(session);
        }
        
        return createResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Metodo non supportato");
    }
    
    private Response serveRegisterPage() {
        try {
            String filePath = "./sito/html/register.html";
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }
    
    private Response handleRegistrationAttempt(IHTTPSession session) throws Exception {
        // Estrai i parametri dalla richiesta POST
        Map<String, String> postData = new HashMap<>();
        session.parseBody(postData);
        Map<String, List<String>> parameters = session.getParameters();

        String nome = parameters.get("nome") != null ? parameters.get("nome").get(0) : null;
        String cognome = parameters.get("cognome") != null ? parameters.get("cognome").get(0) : null;
        String telefono = parameters.get("telefono") != null ? parameters.get("telefono").get(0) : null;
        String email = parameters.get("email") != null ? parameters.get("email").get(0) : null;
        String password = parameters.get("password") != null ? parameters.get("password").get(0) : null;

        // Usa il Controller per gestire la registrazione
        boolean registrazioneSuccesso = controller.effettuaRegistrazione(nome, cognome, telefono, email, password);

        if (registrazioneSuccesso) {
            // Registrazione avvenuta con successo
            Response response = createResponse(Response.Status.CREATED, "text/plain", "Registrazione completata con successo");
            return addNoCacheHeaders(response);
        } else {
            // Email già registrata o altro errore
            return createResponse(Response.Status.BAD_REQUEST, "text/plain", "Email già registrata");
        }
    }
}