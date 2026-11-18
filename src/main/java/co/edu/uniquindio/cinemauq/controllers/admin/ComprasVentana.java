package co.edu.uniquindio.cinemauq.controllers.admin;

import co.edu.uniquindio.cinemauq.controllers.comun.Tablas;
import co.edu.uniquindio.cinemauq.model.Compra;
import co.edu.uniquindio.cinemauq.model.Reporte;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracion;
import javafx.scene.control.TableView;

import static co.edu.uniquindio.cinemauq.controllers.comun.Tablas.columna;

/** Consulta de todas las compras del cine. */
public class ComprasVentana {

    private final ServicioAdministracion servicio;

    public ComprasVentana(ServicioAdministracion servicio) {
        this.servicio = servicio;
    }

    public void mostrar() {
        TableView<Compra> tabla = Tablas.tabla(
                columna("Compra", Compra::getId),
                columna("Cliente", c -> c.getCliente().getNombre()),
                columna("Película", c -> c.getFuncion().getPelicula().getTitulo()),
                columna("Función", c -> c.getFuncion().getFechaHora().format(Reporte.FORMATO_FECHA_HORA)),
                columna("Fecha compra", c -> c.getFechaHora().format(Reporte.FORMATO_FECHA_HORA)),
                columna("Entradas", c -> String.valueOf(c.getListaEntradas().size())),
                columna("Total", c -> Reporte.dinero(c.getTotal())),
                columna("Pago", c -> c.getMetodo().name()),
                columna("Estado", Compra::getNombreEstado));
        Tablas.cargar(tabla, servicio::consultarCompras);
        Tablas.mostrarVentana("Compras", tabla);
    }
}
