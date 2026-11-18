package co.edu.uniquindio.cinemauq.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Notificacion {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private final String id;
    private String mensaje;
    private final LocalDateTime fechaHora;
    private boolean leida;

    public Notificacion(String mensaje, LocalDateTime fechaHora) {
        this.id = "N-" + SECUENCIA.incrementAndGet();
        this.mensaje = mensaje;
        this.fechaHora = fechaHora;
    }

    public void marcarLeida() { leida = true; }

    public String getId() { return id; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public boolean estaLeida() { return leida; }

    @Override
    public String toString() { return mensaje; }
}
