package exception;

public class EccezioniFile extends RuntimeException {
	
	// Attributo
    private static final long serialVersionUID = 1L;
	
    // Costruttore
    public EccezioniFile(String msg, Throwable cause) {
        super(msg, cause);
    }
    
}