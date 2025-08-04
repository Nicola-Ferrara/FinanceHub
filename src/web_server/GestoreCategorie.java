package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import controller.Controller;
import dto.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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
            return serveCategoriesPage();
        } else if ("/api/categorie/spesa".equals(uri) && method == Method.GET) {
            return getCategorieSpesa();
        } else if ("/api/categorie/guadagno".equals(uri) && method == Method.GET) {
            return getCategorieGuadagno();
        } else if ("/api/categoria".equals(uri) && method == Method.POST) {
            return handleAddCategoria(session);
        } else if (uri.startsWith("/api/categoria/") && method == Method.PUT) {
            return handleUpdateCategoria(uri, session);
        } else if (uri.startsWith("/api/categoria/") && method == Method.DELETE) {
            return handleDeleteCategoria(uri);
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }
    
    private Response serveCategoriesPage() {
        if (!controller.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            String filePath = "./sito/html/categorie.html";
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                "Errore nel caricamento della pagina categorie: " + e.getMessage());
        }
    }
    
    private Response getCategorieSpesa() {
        try {
            LinkedList<Categoria> categorie = controller.getCategoriaSpesa();
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
    
    private Response getCategorieGuadagno() {
        try {
            LinkedList<Categoria> categorie = controller.getCategoriaGuadagno();
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
        try {
            // Leggi il body della richiesta
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String requestBody = body.get("postData");
            
            if (requestBody == null || requestBody.isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Body della richiesta vuoto\"}");
            }
            
            // Estrai i dati dal JSON
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
            
            // Validation tipo
            if (!tipoStr.equals("Spesa") && !tipoStr.equals("Guadagno")) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Tipo categoria non valido. Deve essere 'Spesa' o 'Guadagno'\"}");
            }
            
            // Converti il tipo
            Categoria.TipoCategoria tipo = tipoStr.equals("Spesa") ? 
                Categoria.TipoCategoria.Spesa : Categoria.TipoCategoria.Guadagno;
            
            // Aggiungi la categoria
            controller.aggiungiCategoria(nome.trim(), tipo);
            
            return createResponse(Response.Status.OK, "application/json", 
                "{\"success\": true, \"message\": \"Categoria aggiunta con successo\"}");
                
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + escapeJsonString(e.getMessage()) + "\"}");
        }
    }
    
    private Response handleUpdateCategoria(String uri, IHTTPSession session) {
        try {
            // Estrai ID dalla URI
            String[] parts = uri.split("/");
            if (parts.length < 4) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID categoria mancante nell'URI\"}");
            }
            
            int categoriaId = Integer.parseInt(parts[3]);
            
            // Verifica che non sia una categoria predefinita
            if (categoriaId == 1 || categoriaId == 2) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Non è possibile modificare le categorie predefinite\"}");
            }
            
            // Leggi il body della richiesta
            String requestBody = null;
            
            try {
                String contentLengthHeader = session.getHeaders().get("content-length");
                if (contentLengthHeader != null && !contentLengthHeader.equals("0")) {
                    int contentLength = Integer.parseInt(contentLengthHeader);
                    
                    if (contentLength > 0) {
                        byte[] bodyBytes = new byte[contentLength];
                        java.io.InputStream inputStream = session.getInputStream();
                        
                        int totalRead = 0;
                        while (totalRead < contentLength) {
                            int bytesRead = inputStream.read(bodyBytes, totalRead, contentLength - totalRead);
                            if (bytesRead == -1) break;
                            totalRead += bytesRead;
                        }
                        
                        if (totalRead > 0) {
                            requestBody = new String(bodyBytes, 0, totalRead, "UTF-8");
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback a parseBody
                try {
                    Map<String, String> files = new HashMap<>();
                    session.parseBody(files);
                    requestBody = files.get("postData");
                } catch (Exception e2) {
                    // Ignore fallback errors
                }
            }
            
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Body della richiesta vuoto\"}");
            }
            
            // Estrai il nuovo nome dal JSON
            String nuovoNome = extractJsonValue(requestBody, "nuovoNome");
            
            if (nuovoNome == null || nuovoNome.trim().isEmpty()) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Nuovo nome mancante o vuoto\"}");
            }
            
            // Modifica la categoria
            boolean success = controller.modificaCategoria(categoriaId, nuovoNome.trim());
            
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
    
    private Response handleDeleteCategoria(String uri) {
        try {
            String[] parts = uri.split("/");
            if (parts.length < 4) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"ID categoria mancante nell'URI\"}");
            }
            
            int categoriaId = Integer.parseInt(parts[3]);
            
            // Verifica che non sia una categoria predefinita
            if (categoriaId == 1 || categoriaId == 2) {
                return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                    "{\"error\": \"Non è possibile eliminare le categorie predefinite 'Spesa' e 'Guadagno'\"}");
            }
            
            // Elimina la categoria
            boolean success = controller.eliminaCategoria(categoriaId);
            
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
    
    // Utility per estrarre valori JSON (semplice parsing)
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int valueStart = keyIndex + searchKey.length();
        int valueEnd = json.indexOf("\"", valueStart);
        
        if (valueStart >= json.length() || valueEnd == -1) return null;
        
        return json.substring(valueStart, valueEnd);
    }
    
    // Utility per escape caratteri speciali in JSON
    private String escapeJsonString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}