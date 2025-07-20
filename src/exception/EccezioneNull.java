package exception;

public class EccezioneNull extends RuntimeException {

    // Attributo
    private static final long serialVersionUID = 1L;

    // Costruttore con messaggio personalizzato
    public EccezioneNull(String message) {
        super(message);
    }

    // Costruttore con messaggio e causa
    public EccezioneNull(String message, Throwable cause) {
        super(message, cause);
    }

    // Metodo statico per gestire l'eccezione
    public static void errorePWD(NullPointerException e) {
        System.err.println("ERRORE DI LETTURA DI PWDFILE: " + e.getMessage());
    }
}