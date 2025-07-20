package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import controller.Controller;
import dto.*;

public class GestoreConti extends BaseGestorePagina {
    
    public GestoreConti(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return uri.startsWith("/conti") || 
               uri.startsWith("/api/conto/") ||
               uri.startsWith("/api/categorie");
    }

    @Override
    public Response handle(IHTTPSession session) throws Exception {
        String uri = session.getUri();
        Method method = session.getMethod();
        
        if ("/conti".equals(uri) && method == Method.GET) {
            Map<String, java.util.List<String>> params = session.getParameters();
            String contoId = null;
            
            if (params.containsKey("id") && !params.get("id").isEmpty()) {
                contoId = params.get("id").get(0);
            }
            
            if (contoId != null && !contoId.isEmpty()) {
                return serveContoSpecificPage();
            } else {
                Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
                response.addHeader("Location", "/home");
                return addNoCacheHeaders(response);
            }
        } else if (uri.matches("/api/conto/\\d+/conti-disponibili") && method == Method.GET) {
            return handleContiDisponibili(uri);
        } else if (uri.matches("/api/conto/\\d+/trasferimento") && method == Method.POST) {
            return handleAddTrasferimento(uri, session);
        } else if (uri.matches("/api/conto/\\d+/transazione") && method == Method.POST) {
            return handleAddTransazione(uri, session);
        } else if (uri.startsWith("/api/conto/") && method == Method.GET) {
            return handleContoAPI(uri);
        } else if (uri.startsWith("/api/conto/") && method == Method.PUT) {
            return handleUpdateConto(uri, session);
        } else if (uri.startsWith("/api/conto/") && method == Method.DELETE) {
            return handleDeleteConto(uri, session);
        } else if (uri.startsWith("/api/categorie/") && method == Method.GET) {
            return handleCategorieAPI(uri);
        }
        
        return createResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
    }

    private Response handleDeleteConto(String uri, IHTTPSession session) throws Exception {
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoIdStr = parts[3];
                int contoId = Integer.parseInt(contoIdStr);
                
                // Verifica che il conto esista
                Conto conto = controller.getContoById(contoId);
                if (conto == null) {
                    return createResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Conto non trovato\"}");
                }
                
                // Usa il metodo del controller per eliminare il conto
                controller.modificaConto(conto.getID(), conto.getNome(), false, conto.getTipo());
                
