package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public interface GestorePagina {
    boolean canHandle(String uri, String method);
    Response handle(IHTTPSession session) throws Exception;
}