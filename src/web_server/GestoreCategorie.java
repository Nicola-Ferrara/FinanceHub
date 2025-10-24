package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import controller.Controller;
import dto.*;

import java.util.LinkedList;

public class GestoreCategorie extends BaseGestorePagina {
    
    public GestoreCategorie(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return "/categorie".equals(uri) || 
               "/api/categorie/spesa".equals(uri) ||
               "/api/categorie/guadagno".equals(uri) ||
               uri.startsWith("/api/categoria");
    }
    
    @Override
    public Response handle(IHTTPSession session) throws Exception {
        String uri = session.getUri();
        Method method = session.getMethod();
        
        if ("/categorie".equals(uri) && method == Method.GET) {
            return serveCategoriesPage(session);
        } else if ("/api/categorie/spesa".equals(uri) && method == Method.GET) {
            return getCategorieSpesa(session);  // <-- Passa session
        } else if ("/api/categorie/guadagno".equals(uri) && method == Method.GET) {
            return getCategorieGuadagno(session);  // <-- Passa session
        } else if ("/api/categoria".equals(uri) && method == Method.POST) {
            return handleAddCategoria(session);
        } else if (uri.startsWith("/api/categoria/") && method == Method.PUT) {
            return handleUpdateCategoria(uri, session);
        } else if (uri.startsWith("/api/categoria/") && method == Method.DELETE) {
            return handleDeleteCategoria(uri, session);  // <-- Passa session
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }
    
    private Response serveCategoriesPage(IHTTPSession session) {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        String htmlPath = "categorie.html";
        String html = leggiFile(htmlPath);
        
        if (html == null) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/html", 
                "<h1>Errore durante il caricamento della pagina</h1>");
        }
        
        Response response = createResponse(Response.Status.OK, "text/html", html);
        return addNoCacheHeaders(response);
    }
    
    private Response getCategorieSpesa(IHTTPSession session) {  // <-- Aggiungi parametro
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            LinkedList<Categoria> categorie = sessionController.getCategoriaSpesa();  // <-- Usa sessionController
            StringBuilder jsonBuilder = new StringBuilder("[");
            
            for (int i = 0; i < categorie.size(); i++) {
                Categoria categoria = categorie.get(i);
                jsonBuilder.append(String.format(
                    "{\"id\": %d, \"nome\": \"%s\", \"tipo\": \"%s\"}", 
                    categoria.getID(), 
                    escapeJsonString(categoria.getNome()), 
                    categoria.getTipo()));
                
                if (i < categorie.size() - 1) {
                    jsonBuilder.append(",");
                }
            }
            jsonBuilder.append("]");
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonBuilder.toString());
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero delle categorie spesa: " + escapeJsonString(e.getMessage()) + "\"}");
        }
    }
    
    private Response getCategorieGuadagno(IHTTPSession session) {  // <-- Aggiungi parametro
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            LinkedList<Categoria> categorie = sessionController.getCategoriaGuadagno();  // <-- Usa sessionController
            StringBuilder jsonBuilder = new StringBuilder("[");
            
            for (int i = 0; i < categorie.size(); i++) {
                Categoria categoria = categorie.get(i);
                jsonBuilder.append(String.format(
                    "{\"id\": %d, \"nome\": \"%s\", \"tipo\": \"%s\"}", 
                    categoria.getID(), 
                    escapeJsonString(categoria.getNome()), 
                    categoria.getTipo()));
                
                if (i < categorie.size() - 1) {
                    jsonBuilder.append(",");
                }
            }
            jsonBuilder.append("]");
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonBuilder.toString());
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero delle categorie guadagno: " + escapeJsonString(e.getMessage()) + "\"}");
        }
    }
    
    private Response handleAddCategoria(IHTTPSession session) {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
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
            String tipoStr = extractJsonValue(requestBody, "tipo");
            
            if (nome == null || nome.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Nome categoria mancante o vuoto\"}");
            }
            
            if (tipoStr == null || tipoStr.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Tipo categoria mancante\"}");
            }
            
            if (!tipoStr.equals("Spesa") && !tipoStr.equals("Guadagno")) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Tipo categoria non valido. Deve essere 'Spesa' o 'Guadagno'\"}");
            }
            
            Categoria.TipoCategoria tipo = tipoStr.equals("Spesa") ? 
                Categoria.TipoCategoria.Spesa : Categoria.TipoCategoria.Guadagno;
            
            sessionController.aggiungiCategoria(nome.trim(), tipo);  // <-- Usa sessionController
            
            return createResponse(Response.Status.OK, "application/json", 
                "{\"success\": true, \"message\": \"Categoria aggiunta con successo\"}");
                
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + escapeJsonString(e.getMessage()) + "\"}");
        }
    }
    
    private Response handleUpdateCategoria(String uri, IHTTPSession session) {
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            String[] parts = uri.split("/");
            if (parts.length < 4) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID categoria mancante nell'URI\"}");
            }
            
            int categoriaId = Integer.parseInt(parts[3]);
            
            if (categoriaId == 1 || categoriaId == 2) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Non è possibile modificare le categorie predefinite\"}");
            }
            
            String contentLengthHeader = session.getHeaders().get("content-length");
            if (contentLengthHeader == null) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Body della richiesta vuoto\"}");
            }
            
            int contentLength = Integer.parseInt(contentLengthHeader);
            byte[] buffer = new byte[contentLength];
            int bytesRead = session.getInputStream().read(buffer, 0, contentLength);
            String requestBody = new String(buffer, 0, bytesRead, "UTF-8");
            
            String nuovoNome = extractJsonValue(requestBody, "nuovoNome");
            
            if (nuovoNome == null || nuovoNome.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Nuovo nome mancante o vuoto\"}");
            }
            
            boolean success = sessionController.modificaCategoria(categoriaId, nuovoNome.trim());  // <-- Usa sessionController
            
            if (!success) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Categoria non trovata o impossibile modificare questa categoria\"}");
            }
            
            return createResponse(Response.Status.OK, "application/json", 
                "{\"success\": true, \"message\": \"Categoria modificata con successo\"}");
                
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"ID categoria non valido\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + escapeJsonString(e.getMessage()) + "\"}");
        }
    }
    
    private Response handleDeleteCategoria(String uri, IHTTPSession session) {  // <-- Aggiungi parametro
        Controller sessionController = getSessionController(session);
        if (sessionController == null || !sessionController.isUtenteLogged()) {
            return createResponse(Response.Status.UNAUTHORIZED, "application/json", 
                "{\"error\": \"Non autorizzato\"}");
        }
        
        try {
            String[] parts = uri.split("/");
            if (parts.length < 4) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID categoria mancante nell'URI\"}");
            }
            
            int categoriaId = Integer.parseInt(parts[3]);
            
            if (categoriaId == 1 || categoriaId == 2) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Non è possibile eliminare le categorie predefinite 'Spesa' e 'Guadagno'\"}");
            }
            
            boolean success = sessionController.eliminaCategoria(categoriaId);  // <-- Usa sessionController
            
            if (!success) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Categoria non trovata o impossibile eliminare questa categoria\"}");
            }
            
            return createResponse(Response.Status.OK, "application/json", 
                "{\"success\": true, \"message\": \"Categoria eliminata con successo\"}");
                
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"ID categoria non valido\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + escapeJsonString(e.getMessage()) + "\"}");
        }
    }
    
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int valueStart = keyIndex + searchKey.length();
        int valueEnd = json.indexOf("\"", valueStart);
        
        if (valueStart >= json.length() || valueEnd == -1) return null;
        
        return json.substring(valueStart, valueEnd);
    }
    
    private String escapeJsonString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}