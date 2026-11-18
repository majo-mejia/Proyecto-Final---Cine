package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.model.enums.MetodoPago;
import co.edu.uniquindio.cinemauq.model.enums.MotivoCancelacion;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.state.EstadoCompra;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.state.EstadoPagada;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.state.EstadoPendiente;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy.PoliticaReembolso;
import co.edu.uniquindio.cinemauq.patrones.creacionales.builder.CompraBuilder;
import co.edu.uniquindio.cinemauq.patrones.creacionales.singleton.ConfiguracionCine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Compra de entradas y confitería. Solo se crea con CompraBuilder (patrón Builder) y su
 * ciclo de vida lo controla EstadoCompra (patrón State): Pendiente → Pagada → Cancelada.
 */
public class Compra {

    private final String id;
    private final LocalDateTime fechaHora;
    private final double subtotal;
    private final double descuentoTotal;
    private final double serviceFee;
    private final double total;
    private EstadoCompra estado = new EstadoPendiente();
    private final MetodoPago metodo;
    private final int puntosUtilizados;
    private final Cliente cliente;
    private final Funcion funcion;
    private final List<Entrada> listaEntradas;
    private final List<LineaConfiteria> listaLineasConfiteria;
    private final List<Promocion> listaPromociones;
    private Reembolso reembolso;

    /**
     * Recibe las partes que juntó el builder, valida que el contenido cumpla las reglas y calcula
     * sus valores. Si algo no cumple lanza la excepción y la compra no llega a existir.
     */
    public Compra(CompraBuilder builder) throws DatosInvalidosException {
        this.id = builder.getId();
        this.fechaHora = builder.getFechaHora();
        this.metodo = builder.getMetodo();
        this.cliente = builder.getCliente();
        this.funcion = builder.getFuncion();
        this.listaEntradas = new ArrayList<>(builder.getListaEntradas());
        this.listaLineasConfiteria = new ArrayList<>(builder.getListaLineasConfiteria());
        this.listaPromociones = new ArrayList<>(builder.getListaPromociones());

        ConfiguracionCine config = ConfiguracionCine.getInstancia();
        validarContenido();
        this.subtotal = listaEntradas.stream().mapToDouble(Entrada::getPrecio).sum()
                + listaLineasConfiteria.stream().mapToDouble(LineaConfiteria::getSubtotal).sum();
        this.descuentoTotal = calcularDescuentoTotal();
        this.serviceFee = config.getServiceFee();
        this.total = calcularTotal();
        this.puntosUtilizados = metodo == MetodoPago.PUNTOS ? calcularPuntosRequeridos() : 0;
    }

    // ---------------- Validación y cálculos ----------------

    /**
     * Reglas del contenido: cliente, función y fecha obligatorios; entre 1 y maxAsientosPorCompra
     * entradas; como máximo maxPromocionesPorCompra promociones y todas vigentes en la fecha.
     */
    public void validarContenido() throws DatosInvalidosException {
        ConfiguracionCine config = ConfiguracionCine.getInstancia();
        if (cliente == null || funcion == null || fechaHora == null) {
            throw new DatosInvalidosException("La compra necesita cliente, función y fecha.");
        }
        if (listaEntradas.isEmpty()) {
            throw new DatosInvalidosException("Debe elegir al menos un asiento.");
        }
        if (listaEntradas.size() > config.getMaxAsientosPorCompra()) {
            throw new DatosInvalidosException("Máximo " + config.getMaxAsientosPorCompra() + " asientos por compra.");
        }
        if (listaPromociones.size() > config.getMaxPromocionesPorCompra()) {
            throw new DatosInvalidosException("Máximo " + config.getMaxPromocionesPorCompra() + " promociones por compra.");
        }
        for (Promocion p : listaPromociones) {
            if (!p.estaVigente(fechaHora.toLocalDate())) {
                throw new DatosInvalidosException("La promoción " + p.getNombre() + " no está vigente.");
            }
        }
    }

    /** Suma de los descuentos de las promociones, sin pasar del subtotal (el total nunca es negativo). */
    public double calcularDescuentoTotal() {
        double descuentos = listaPromociones.stream().mapToDouble(p -> p.calcularDescuento(subtotal)).sum();
        return Math.min(subtotal, descuentos);
    }

    /** total = subtotal − descuentos + tarifa de servicio. */
    public double calcularTotal() {
        return subtotal - descuentoTotal + serviceFee;
    }

    /** Puntos para pagar con puntos: entradas × costoEntradaEnPuntos + costoEnPuntos de cada ítem. */
    private int calcularPuntosRequeridos() throws DatosInvalidosException {
        int puntos = listaEntradas.size() * ConfiguracionCine.getInstancia().getCostoEntradaEnPuntos();
        for (LineaConfiteria linea : listaLineasConfiteria) {
            int costo = linea.getItem().obtenerCostoEnPuntos();
            if (costo <= 0) {
                throw new DatosInvalidosException(linea.getItem().obtenerNombre() + " no se puede pagar con puntos.");
            }
            puntos += costo * linea.getCantidad();
        }
        return puntos;
    }

    // ---------------- Operaciones delegadas al estado (patrón State) ----------------

    public void pagar() throws CinemaUQException {
        estado.pagar(this);
    }

    public void cancelar(PoliticaReembolso politica, MotivoCancelacion motivo, LocalDateTime ahora)
            throws CinemaUQException {
        estado.cancelar(this, politica, motivo, ahora);
    }

    /** Uso exclusivo de las clases EstadoCompra. */
    public void cambiarEstado(EstadoCompra nuevoEstado) { this.estado = nuevoEstado; }

    /** Uso exclusivo de las clases EstadoCompra. */
    public void registrarReembolso(Reembolso reembolso) { this.reembolso = reembolso; }

    // ---------------- Consultas ----------------

    public boolean estaPagada() { return estado instanceof EstadoPagada; }

    public String getNombreEstado() { return estado.getNombre(); }

    public String getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public double getSubtotal() { return subtotal; }
    public double getDescuentoTotal() { return descuentoTotal; }
    public double getServiceFee() { return serviceFee; }
    public double getTotal() { return total; }
    public EstadoCompra getEstado() { return estado; }
    public MetodoPago getMetodo() { return metodo; }
    public int getPuntosUtilizados() { return puntosUtilizados; }
    public Cliente getCliente() { return cliente; }
    public Funcion getFuncion() { return funcion; }
    public List<Entrada> getListaEntradas() { return Collections.unmodifiableList(listaEntradas); }
    public List<LineaConfiteria> getListaLineasConfiteria() { return Collections.unmodifiableList(listaLineasConfiteria); }
    public List<Promocion> getListaPromociones() { return Collections.unmodifiableList(listaPromociones); }
    public Reembolso getReembolso() { return reembolso; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Compra c && id.equals(c.id));
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return id + " - " + getNombreEstado() + " - $" + total; }
}
