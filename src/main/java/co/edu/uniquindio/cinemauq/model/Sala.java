package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.model.enums.TipoSala;

import java.util.*;

public class Sala {

    private final String id;
    private String nombre;
    private TipoSala tipo;
    private final List<Asiento> listaAsientos = new ArrayList<>();
    private final List<Funcion> listaFunciones = new ArrayList<>();

    public Sala(String id, String nombre, TipoSala tipo) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public Asiento crearAsiento(char fila, int numero, boolean accesible) throws DatosInvalidosException {
        if (!Character.isLetter(fila) || numero < 1) {
            throw new DatosInvalidosException("La fila debe ser una letra y el número mayor que cero.");
        }
        Asiento asiento = new Asiento(fila, numero, accesible);
        if (listaAsientos.contains(asiento)) {
            throw new DatosInvalidosException("El asiento " + asiento.obtenerIdentificador() + " ya existe en " + nombre + ".");
        }
        listaAsientos.add(asiento);
        return asiento;
    }


    public void crearAsientos(int filas, int columnas, int filasAccesibles) throws DatosInvalidosException {
        if (filas < 1 || filas > 26 || columnas < 1) {
            throw new DatosInvalidosException("La sala debe tener entre 1 y 26 filas y al menos 1 columna.");
        }
        for (int f = 0; f < filas; f++) {
            for (int c = 1; c <= columnas; c++) {
                crearAsiento((char) ('A' + f), c, f < filasAccesibles);
            }
        }
    }

    public Optional<Asiento> buscarAsiento(String codigo) {
        return listaAsientos.stream().filter(a -> a.obtenerIdentificador().equalsIgnoreCase(codigo)).findFirst();
    }

    public int obtenerCapacidad() { return listaAsientos.size(); }


    void agregarFuncion(Funcion funcion) { listaFunciones.add(funcion); }

    public List<Funcion> getListaFunciones() { return Collections.unmodifiableList(listaFunciones); }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoSala getTipo() { return tipo; }
    public void setTipo(TipoSala tipo) { this.tipo = tipo; }
    public List<Asiento> getListaAsientos() { return Collections.unmodifiableList(listaAsientos); }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Sala s && id.equals(s.id));
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return nombre + " (" + tipo + ")"; }
}
