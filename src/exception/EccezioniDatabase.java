package exception;

public class EccezioniDatabase extends RuntimeException {
	
	// Attributo
	private static final long serialVersionUID = 1L;
	
	// Costruttore
    public EccezioniDatabase(String message, Throwable cause) {
        super(message, cause);
    }
    
}