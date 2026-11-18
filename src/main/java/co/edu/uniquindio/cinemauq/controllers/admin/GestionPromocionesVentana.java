package co.edu.uniquindio.cinemauq.controllers.admin;

import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.controllers.comun.Tablas;
import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.model.Promocion;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracion;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDate;

import static co.edu.uniquindio.cinemauq.controllers.comun.Tablas.columna;

/** Promociones (descuento porcentual con vigencia): agregar, editar y eliminar. */
public class GestionPromocionesVentana {

    private final ServicioAdministracion servicio;
    private final TableView<Promocion> tabla = Tablas.tabla(
            columna("Id", Promocion::getId),
            columna("Nombre", Promocion::getNombre),
            columna("Descuento", p -> Math.round(p.getPorcentajeDescuento() * 100) + " %"),
            columna("Desde", p -> p.getFechaInicio().toString()),
            columna("Hasta", p -> p.getFechaFin().toString()),
            columna("Vigente hoy", p -> p.estaVigente(LocalDate.now()) ? "Sí" : "No"));

    public GestionPromocionesVentana(ServicioAdministracion servicio) {
        this.servicio = servicio;
    }

    public void mostrar() {
        recargar();
        Tablas.mostrarVentana("Promociones", Tablas.panel(tabla,
                Tablas.boton("Agregar promoción", () -> { formulario(null); recargar(); }),
                Tablas.botonConSeleccion("Editar", tabla, "una promoción", this::formulario, this::recargar),
                Tablas.botonConSeleccion("Eliminar", tabla, "una promoción", this::eliminar, this::recargar)));
    }

    private void recargar() {
        Tablas.cargar(tabla, servicio::consultarPromociones);
    }

    private void eliminar(Promocion p) {
        if (Dialogos.confirmar("Eliminar promoción", "Se eliminará \"" + p.getNombre() + "\".")) {
            Tablas.ejecutar("No se pudo eliminar la promoción", () -> servicio.eliminarPromocion(p.getId()));
        }
    }

    private void formulario(Promocion existente) {
        TextField nombre = new TextField(existente == null ? "" : existente.getNombre());
        Spinner<Integer> porcentaje = new Spinner<>(1, 100,
                existente == null ? 10 : (int) Math.round(existente.getPorcentajeDescuento() * 100));
        porcentaje.setEditable(true);
        DatePicker inicio = new DatePicker(existente == null ? LocalDate.now() : existente.getFechaInicio());
        DatePicker fin = new DatePicker(existente == null ? LocalDate.now().plusDays(30) : existente.getFechaFin());
        if (!Tablas.pedir(existente == null ? "Agregar promoción" : "Editar promoción", Tablas.formulario(
                new Label("Nombre"), nombre, new Label("Descuento (%)"), porcentaje,
                new Label("Desde"), inicio, new Label("Hasta"), fin))) {
            return;
        }
        try {
            if (nombre.getText().isBlank() || inicio.getValue() == null || fin.getValue() == null) {
                Dialogos.error("Datos incompletos", "Escriba el nombre y elija las dos fechas.");
                return;
            }
            double descuento = porcentaje.getValue() / 100.0;
            if (existente == null) {
                String id = Tablas.siguienteId("PM", servicio.consultarPromociones().stream().map(Promocion::getId).toList());
                servicio.agregarPromocion(new Promocion(id, nombre.getText().trim(), descuento, inicio.getValue(), fin.getValue()));
            } else {
                servicio.actualizarPromocion(existente.getId(), nombre.getText(), descuento, inicio.getValue(), fin.getValue());
            }
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo guardar la promoción", e.getMessage());
        }
    }
}
