package web_server;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import controller.Controller;

public class SessionManager {
    private static final ConcurrentHashMap<String, Controller> sessions = new ConcurrentHashMap<>();

    public static String createSession(Controller controller) {
        String sessionToken = UUID.randomUUID().toString();
        sessions.put(sessionToken, controller);
        return sessionToken;
    }

    public static Controller getSession(String sessionToken) {
        return sessions.get(sessionToken);
    }

    public static void removeSession(String sessionToken) {
        sessions.remove(sessionToken);
    }
}