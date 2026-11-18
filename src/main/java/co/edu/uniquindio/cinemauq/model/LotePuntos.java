package co.edu.uniquindio.cinemauq.model;

import java.time.LocalDate;

/** Puntos ganados en una compra; vencen al cumplirse la vigencia (regla 5). */
public class LotePuntos {

    private int puntosObtenidos;
    private int puntosDisponibles;
    private final LocalDate fechaObtencion;
    private final LocalDate fechaVencimiento;
    /** Compra que generó los puntos; null si no salieron de una compra (p. ej. devolución de puntos). */
    private final String idCompra;
    private int puntosAnulados;

    public LotePuntos(int puntosObtenidos, LocalDate fechaObtencion, int vigenciaAnios, String idCompra) {
        this.puntosObtenidos = puntosObtenidos;
        this.puntosDisponibles = puntosObtenidos;
        this.fechaObtencion = fechaObtencion;
        this.fechaVencimiento = fechaObtencion.plusYears(vigenciaAnios);
        this.idCompra = idCompra;
    }

    /** Vencidos a partir del día en que se cumple la vigencia. */
    public boolean estanVencido(LocalDate hoy) {
        return !hoy.isBefore(fechaVencimiento);
    }

    /** Consume hasta "cantidad" puntos y devuelve cuántos consumió realmente. */
    int consumirPuntos(int cantidad) {
        int usados = Math.min(cantidad, puntosDisponibles);
        puntosDisponibles -= usados;
        return usados;
    }

    /**
     * Deja el lote sin puntos disponibles (la compra que lo generó se canceló) y devuelve cuántos
     * anuló. Los que el cliente ya gastó no se pueden recuperar.
     */
    int anularPuntosDisponibles() {
        int anulados = puntosDisponibles;
        puntosDisponibles = 0;
        puntosAnulados += anulados;
        return anulados;
    }

    public boolean esDeCompra(String idCompra) {
        return this.idCompra != null && this.idCompra.equals(idCompra);
    }

    public int getPuntosObtenidos() { return puntosObtenidos; }
    public void setPuntosObtenidos(int puntosObtenidos) { this.puntosObtenidos = puntosObtenidos; }
    public int getPuntosDisponibles() { return puntosDisponibles; }
    public LocalDate getFechaObtencion() { return fechaObtencion; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public String getIdCompra() { return idCompra; }
    public int getPuntosAnulados() { return puntosAnulados; }
}
