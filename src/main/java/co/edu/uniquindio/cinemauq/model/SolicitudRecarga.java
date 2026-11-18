package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.exceptions.OperacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.model.enums.EstadoSolicitudRecarga;

import java.time.LocalDateTime;

/** Solicitud de recarga de un cliente; el administrador la aprueba (y recarga) o la rechaza. */
public class SolicitudRecarga {

    private final String id;
    private final double montoSolicitado;
    private final LocalDateTime fechaSolicitud;
    private final String correoElectronico;
    private EstadoSolicitudRecarga estado = EstadoSolicitudRecarga.PENDIENTE;

    public SolicitudRecarga(String id, double montoSolicitado, LocalDateTime fechaSolicitud, String correoElectronico)
            throws DatosInvalidosException {
        if (montoSolicitado <= 0) {
            throw new DatosInvalidosException("El monto de la recarga debe ser mayor que cero.");
        }
        this.id = id;
        this.montoSolicitado = montoSolicitado;
        this.fechaSolicitud = fechaSolicitud;
        this.correoElectronico = correoElectronico;
    }

    public boolean estaPendiente() { return estado == EstadoSolicitudRecarga.PENDIENTE; }

    public void aprobar() throws OperacionNoPermitidaException {
        cambiarEstado(EstadoSolicitudRecarga.APROBADA);
    }

    public void rechazar() throws OperacionNoPermitidaException {
        cambiarEstado(EstadoSolicitudRecarga.RECHAZADA);
    }

    private void cambiarEstado(EstadoSolicitudRecarga nuevo) throws OperacionNoPermitidaException {
        if (!estaPendiente()) {
            throw new OperacionNoPermitidaException("La solicitud " + id + " ya fue " + estado.name().toLowerCase() + ".");
        }
        estado = nuevo;
    }

    public String getId() { return id; }
    public double getMontoSolicitado() { return montoSolicitado; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public String getCorreoElectronico() { return correoElectronico; }
    public EstadoSolicitudRecarga getEstado() { return estado; }
}
