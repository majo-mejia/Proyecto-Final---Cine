package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.model.enums.MotivoCancelacion;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/** Devolución de una compra cancelada. Una compra tiene como máximo un reembolso. */
public class Reembolso {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private final String id;
    private final String compraId;
    private final LocalDateTime fechaHora;
    /** Fracción devuelta: 1.0 = 100 %, 0.8 = 80 %. */
    private final double porcentaje;
    /** Valor devuelto; si la compra se pagó con puntos, este valor se devuelve en puntos. */
    private final double monto;
    private final MotivoCancelacion motivo;

    public Reembolso(String compraId, LocalDateTime fechaHora, double porcentaje, double monto, MotivoCancelacion motivo) {
        this.id = "R-" + SECUENCIA.incrementAndGet();
        this.compraId = compraId;
        this.fechaHora = fechaHora;
        this.porcentaje = porcentaje;
        this.monto = monto;
        this.motivo = motivo;
    }

    public String getId() { return id; }
    public String getCompraId() { return compraId; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public double getPorcentaje() { return porcentaje; }
    public double getMonto() { return monto; }
    public MotivoCancelacion getMotivo() { return motivo; }
}
