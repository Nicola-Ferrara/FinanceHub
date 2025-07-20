package web_server;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import controller.Controller;

public class GestoreConti extends BaseGestorePagina {
    
    public GestoreConti(Controller controller) {
        super(controller);
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return "/conti".equals(uri) && "GET".equals(method);
    }
    
    @Override
    public Response handle(IHTTPSession session) throws Exception {
        return serveContiPage(session);
    }
    
    private Response serveContiPage(IHTTPSession session) {
        if (!controller.isUtenteLogged()) {
            Response response = createResponse(Response.Status.REDIRECT, "text/html", "");
            response.addHeader("Location", "/login");
            return addNoCacheHeaders(response);
        }
        
        try {
            // Per ora restituiamo una pagina placeholder
            String html = """
                <!DOCTYPE html>
                <html lang="it">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>FinanceHub - Gestione Conti</title>
                    <link rel="stylesheet" href="../css/home.css">
                    <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;600&display=swap" rel="stylesheet">
                </head>
                <body>
                    <header class="header">
                        <h1>Gestione Conti</h1>
                        <div>
                            <a href="/home" style="color: white; text-decoration: none; margin-right: 20px;">← Torna alla Home</a>
                            <form action="/logout" method="get" style="display: inline;">
                                <button type="submit" class="logout-button">Logout</button>
                            </form>
                        </div>
                    </header>
                    
                    <main class="main-content" style="grid-template-columns: 1fr;">
                        <section class="balance-section">
                            <h2>Gestione Conti</h2>
                            <p style="text-align: center; font-size: 18px; color: #666;">
                                Questa sezione è in fase di sviluppo.<br>
                                Qui potrai gestire i tuoi conti, visualizzare le transazioni dettagliate e molto altro!
                            </p>
                            <div style="text-align: center; margin-top: 30px;">
                                <a href="/home" style="
                                    display: inline-block;
                                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                    color: white;
                                    text-decoration: none;
                                    padding: 15px 30px;
                                    border-radius: 25px;
                                    font-weight: 500;
                                    transition: all 0.3s ease;
                                    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
                                ">
                                    Torna alla Home
                                </a>
                            </div>
                        </section>
                    </main>
                </body>
                </html>
                """;
            
            Response response = createResponse(Response.Status.OK, "text/html", html);
            return addNoCacheHeaders(response);
        } catch (Exception e) {
            return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Errore nel caricamento della pagina: " + e.getMessage());
        }
    }
}