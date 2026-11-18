package co.edu.uniquindio.cinemauq.controllers.admin;

import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.controllers.comun.Tablas;
import co.edu.uniquindio.cinemauq.model.Cliente;
import co.edu.uniquindio.cinemauq.model.Reporte;
import co.edu.uniquindio.cinemauq.model.SolicitudRecarga;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracion;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;

import java.time.LocalDate;

import static co.edu.uniquindio.cinemauq.controllers.comun.Tablas.columna;

/** Clientes y recargas: recargar, bloquear y activar tarjetas; aprobar o rechazar solicitudes. */
public class GestionClientesVentana {

    private final ServicioAdministracion servicio;
    private final TableView<Cliente> clientes = Tablas.tabla(
            columna("Nombre", Cliente::getNombre),
            columna("Documento", Cliente::getDocumento),
            columna("Correo", Cliente::getCorreoElectronico),
            columna("Saldo", c -> Reporte.dinero(c.obtenerTarjeta().getSaldo())),
            columna("Tarjeta", c -> c.obtenerTarjeta().getEstado().name()),
            columna("Puntos", c -> String.valueOf(c.obtenerCuentaPuntos().obtenerPuntosDisponibles(LocalDate.now()))));
    private final TableView<SolicitudRecarga> solicitudes = Tablas.tabla(
            columna("Solicitud", SolicitudRecarga::getId),
            columna("Correo", SolicitudRecarga::getCorreoElectronico),
            columna("Monto", s -> Reporte.dinero(s.getMontoSolicitado())),
            columna("Fecha", s -> s.getFechaSolicitud().format(Reporte.FORMATO_FECHA_HORA)),
            columna("Estado", s -> s.getEstado().name()));

    public GestionClientesVentana(ServicioAdministracion servicio) {
        this.servicio = servicio;
    }

    public void mostrar() {
        recargar();
        TabPane pestanas = new TabPane(
                new Tab("Clientes", Tablas.panel(clientes,
                        Tablas.botonConSeleccion("Recargar tarjeta", clientes, "un cliente", this::recargarTarjeta, this::recargar),
                        Tablas.botonConSeleccion("Bloquear tarjeta", clientes, "un cliente",
                                c -> Tablas.ejecutar("No se pudo bloquear", () -> servicio.bloquearTarjeta(c.getDocumento())), this::recargar),
                        Tablas.botonConSeleccion("Activar tarjeta", clientes, "un cliente",
                                c -> Tablas.ejecutar("No se pudo activar", () -> servicio.activarTarjeta(c.getDocumento())), this::recargar))),
                new Tab("Solicitudes de recarga", Tablas.panel(solicitudes,
                        Tablas.botonConSeleccion("Aprobar solicitud", solicitudes, "una solicitud",
                                s -> Tablas.ejecutar("No se pudo aprobar", () -> servicio.aprobarSolicitudRecarga(s.getId())), this::recargar),
                        Tablas.botonConSeleccion("Rechazar solicitud", solicitudes, "una solicitud",
                                s -> Tablas.ejecutar("No se pudo rechazar", () -> servicio.rechazarSolicitudRecarga(s.getId())), this::recargar))));
        pestanas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tablas.mostrarVentana("Clientes y recargas", pestanas);
    }

    private void recargar() {
        Tablas.cargar(clientes, servicio::consultarClientes);
        Tablas.cargar(solicitudes, servicio::consultarSolicitudesRecarga);
    }

    private void recargarTarjeta(Cliente cliente) {
        TextInputDialog monto = Dialogos.conDueno(new TextInputDialog("50000"));
        monto.setTitle("CinemaUQ");
        monto.setHeaderText("Recargar la tarjeta de " + cliente.getNombre());
        monto.setContentText("Monto:");
        monto.showAndWait().ifPresent(valor -> {
            try {
                double cantidad = Double.parseDouble(valor.trim());
                Tablas.ejecutar("No se pudo recargar", () -> servicio.recargarTarjeta(cliente.getDocumento(), cantidad));
            } catch (NumberFormatException e) {
                Dialogos.error("Monto inválido", "Escriba solo números.");
            }
        });
    }
}
