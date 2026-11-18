package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.model.enums.TipoUsuario;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.observer.Observador;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Patrón OBSERVER (observador concreto): el cliente se suscribe a las funciones que compra y,
 * si el cine cancela una, recibe una notificación sin que Funcion conozca a la clase Cliente.
 */
public class Cliente extends Usuario implements Observador {

    private final LocalDate fechaRegistro;
    private final TarjetaVirtual tarjeta;
    private final CuentaPuntos cuentaPuntos;
    private final List<Notificacion> listaNotificaciones = new ArrayList<>();
    private final List<Compra> listaCompras = new ArrayList<>();

    public Cliente(String nombre, String documento, String correoElectronico, String contrasena,
                   LocalDate fechaRegistro, TarjetaVirtual tarjeta) {
        super(nombre, documento, correoElectronico, contrasena, TipoUsuario.CLIENTE);
        this.fechaRegistro = fechaRegistro;
        this.tarjeta = tarjeta;
        this.cuentaPuntos = new CuentaPuntos(documento);
    }

    public TarjetaVirtual obtenerTarjeta() { return tarjeta; }

    public CuentaPuntos obtenerCuentaPuntos() { return cuentaPuntos; }

    @Override
    public void actualizar(Funcion funcion) {
        notificar(new Notificacion("La función de " + funcion.getPelicula().getTitulo() + " del "
                + funcion.getFechaHora() + " fue cancelada. Se reembolsará el 100 % de su compra.",
                LocalDateTime.now()));
    }

    public void notificar(Notificacion notificacion) { listaNotificaciones.add(notificacion); }

    public void agregarCompra(Compra compra) { listaCompras.add(compra); }

    public List<Compra> consultarHistorialCompras() { return Collections.unmodifiableList(listaCompras); }

    /** Crea la solicitud con el correo del cliente; el cine la guarda y el administrador la responde. */
    public SolicitudRecarga solicitarRecarga(String id, double monto, LocalDateTime fecha) throws DatosInvalidosException {
        return new SolicitudRecarga(id, monto, fecha, getCorreoElectronico());
    }

    public long contarNotificacionesNoLeidas() {
        return listaNotificaciones.stream().filter(n -> !n.estaLeida()).count();
    }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public List<Notificacion> getListaNotificaciones() { return Collections.unmodifiableList(listaNotificaciones); }
    public List<Compra> getListaCompras() { return Collections.unmodifiableList(listaCompras); }
}
