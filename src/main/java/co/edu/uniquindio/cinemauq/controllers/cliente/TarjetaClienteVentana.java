package co.edu.uniquindio.cinemauq.controllers.cliente;

import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.controllers.comun.Tablas;
import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.model.Compra;
import co.edu.uniquindio.cinemauq.model.MovimientoTarjeta;
import co.edu.uniquindio.cinemauq.model.Notificacion;
import co.edu.uniquindio.cinemauq.model.Reembolso;
import co.edu.uniquindio.cinemauq.model.Reporte;
import co.edu.uniquindio.cinemauq.model.SolicitudRecarga;
import co.edu.uniquindio.cinemauq.model.enums.MetodoPago;
import co.edu.uniquindio.cinemauq.patrones.estructurales.facade.CineFacade;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static co.edu.uniquindio.cinemauq.controllers.comun.Tablas.columna;

/**
 * "Mi tarjeta" del cliente: saldo y puntos, solicitud de recarga, movimientos, historial de
 * compras (con cancelación según la política de reembolso) y notificaciones. Todo por CineFacade.
 */
public class TarjetaClienteVentana {

    private final CineFacade fachada;
    private final Label lblSaldo = new Label();
    private final Label lblPuntos = new Label();
    private final TableView<MovimientoTarjeta> movimientos = Tablas.tabla(
            columna("Fecha", m -> m.getFechaHora().format(Reporte.FORMATO_FECHA_HORA)),
            columna("Tipo", m -> m.getTipo().name()),
            columna("Monto", m -> Reporte.dinero(m.getMonto())),
            columna("Saldo", m -> Reporte.dinero(m.getSaldoResultante())),
            columna("Descripción", MovimientoTarjeta::getDescripcion));
    private final TableView<Compra> compras = Tablas.tabla(
            columna("Compra", Compra::getId),
            columna("Película", c -> c.getFuncion().getPelicula().getTitulo()),
            columna("Función", c -> c.getFuncion().getFechaHora().format(Reporte.FORMATO_FECHA_HORA)),
            columna("Asientos", c -> String.join(", ", c.getListaEntradas().stream()
                    .map(e -> e.getAsiento().obtenerIdentificador()).toList())),
            columna("Total", c -> c.getMetodo() == MetodoPago.PUNTOS ? c.getPuntosUtilizados() + " pts" : Reporte.dinero(c.getTotal())),
            columna("Estado", Compra::getNombreEstado));
    private final TableView<Notificacion> notificaciones = Tablas.tabla(
            columna("Fecha", n -> n.getFechaHora().format(Reporte.FORMATO_FECHA_HORA)),
            columna("Mensaje", Notificacion::getMensaje),
            columna("Estado", n -> n.estaLeida() ? "Leída" : "Nueva"));

    public TarjetaClienteVentana(CineFacade fachada) {
        this.fachada = fachada;
    }

    public void mostrar() {
        if (!recargar()) {
            return;
        }
        TextField monto = new TextField("50000");
        monto.setPrefColumnCount(10);
        lblSaldo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        VBox resumen = new VBox(14, lblSaldo, lblPuntos,
                new Label("Solo el administrador recarga la tarjeta: aquí puede enviarle una solicitud."),
                new HBox(10, new Label("Monto a solicitar"), monto,
                        Tablas.boton("Solicitar recarga", () -> solicitarRecarga(monto.getText()))));
        resumen.setPadding(new Insets(16));

        TabPane pestanas = new TabPane(
                new Tab("Saldo y puntos", resumen),
                new Tab("Movimientos", Tablas.panel(movimientos)),
                new Tab("Mis compras", Tablas.panel(compras, Tablas.botonConSeleccion("Cancelar compra seleccionada", compras, "una compra", this::cancelar, this::recargar))),
                new Tab("Notificaciones", Tablas.panel(notificaciones)));
        pestanas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tablas.mostrarVentana("Mi tarjeta CinemaUQ", pestanas);
        notificaciones.getItems().forEach(Notificacion::marcarLeida);
    }

    private boolean recargar() {
        try {
            lblSaldo.setText("Saldo disponible: " + Reporte.dinero(fachada.consultarSaldo()));
            lblPuntos.setText("Puntos CinemaUQ vigentes: " + fachada.consultarPuntos());
        } catch (CinemaUQException e) {
            Dialogos.error("Sesión no válida", e.getMessage());
            return false;
        }
        Tablas.cargar(movimientos, () -> fachada.consultarMovimientos().reversed());
        Tablas.cargar(compras, () -> fachada.consultarHistorialCompras().reversed());
        Tablas.cargar(notificaciones, () -> fachada.consultarNotificaciones().reversed());
        return true;
    }

    private void solicitarRecarga(String texto) {
        try {
            SolicitudRecarga solicitud = fachada.solicitarRecarga(Double.parseDouble(texto.trim()));
            Dialogos.info("Solicitud enviada", "La solicitud " + solicitud.getId() + " por "
                    + Reporte.dinero(solicitud.getMontoSolicitado()) + " quedó pendiente de aprobación.");
        } catch (NumberFormatException e) {
            Dialogos.error("Monto inválido", "Escriba solo números.");
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo solicitar la recarga", e.getMessage());
        }
    }

    /** Regla 4: el porcentaje lo decide la política según las horas que faltan para la función. */
    private void cancelar(Compra compra) {
        if (!Dialogos.confirmar("Cancelar compra " + compra.getId(), "Reembolso según el tiempo que falta:\n"
                + "· más de 24 h: 100 %\n· entre 2 y 24 h: 80 %\n· menos de 2 h: no se permite\n\n"
                + "Los puntos que ganó esta compra se anulan.")) {
            return;
        }
        if (Tablas.ejecutar("No se pudo cancelar", () -> fachada.cancelarCompra(compra))) {
            Reembolso r = compra.getReembolso();
            String devuelto = compra.getMetodo() == MetodoPago.PUNTOS
                    ? Math.round(compra.getPuntosUtilizados() * r.getPorcentaje()) + " puntos"
                    : Reporte.dinero(r.getMonto());
            Dialogos.info("Compra cancelada", "Se devolvió el " + Math.round(r.getPorcentaje() * 100) + " %: " + devuelto);
        }
    }
}
