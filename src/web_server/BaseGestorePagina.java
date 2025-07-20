package web_server;

import controller.Controller;
import fi.iki.elonen.NanoHTTPD;
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
}