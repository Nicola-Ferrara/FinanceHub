package web_server;

import fi.iki.elonen.NanoHTTPD;
import controller.Controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebServer extends NanoHTTPD {

    private List<GestorePagina> gestori;

    public WebServer(Controller controller) throws IOException {
        // ✅ LEGGI LA PORTA DALL'AMBIENTE (Render usa PORT)
        super(Integer.parseInt(System.getenv().getOrDefault("PORT", "8080")));
        
        // Registra i gestori
        this.gestori = new ArrayList<>();
        this.gestori.add(new GestoreLogin(controller));
        this.gestori.add(new GestoreHome(controller));
        this.gestori.add(new GestoreRegistrazione(controller));
        this.gestori.add(new GestoreConti(controller));
        this.gestori.add(new GestoreAggiungiConto(controller));
        
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        System.out.println("FinanceHub Server avviato su porta: " + port);
        System.out.println("Server raggiungibile su: http://localhost:" + port);
    }


    @Override
    public Response serve(IHTTPSession session) {
        try {
            String uri = session.getUri();
            String method = session.getMethod().toString();

            if (uri.equals("/favicon.ico")) {
                return serveStaticFile(uri);
            }
            
            // Cerca il gestore appropriato
            for (GestorePagina gestore : gestori) {
                if (gestore.canHandle(uri, method)) {
                    return gestore.handle(session);
                }
            }
            
            // Gestione dei file statici (CSS, JS, immagini)
            if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/img/")) {
                return serveStaticFile(uri);
            }
            
            // Nessun gestore trovato
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Risorsa non trovata");
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Errore del server: " + e.getMessage());
        }
    }
    
    private Response serveStaticFile(String uri) {
        try {
            String filePath = "./sito" + uri;
            String mimeType = getContentTypeForFile(uri);
            
            byte[] fileData = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
            
            Response response = newFixedLengthResponse(Response.Status.OK, mimeType, new String(fileData));
            response.addHeader("Cache-Control", "max-age=86400"); // Cache per 24 ore
            return response;
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File non trovato");
        }
    }
    
    private String getContentTypeForFile(String uri) {
        if (uri.endsWith(".css")) return "text/css";
        if (uri.endsWith(".js")) return "application/javascript";
        if (uri.endsWith(".png")) return "image/png";
        if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
        if (uri.endsWith(".gif")) return "image/gif";
        if (uri.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

}