                return createResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"Conto eliminato con successo\"}");
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"Endpoint non valido\"}");
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"ID conto non valido\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json",  "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }

    private Response handleUpdateConto(String uri, IHTTPSession session) throws Exception {
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoIdStr = parts[3];
                int contoId = Integer.parseInt(contoIdStr);
                
                if (controller.getContoById(contoId) == null) {
                    return createResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Conto non trovato\"}");
                }
                
                String body = null;
                int contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
                
                if (contentLength > 0) {
                    byte[] buffer = new byte[contentLength];
                    session.getInputStream().read(buffer, 0, contentLength);
                    body = new String(buffer);
                }
                
                if (body == null || body.isEmpty()) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"Body della richiesta vuoto\"}");
                }
                
                String nome = extractJsonValue(body, "nome");
                String tipo = extractJsonValue(body, "tipo");
                
                if (nome == null || tipo == null) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"Nome e tipo sono obbligatori\"}");
                }
                
                controller.modificaConto(contoId, nome, true, tipo);
                
                return createResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"Conto modificato con successo\"}");
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"Endpoint non valido\"}");
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"ID conto non valido\"}");
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int valueStart = json.indexOf(":", keyIndex) + 1;
        if (valueStart == 0) return null;
        
        while (valueStart < json.length() && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '\t')) {
            valueStart++;
        }
        
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') return null;
        valueStart++;
        
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd == -1) return null;
        
        return json.substring(valueStart, valueEnd);
    }
    
    private Response serveContoSpecificPage() {
        if (!controller.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            String filePath = "./sito/html/conto.html";
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            
            Response response = createResponse(Response.Status.OK, "text/html", content);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }
    
    private Response handleContoAPI(String uri) {
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoId = parts[3];
                
                if (parts.length == 4) {
                    return getContoData(contoId);
                } else if (parts.length == 5 && "operazioni".equals(parts[4])) {
                    return getContoOperazioni(contoId);
                }
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"Endpoint non valido\"}");
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }
    
    private Response getContoData(String contoIdStr) {
        try {
            int contoId = Integer.parseInt(contoIdStr);
            
            Conto conto = controller.getContoById(contoId);
            
            if (conto == null) {
                return createResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Conto non trovato\"}");
            }
            
            String json = String.format(java.util.Locale.US,
                "{\"id\": %d, \"nome\": \"%s\", \"tipo\": \"%s\", \"saldo\": %.2f}",
                conto.getID(), conto.getNome(), conto.getTipo(), conto.getSaldo());
            
            Response response = createResponse(Response.Status.OK, "application/json", json);
            return addNoCacheHeaders(response);
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"ID conto non valido\"}");
        }
    }
    
    private Response getContoOperazioni(String contoIdStr) {
        try {
            int contoId = Integer.parseInt(contoIdStr);
            
            Conto conto = controller.getContoById(contoId);
            if (conto == null) {
                return createResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Conto non trovato\"}");
            }
            
            Operazione[] operazioni = controller.getOperazioniConto(contoId);
            
            StringBuilder jsonBuilder = new StringBuilder("[");
            
            for (int i = 0; i < operazioni.length; i++) {
                Operazione operazione = operazioni[i];
                
                String tipo;
                String categoria;
                boolean isIncoming = false;
                
                if (operazione instanceof Transazione) {
                    Transazione transazione = (Transazione) operazione;
                    tipo = transazione.getCategoria().getTipo().toString();
                    categoria = transazione.getCategoria().getNome();
                } else if (operazione instanceof Trasferimento) {
                    Trasferimento trasferimento = (Trasferimento) operazione;
                    tipo = "Trasferimento";
                    isIncoming = trasferimento.getIdContoDestinatario() == contoId;
                    categoria = isIncoming ? "Trasferimento ricevuto" : "Trasferimento inviato";
                } else {
                    tipo = "Sconosciuto";
                    categoria = "Sconosciuto";
                }
                
                String dataFormattata = operazione.getData().toString();

                
                jsonBuilder.append(String.format(java.util.Locale.US,
                    "{\"id\": %d, \"data\": \"%s\", \"categoria\": \"%s\", \"tipo\": \"%s\", \"importo\": %.2f, \"descrizione\": \"%s\", \"isIncoming\": %s}",
                    operazione.getID(), dataFormattata, categoria, tipo, 
                    operazione.getImporto(), operazione.getDescrizione(), 
                    tipo.equals("Trasferimento") ? Boolean.toString(isIncoming) : "false"));
                
                if (i < operazioni.length - 1) {
                    jsonBuilder.append(",");
                }
            }
            
            jsonBuilder.append("]");
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonBuilder.toString());
            return addNoCacheHeaders(response);
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\": \"ID conto non valido\"}");
        }
    }

    private Response handleCategorieAPI(String uri) {
        try {
            if (uri.equals("/api/categorie/spesa")) {
                return getCategorieSpesa();
            } else if (uri.equals("/api/categorie/guadagno")) {
                return getCategorieGuadagno();
            }
            
            return createResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Endpoint non valido\"}");
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
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
                    categoria.getID(), categoria.getNome(), categoria.getTipo()));
                
                if (i < categorie.size() - 1) {
                    jsonBuilder.append(",");
                }
            }
            jsonBuilder.append("]");
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonBuilder.toString());
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero delle categorie spesa\"}");
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
                    categoria.getID(), categoria.getNome(), categoria.getTipo()));
                
                if (i < categorie.size() - 1) {
                    jsonBuilder.append(",");
                }
            }
            jsonBuilder.append("]");
            
            Response response = createResponse(Response.Status.OK, "application/json", jsonBuilder.toString());
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore durante il recupero delle categorie guadagno\"}");
        }
    }

    private Response handleAddTransazione(String uri, IHTTPSession session) {
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoIdStr = parts[3];
                int contoId = Integer.parseInt(contoIdStr);
                
                // Leggi il body della richiesta con il metodo corretto
                Map<String, String> body = new HashMap<>();
                session.parseBody(body);
                String requestBody = body.get("postData");
                
                if (requestBody == null || requestBody.isEmpty()) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Body della richiesta vuoto\"}");
                }
                
                // Estrai i dati dal JSON
                String importoStr = extractJsonValueNumber(requestBody, "importo");
                String descrizione = extractJsonValue(requestBody, "descrizione");
                String categoriaIdStr = extractJsonValueNumber(requestBody, "categoriaId");
                                
                if (importoStr == null || descrizione == null || categoriaIdStr == null) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Dati mancanti - importo: " + importoStr + ", descrizione: " + descrizione + ", categoriaId: " + categoriaIdStr + "\"}");
                }
                
                double importo = Double.parseDouble(importoStr);
                int categoriaId = Integer.parseInt(categoriaIdStr);
                
                // Trova la categoria
                Categoria categoria = controller.getCategoriaById(categoriaId);
                if (categoria == null) {
                    return createResponse(Response.Status.NOT_FOUND, "application/json", 
                        "{\"error\": \"Categoria non trovata con ID: " + categoriaId + "\"}");
                }
                
                // Aggiungi la transazione
                controller.aggiungiTransazione(importo, descrizione, categoria, contoId);
                
                return createResponse(Response.Status.OK, "application/json", 
                    "{\"success\": true, \"message\": \"Transazione aggiunta con successo\"}");
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Endpoint non valido\"}");
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Dati numerici non validi: " + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }

    // Metodo per estrarre valori numerici dal JSON
    private String extractJsonValueNumber(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int valueStart = json.indexOf(":", keyIndex) + 1;
        if (valueStart == 0) return null;
        
        // Rimuovi spazi
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        
        if (valueStart >= json.length()) return null;
        
        // Per i numeri, non cercare le virgolette
        int valueEnd = valueStart;
        while (valueEnd < json.length() && 
            (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '.')) {
            valueEnd++;
        }
        
        // Trova la fine del valore (virgola o parentesi graffa)
        while (valueEnd < json.length() && 
            json.charAt(valueEnd) != ',' && 
            json.charAt(valueEnd) != '}') {
            valueEnd++;
        }
        
        if (valueEnd > valueStart) {
            String value = json.substring(valueStart, valueEnd).trim();
            // Rimuovi virgole finali se presenti
            if (value.endsWith(",")) {
                value = value.substring(0, value.length() - 1);
            }
            return value;
        }
        
        return null;
    }

    private Response handleContiDisponibili(String uri) {
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoIdStr = parts[3];
                int contoId = Integer.parseInt(contoIdStr);
                
                LinkedList<Conto> contiDisponibili = controller.getSomeConti(contoId);
                
                StringBuilder jsonBuilder = new StringBuilder("[");
                for (int i = 0; i < contiDisponibili.size(); i++) {
                    Conto conto = contiDisponibili.get(i);
                    jsonBuilder.append(String.format(java.util.Locale.US,
                        "{\"id\": %d, \"nome\": \"%s\", \"tipo\": \"%s\", \"saldo\": %.2f}",
                        conto.getID(), conto.getNome(), conto.getTipo(), conto.getSaldo()));
                    
                    if (i < contiDisponibili.size() - 1) {
                        jsonBuilder.append(",");
                    }
                }
                jsonBuilder.append("]");
                
                Response response = createResponse(Response.Status.OK, "application/json", jsonBuilder.toString());
                return addNoCacheHeaders(response);
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Endpoint non valido\"}");
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"ID conto non valido\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }

    private Response handleAddTrasferimento(String uri, IHTTPSession session) {
        try {
            String[] parts = uri.split("/");
            if (parts.length >= 4) {
                String contoIdStr = parts[3];
                int contoMittente = Integer.parseInt(contoIdStr);
                
                // Leggi il body della richiesta
                Map<String, String> body = new HashMap<>();
                session.parseBody(body);
                String requestBody = body.get("postData");
                
                if (requestBody == null || requestBody.isEmpty()) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Body della richiesta vuoto\"}");
                }
                
                // Estrai i dati dal JSON
                String importoStr = extractJsonValueNumber(requestBody, "importo");
                String descrizione = extractJsonValue(requestBody, "descrizione");
                String contoDestinatarioStr = extractJsonValueNumber(requestBody, "contoDestinatario");
                
                if (importoStr == null || descrizione == null || contoDestinatarioStr == null) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Dati mancanti\"}");
                }
                
                double importo = Double.parseDouble(importoStr);
                int contoDestinatario = Integer.parseInt(contoDestinatarioStr);
                
                // Verifica che i conti esistano e siano diversi
                Conto contoMit = controller.getContoById(contoMittente);
                Conto contoDest = controller.getContoById(contoDestinatario);
                
                if (contoMit == null || contoDest == null) {
                    return createResponse(Response.Status.NOT_FOUND, "application/json", 
                        "{\"error\": \"Uno dei conti non è stato trovato\"}");
                }
                
                if (contoMittente == contoDestinatario) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Non puoi trasferire denaro allo stesso conto\"}");
                }
                
                // Verifica fondi sufficienti
                if (importo > contoMit.getSaldo()) {
                    return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                        "{\"error\": \"Fondi insufficienti\"}");
                }
                
                // Effettua il trasferimento
                controller.aggiungiTrasferimento(importo, descrizione, contoMittente, contoDestinatario);
                
                return createResponse(Response.Status.OK, "application/json", 
                    "{\"success\": true, \"message\": \"Trasferimento effettuato con successo\"}");
            }
            
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Endpoint non valido\"}");
        } catch (NumberFormatException e) {
            return createResponse(Response.Status.BAD_REQUEST, "application/json", 
                "{\"error\": \"Dati numerici non validi\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                "{\"error\": \"Errore del server: " + e.getMessage() + "\"}");
        }
    }

}