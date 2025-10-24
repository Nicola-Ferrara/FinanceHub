package web_server;

import controller.Controller;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public abstract class BaseGestorePagina implements GestorePagina {
    protected Controller controller;
    
    public BaseGestorePagina(Controller controller) {
        this.controller = controller;
    }
    
    // Metodo di utilità per creare risposte
    protected Response createResponse(Response.Status status, String mimeType, String data) {
        return NanoHTTPD.newFixedLengthResponse(status, mimeType, data);
    }
    
    // Metodo per aggiungere header no-cache
    protected Response addNoCacheHeaders(Response response) {
        response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Expires", "0");
        return response;
    }

    protected Controller getSessionController(IHTTPSession session) {
        String cookieHeader = session.getHeaders().get("cookie");
        String sessionToken = extractSessionToken(cookieHeader);
        
        if (sessionToken == null) {
            return null;
        }
        
        return SessionManager.getSession(sessionToken);
    }

    private String extractSessionToken(String cookieHeader) {
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split("; ")) {
                if (cookie.startsWith("session_token=")) {
                    return cookie.substring("session_token=".length());
                }
            }
        }
        return null;
    }

    protected String leggiFile(String path) {
        try {
            // Costruisci il path assoluto dalla directory del progetto
            java.nio.file.Path filePath = java.nio.file.Paths.get("sito", "html", path);
            
            if (java.nio.file.Files.exists(filePath)) {
                return java.nio.file.Files.readString(filePath, java.nio.charset.StandardCharsets.UTF_8);
            }
            
            System.err.println("File non trovato: " + filePath.toAbsolutePath());
            return null;
        } catch (Exception e) {
            System.err.println("Errore nella lettura del file " + path + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}