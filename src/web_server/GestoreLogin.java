package web_server;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import controller.Controller;
import exception.EccezioniDatabase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GestoreLogin extends BaseGestorePagina {
    
    public GestoreLogin(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return ("/login".equals(uri) || "/".equals(uri)) && ("POST".equals(method) || "GET".equals(method) || "HEAD".equals(method));
    }

    @Override
    public Response handle(IHTTPSession session) throws IOException {
        if (session.getMethod() == NanoHTTPD.Method.GET || session.getMethod() == NanoHTTPD.Method.HEAD) {
            String htmlPath = "login.html";
            String html = leggiFile(htmlPath);
            if (html == null) {
                return createResponse(Response.Status.INTERNAL_ERROR, "text/html", 
                    "<h1>Errore durante il caricamento della pagina</h1>");
            }
            return createResponse(Response.Status.OK, "text/html", html);
        }
        
        // Se è POST, leggi il body direttamente dall'InputStream
        Map<String, String> params = new HashMap<>();
        
        try {
            String contentLengthHeader = session.getHeaders().get("content-length");
            if (contentLengthHeader != null) {
                int contentLength = Integer.parseInt(contentLengthHeader);
                
                byte[] buffer = new byte[contentLength];
                int bytesRead = session.getInputStream().read(buffer, 0, contentLength);
                String postData = new String(buffer, 0, bytesRead, "UTF-8");
                
                if (postData != null && !postData.isEmpty()) {
                    String[] pairs = postData.split("&");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=", 2);
                        if (keyValue.length == 2) {
                            String key = java.net.URLDecoder.decode(keyValue[0], "UTF-8");
                            String value = java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                            params.put(key, value);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore nella lettura del body\"}");
        }

        String email = params.get("email");
        String password = params.get("password");

        if (email == null || password == null) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Email e password sono richiesti\"}");
        }

        Controller sessionController = new Controller();
        
        try {
            boolean loginSuccess = sessionController.effettuaLogin(email, password);
            if (loginSuccess) {
                String sessionToken = SessionManager.createSession(sessionController);
                
                Response response = createResponse(Response.Status.OK, "application/json", "{\"success\": true}");
                response.addHeader("Set-Cookie", "session_token=" + sessionToken + "; Path=/; HttpOnly; Max-Age=86400");
                
                return response;
            } else {
                return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\": \"Email o password non corretti\"}");
            }
        } catch (EccezioniDatabase e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server\"}");
        }
    }
}