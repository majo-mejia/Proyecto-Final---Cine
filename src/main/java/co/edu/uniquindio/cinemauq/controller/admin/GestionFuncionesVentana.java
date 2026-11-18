package co.edu.uniquindio.cinemauq.controllers.admin;

import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.controllers.comun.Tablas;
import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.model.Funcion;
import co.edu.uniquindio.cinemauq.model.Pelicula;
import co.edu.uniquindio.cinemauq.model.Reporte;
import co.edu.uniquindio.cinemauq.model.Sala;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracion;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static co.edu.uniquindio.cinemauq.controllers.comun.Tablas.columna;

/** Funciones del cine: listar, programar y cancelar (el cine reembolsa el 100 % y notifica). */
public class GestionFuncionesVentana {

    private final ServicioAdministracion servicio;
    private final TableView<Funcion> tabla = Tablas.tabla(
            columna("Película", f -> f.getPelicula().getTitulo()),
            columna("Sala", f -> f.getSala().getNombre()),
            columna("Fecha y hora", f -> f.getFechaHora().format(Reporte.FORMATO_FECHA_HORA)),
            columna("Precio", f -> Reporte.dinero(f.getPrecioBase())),
            columna("Ocupación", f -> f.contarOcupados() + "/" + f.getSala().obtenerCapacidad()),
            columna("Estado", f -> f.estaProgramada() ? "Programada" : "Cancelada"));

    public GestionFuncionesVentana(ServicioAdministracion servicio) {
        this.servicio = servicio;
    }

    public void mostrar() {
        recargar();
        Tablas.mostrarVentana("Funciones", Tablas.panel(tabla,
                Tablas.boton("Programar función", () -> { programar(); recargar(); }),
                Tablas.botonConSeleccion("Cancelar función seleccionada", tabla, "una función", this::cancelar, this::recargar)));
    }

    private void recargar() {
        Tablas.cargar(tabla, servicio::consultarFunciones);
    }

    private void cancelar(Funcion f) {
        if (Dialogos.confirmar("Cancelar función", "Se cancelará " + f.getPelicula().getTitulo() + " del "
                + f.getFechaHora().format(Reporte.FORMATO_FECHA_HORA) + ".\nLas compras pagadas se reembolsan "
                + "al 100 % y los clientes reciben una notificación.")) {
            Tablas.ejecutar("No se pudo cancelar la función", () -> servicio.cancelarFuncion(f.getId()));
        }
    }

    private void programar() {
        try {
            ComboBox<Pelicula> pelicula = new ComboBox<>(FXCollections.observableArrayList(servicio.consultarPeliculas()));
            ComboBox<Sala> sala = new ComboBox<>(FXCollections.observableArrayList(servicio.consultarSalas()));
            DatePicker fecha = new DatePicker(LocalDate.now().plusDays(1));
            TextField hora = new TextField("18:00");
            TextField precio = new TextField("15000");
            pelicula.getSelectionModel().selectFirst();
            sala.getSelectionModel().selectFirst();

            Dialog<ButtonType> dialogo = Dialogos.conDueno(new Dialog<>());
            dialogo.setTitle("CinemaUQ");
            dialogo.setHeaderText("Programar función");
            dialogo.getDialogPane().setContent(Tablas.formulario(
                    new Label("Película"), pelicula, new Label("Sala"), sala, new Label("Fecha"), fecha,
                    new Label("Hora (HH:mm)"), hora, new Label("Precio"), precio));
            dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            if (dialogo.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) {
                return;
            }
            LocalDateTime fechaHora = LocalDateTime.of(fecha.getValue(), LocalTime.parse(hora.getText().trim()));
            Funcion f = servicio.programarFuncion(pelicula.getValue().getId(), sala.getValue().getId(), fechaHora,
                    Double.parseDouble(precio.getText().trim()));
            Dialogos.info("Función programada", f.toString());
        } catch (DateTimeParseException | NumberFormatException e) {
            Dialogos.error("Datos inválidos", "Revise la hora (HH:mm) y el precio (solo números).");
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo programar la función", e.getMessage());
        }
    }
}
