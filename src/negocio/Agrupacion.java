package negocio;

public class Agrupacion {
    private String codigo;
    private String nombre;
    private int votos;

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public Agrupacion(String codigo, String nombre, int votos) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.votos = votos;
    }

    public Agrupacion(Agrupacion a) {
        codigo = a.getCodigo();
        nombre = a.getNombre();
        votos = 0;
    }

    public void sumar(int valor) {
        votos += valor;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("");
        sb.append(" ").append(nombre);
        sb.append("------").append(votos);
        sb.append(' ');
        return sb.toString();
    }
}
