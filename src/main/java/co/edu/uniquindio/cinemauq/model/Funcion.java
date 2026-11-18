package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.AsientoNoDisponibleException;
import co.edu.uniquindio.cinemauq.exceptions.FuncionNoDisponibleException;
import co.edu.uniquindio.cinemauq.exceptions.OperacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.model.enums.EstadoFuncion;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.observer.Observador;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Función = película + sala + fecha y hora (regla 1).
 * Patrón OBSERVER (sujeto): al cancelarse avisa a todos los observadores de listaObservadores.
 */
public class Funcion {

    private final String id;
    private final Pelicula pelicula;
    private final Sala sala;
    private LocalDateTime fechaHora;
    private double precioBase;
    private EstadoFuncion estado = EstadoFuncion.PROGRAMADA;
    private final Map<String, AsientoFuncion> asientos = new LinkedHashMap<>();
    private final List<Observador> listaObservadores = new ArrayList<>();

    public Funcion(String id, Pelicula pelicula, Sala sala, LocalDateTime fechaHora, double precioBase) {
        this.id = id;
        this.pelicula = pelicula;
        this.sala = sala;
        this.fechaHora = fechaHora;
        this.precioBase = precioBase;
        for (Asiento asiento : sala.getListaAsientos()) {
            asientos.put(asiento.obtenerIdentificador(), new AsientoFuncion(asiento));
        }
    }

    // ---------------- Asientos ----------------

    public AsientoFuncion obtenerAsientoFuncion(String codigo) throws AsientoNoDisponibleException {
        AsientoFuncion af = asientos.get(codigo.toUpperCase());
        if (af == null) {
            throw new AsientoNoDisponibleException(
                    "El asiento " + codigo + " no existe en la sala " + sala.getNombre() + ".");
        }
        return af;
    }

    public boolean estaDisponible(String codigo) {
        AsientoFuncion af = asientos.get(codigo.toUpperCase());
        return af != null && af.estaDisponible();
    }

    public void ocuparAsiento(String codigo, LocalDateTime ahora)
            throws AsientoNoDisponibleException, FuncionNoDisponibleException {
        validarVendible(ahora);
        obtenerAsientoFuncion(codigo).marcarOcupado();
    }

    public void liberarAsiento(String codigo) throws AsientoNoDisponibleException {
        obtenerAsientoFuncion(codigo).liberar();
    }

    public Collection<AsientoFuncion> obtenerAsientos() {
        return Collections.unmodifiableCollection(asientos.values());
    }

    public long contarOcupados() {
        return asientos.values().stream().filter(AsientoFuncion::getOcupado).count();
    }

    /** Porcentaje de ocupación de 0 a 100. */
    public double obtenerPorcentajeOcupacion() {
        return asientos.isEmpty() ? 0 : contarOcupados() * 100.0 / asientos.size();
    }

    // ---------------- Tiempo y estado ----------------

    public boolean estaProgramada() { return estado == EstadoFuncion.PROGRAMADA; }

    /** Verdadero desde la hora de inicio en adelante. */
    public boolean yaComenzo(LocalDateTime ahora) { return !ahora.isBefore(fechaHora); }

    /** Precio de una entrada para esta función. Hoy es el precio base (sin recargos por tipo de sala). */
    public double calcularPrecioEntrada() { return precioBase; }

    /** Solo se vende si la función está programada y aún no empieza. */
    public void validarVendible(LocalDateTime ahora) throws FuncionNoDisponibleException {
        if (!estaProgramada()) {
            throw new FuncionNoDisponibleException("La función está cancelada.");
        }
        if (yaComenzo(ahora)) {
            throw new FuncionNoDisponibleException("La función ya empezó.");
        }
    }

    /** Minutos que faltan para que empiece (negativo si ya empezó). */
    public long minutosParaInicio(LocalDateTime ahora) {
        return Duration.between(ahora, fechaHora).toMinutes();
    }

    public LocalDateTime obtenerFechaHoraFin() {
        return fechaHora.plusMinutes(pelicula.getDuracionMinutos());
    }

    public void cancelar() throws OperacionNoPermitidaException {
        if (!estaProgramada()) {
            throw new OperacionNoPermitidaException("La función ya está cancelada.");
        }
        estado = EstadoFuncion.CANCELADA;
        notificarObservadores();
    }

    // ---------------- Observer ----------------

    public void agregarObservador(Observador observador) {
        if (!listaObservadores.contains(observador)) {
            listaObservadores.add(observador);
        }
    }

    public void eliminarObservador(Observador observador) { listaObservadores.remove(observador); }

    private void notificarObservadores() {
        for (Observador o : new ArrayList<>(listaObservadores)) {
            o.actualizar(this);
        }
    }

    // ---------------- Getters / setters ----------------

    public String getId() { return id; }
    public Pelicula getPelicula() { return pelicula; }
    public Sala getSala() { return sala; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public double getPrecioBase() { return precioBase; }
    public void setPrecioBase(double precioBase) { this.precioBase = precioBase; }
    public EstadoFuncion getEstado() { return estado; }
    public List<Observador> getListaObservadores() { return Collections.unmodifiableList(listaObservadores); }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Funcion f && id.equals(f.id));
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return pelicula.getTitulo() + " - " + sala.getNombre() + " - " + fechaHora; }
}
