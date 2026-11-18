package co.edu.uniquindio.cinemauq.model;

public class Entrada {

    private final String id;
    private final double precio;
    private final Funcion funcion;
    private final Asiento asiento;

    public Entrada(String id, Funcion funcion, Asiento asiento, double precio) {
        this.id = id;
        this.funcion = funcion;
        this.asiento = asiento;
        this.precio = precio;
    }

    public String getId() { return id; }
    public double getPrecio() { return precio; }
    public Funcion getFuncion() { return funcion; }
    public Asiento getAsiento() { return asiento; }

    @Override
    public String toString() {
        return "Entrada " + funcion.getPelicula().getTitulo() + " - asiento " + asiento.obtenerIdentificador();
    }
}
