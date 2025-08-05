package web_server;

import controller.*;
import exception.EccezioniDatabase;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;


public class GestoreProfilo extends BaseGestorePagina {

    public GestoreProfilo(Controller controller) {
        super(controller);
    }

    @Override
    public boolean canHandle(String uri, String method) {
        return "/profilo".equals(uri) 
            || "/api/profilo".equals(uri);
    }

    @Override
    public Response handle(IHTTPSession session) throws Exception {
        String uri = session.getUri();
        Method method = session.getMethod();
        
        if ("/profilo".equals(uri) && method == Method.GET) {
            return serveProfiloPage();
        } else if ("/api/profilo".equals(uri)) {
            switch (method) {
                case GET:
                    return handleGetProfilo(session);
                case PUT:
                    return handleUpdateProfilo(session);
                case DELETE:
                    return handleDeleteProfilo(session);
                default:
                    return createResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", 
                        "{\"error\": \"Metodo non supportato\"}");
            }
        } else if ("/api/profilo-password".equals(uri) && method == Method.PUT) {
            return handleUpdateProfiloPassword(session);
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }

    private Response serveProfiloPage() {
        if (!controller.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            String filePath = "./sito/html/profilo.html";
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (IOException e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }

    private Response handleGetProfilo(IHTTPSession session) {
        try {
            if (!controller.isUtenteLogged()) {
                return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\": \"Utente non autorizzato\"}");
            }
            
            // Costruisci JSON response con null safety
            String jsonResponse = String.format(java.util.Locale.US,
                "{\"nome\": \"%s\", \"cognome\": \"%s\", \"email\": \"%s\", \"telefono\": \"%s\", \"data\": \"%s\"}",
                controller.getUtente().getNome(),
                controller.getUtente().getCognome(),
                controller.getUtente().getEmail(),
                controller.getUtente().getNumeroTel(),
                controller.getUtente().getDataIscrizione()
            );
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonResponse);
            return addNoCacheHeaders(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero dell'utente: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Response handleUpdateProfilo(IHTTPSession session) {
        try {
            if (!controller.isUtenteLogged()) {
                return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\": \"Utente non autorizzato\"}");
            }
            
            // Leggi il body della richiesta
            String body = null;
            
            try {
                String contentLengthHeader = session.getHeaders().get("content-length");
                int contentLength = 0;
                
                if (contentLengthHeader != null) {
                    contentLength = Integer.parseInt(contentLengthHeader);
                }
                
                if (contentLength > 0) {
                    java.io.InputStream inputStream = session.getInputStream();
                    byte[] buffer = new byte[contentLength];
                    
                    int totalBytesRead = 0;
                    while (totalBytesRead < contentLength) {
                        int bytesRead = inputStream.read(buffer, totalBytesRead, contentLength - totalBytesRead);
                        if (bytesRead == -1) break;
                        totalBytesRead += bytesRead;
                    }
                    
                    body = new String(buffer, 0, totalBytesRead, "UTF-8");
                } else {
                    Map<String, String> files = new java.util.HashMap<>();
                    session.parseBody(files);
                    body = files.get("postData");
                }
                
            } catch (Exception e) {
                // Ignora errori di lettura e continua
            }
            
            if (body == null || body.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Dati mancanti\"}");
            }
            
            // Parse JSON
            String nome = null;
            String cognome = null;
            String telefono = null;
            
            try {
                nome = extractStringFromJson(body, "nome");
                cognome = extractStringFromJson(body, "cognome");
                telefono = extractStringFromJson(body, "telefono");
            } catch (Exception e) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Errore nel parsing dei dati JSON\"}");
            }
            
            // Validazione nome
            if (nome == null || nome.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Il nome è obbligatorio\"}");
            }
            nome = nome.trim();
            if (nome.length() < 3) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Il nome deve essere di almeno 3 caratteri\"}");
            }
            if (nome.length() > 30) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Il nome non può superare i 30 caratteri\"}");
            }
            
            // Validazione cognome
            if (cognome == null || cognome.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Il cognome è obbligatorio\"}");
            }
            cognome = cognome.trim();
            if (cognome.length() < 3) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Il cognome deve essere di almeno 3 caratteri\"}");
            }
            if (cognome.length() > 30) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Il cognome non può superare i 30 caratteri\"}");
            }

            // Validazione telefono
            if (telefono == null || telefono.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Il telefono è obbligatorio\"}");
            }
            telefono = telefono.trim();
            if (telefono.length() != 10) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Il telefono deve essere composto da 10 numeri\"}");
            }
            if (!telefono.matches("\\d{10}")) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Il telefono deve contenere solo cifre\"}");
            }

            controller.modificaUtente(nome, cognome, telefono);
            
            Response response = createResponse(Response.Status.OK, "application/json", 
                "{\"success\": true, \"message\": \"Utente modificato con successo\"}");
            return addNoCacheHeaders(response);
        } catch (EccezioniDatabase e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore database durante la modifica: " + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante la modifica dell'utente: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Response handleDeleteProfilo(IHTTPSession session) {
        try {
            if (!controller.isUtenteLogged()) {
                return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\": \"Utente non autorizzato\"}");
            }
            controller.eliminaUtente();

            Response response = createResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"Utente eliminato con successo\"}");
            return addNoCacheHeaders(response);
            
        } catch (EccezioniDatabase e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore database durante l'eliminazione: " + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante l'eliminazione dell'utente: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private Response handleUpdateProfiloPassword(IHTTPSession session) {
        try {
            if (!controller.isUtenteLogged()) {
                return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                    "{\"error\": \"Utente non autorizzato\"}");
            }
            
            // Leggi il body della richiesta
            String body = null;
            
            try {
                String contentLengthHeader = session.getHeaders().get("content-length");
                int contentLength = 0;
                
                if (contentLengthHeader != null) {
                    contentLength = Integer.parseInt(contentLengthHeader);
                }
                
                if (contentLength > 0) {
                    java.io.InputStream inputStream = session.getInputStream();
                    byte[] buffer = new byte[contentLength];
                    
                    int totalBytesRead = 0;
                    while (totalBytesRead < contentLength) {
                        int bytesRead = inputStream.read(buffer, totalBytesRead, contentLength - totalBytesRead);
                        if (bytesRead == -1) break;
                        totalBytesRead += bytesRead;
                    }
                    
                    body = new String(buffer, 0, totalBytesRead, "UTF-8");
                } else {
                    Map<String, String> files = new java.util.HashMap<>();
                    session.parseBody(files);
                    body = files.get("postData");
                }
                
            } catch (Exception e) {
                // Ignora errori di lettura e continua
            }
            
            if (body == null || body.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Dati mancanti\"}");
            }
            
            // Parse JSON
            String password = extractStringFromJson(body, "password");
            
            // Validazione password
            if (password == null || password.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"La password è obbligatoria\"}");
            }
            password = password.trim();
            if (password.length() < 6 || password.length() > 30) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"La password deve essere tra 6 e 30 caratteri\"}");
            }
            if (!password.matches("^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$")) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"La password deve contenere almeno una lettera maiuscola, un numero e un carattere speciale (@$!%*?&)\"}");
            }
            
            controller.modificaPassword(password);
            
            Response response = createResponse(Response.Status.OK, "application/json", 
                "{\"success\": true, \"message\": \"Password modificata con successo\"}");
            return addNoCacheHeaders(response);
        } catch (EccezioniDatabase e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore database durante la modifica della password: " + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante la modifica della password: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String extractStringFromJson(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*?)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

}