package dto;

public class Categoria {

    public enum TipoCategoria {
        Guadagno,
        Spesa
    }
    
    // Attributi
    private int ID;
    private String nome;
    private TipoCategoria tipo;

    // Costruttore
    public Categoria(int ID, String nome, TipoCategoria tipo) {
        this.ID = ID;
        this.nome = nome;
        this.tipo = tipo;
    }

    // Getters
    public int getID() {
        return ID;
    }
    public String getNome() {
        return nome;
    }
    public TipoCategoria getTipo() {
        return tipo;
    }

    // Setters
    public void setID(int ID) {
        this.ID = ID;
    }
    public void setNome(String nome) {
        this.nome = nome;
    }
    public void setTipo(TipoCategoria tipo) {
        this.tipo = tipo;
    }
}
