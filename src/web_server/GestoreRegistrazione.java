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
        return "/register".equals(uri) && ("POST".equals(method) || "GET".equals(method));
    }
    
    @Override
    public Response handle(IHTTPSession session) throws Exception {
        if (session.getMethod() == Method.GET) {
            return serveRegisterPage();
        } else if (session.getMethod() == Method.POST) {
            return handleRegistrationAttempt(session);
        }
        
        return createResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Metodo non supportato");
    }
    
    private Response serveRegisterPage() {
        try {
            String htmlPath = "register.html";  // <-- Fix: usa leggiFile
            String content = leggiFile(htmlPath);
            
            if (content == null) {
                return createResponse(Response.Status.INTERNAL_ERROR, "text/html", 
                    "<h1>Errore durante il caricamento della pagina</h1>");
            }
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return addNoCacheHeaders(createResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                "Errore nel caricamento della pagina: " + e.getMessage()));
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

        // Crea un nuovo Controller per la registrazione (non usare quello condiviso)
        Controller registrationController = new Controller();  // <-- Fix: nuovo controller
        boolean registrazioneSuccesso = registrationController.effettuaRegistrazione(nome, cognome, telefono, email, password);

        if (registrazioneSuccesso) {
            return addNoCacheHeaders(createResponse(Response.Status.CREATED, "text/plain", "Registrazione completata con successo"));
        } else {
            return addNoCacheHeaders(createResponse(Response.Status.BAD_REQUEST, "text/plain", "Email già registrata"));
        }
    }
}