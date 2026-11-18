package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.model.enums.TipoMovimiento;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class MovimientoTarjeta {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private final String id;
    private final LocalDateTime fechaHora;
    private final TipoMovimiento tipo;
    private final double monto;
    private final double saldoResultante;
    private final String descripcion;

    public MovimientoTarjeta(LocalDateTime fechaHora, TipoMovimiento tipo, double monto,
                             double saldoResultante, String descripcion) {
        this.id = "M-" + SECUENCIA.incrementAndGet();
        this.fechaHora = fechaHora;
        this.tipo = tipo;
        this.monto = monto;
        this.saldoResultante = saldoResultante;
        this.descripcion = descripcion;
    }

    public String getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public TipoMovimiento getTipo() { return tipo; }
    public double getMonto() { return monto; }
    public double getSaldoResultante() { return saldoResultante; }
    public String getDescripcion() { return descripcion; }

    @Override
    public String toString() { return tipo + " $" + monto + " - " + descripcion; }
}
