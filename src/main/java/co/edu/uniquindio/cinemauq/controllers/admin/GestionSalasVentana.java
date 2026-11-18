package co.edu.uniquindio.cinemauq.controllers.admin;

import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.controllers.comun.Tablas;
import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.model.Sala;
import co.edu.uniquindio.cinemauq.model.enums.TipoSala;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracion;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import static co.edu.uniquindio.cinemauq.controllers.comun.Tablas.columna;

/**
 * Salas del cine: crear con su cuadrícula de asientos (todos estándar, mismo precio) y editar
 * nombre y tipo de proyección. Los asientos no se editan una vez creada la sala, porque las
 * funciones ya programadas guardan su propio mapa de asientos.
 */
public class GestionSalasVentana {

    private final ServicioAdministracion servicio;
    private final TableView<Sala> tabla = Tablas.tabla(
            columna("Id", Sala::getId),
            columna("Nombre", Sala::getNombre),
            columna("Proyección", s -> s.getTipo() == TipoSala.SALA_3D ? "3D" : "2D"),
            columna("Asientos", s -> String.valueOf(s.obtenerCapacidad())),
            columna("Funciones", s -> String.valueOf(s.getListaFunciones().size())));

    public GestionSalasVentana(ServicioAdministracion servicio) {
        this.servicio = servicio;
    }

    public void mostrar() {
        recargar();
        Tablas.mostrarVentana("Salas", Tablas.panel(tabla,
                Tablas.boton("Agregar sala", () -> { agregar(); recargar(); }),
                Tablas.botonConSeleccion("Editar nombre y tipo", tabla, "una sala", this::editar, this::recargar)));
    }

    private void recargar() {
        Tablas.cargar(tabla, servicio::consultarSalas);
    }

    private void agregar() {
        TextField nombre = new TextField();
        ComboBox<TipoSala> tipo = new ComboBox<>(FXCollections.observableArrayList(TipoSala.values()));
        tipo.getSelectionModel().selectFirst();
        Spinner<Integer> filas = new Spinner<>(1, 26, 8);
        Spinner<Integer> columnas = new Spinner<>(1, 30, 12);
        Spinner<Integer> accesibles = new Spinner<>(0, 26, 1);
        for (Spinner<Integer> s : java.util.List.of(filas, columnas, accesibles)) {
            s.setEditable(true);
        }
        if (!Tablas.pedir("Agregar sala", Tablas.formulario(
                new Label("Nombre"), nombre, new Label("Proyección"), tipo,
                new Label("Filas (A, B, C...)"), filas, new Label("Asientos por fila"), columnas,
                new Label("Filas accesibles (primeras)"), accesibles))) {
            return;
        }
        try {
            String id = Tablas.siguienteId("S", servicio.consultarSalas().stream().map(Sala::getId).toList());
            Sala sala = new Sala(id, nombre.getText().trim(), tipo.getValue());
            sala.crearAsientos(filas.getValue(), columnas.getValue(), Math.min(accesibles.getValue(), filas.getValue()));
            servicio.agregarSala(sala);
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo crear la sala", e.getMessage());
        }
    }

    private void editar(Sala sala) {
        TextField nombre = new TextField(sala.getNombre());
        ComboBox<TipoSala> tipo = new ComboBox<>(FXCollections.observableArrayList(TipoSala.values()));
        tipo.getSelectionModel().select(sala.getTipo());
        if (Tablas.pedir("Editar " + sala.getNombre(), Tablas.formulario(
                new Label("Nombre"), nombre, new Label("Proyección"), tipo))) {
            Tablas.ejecutar("No se pudo editar la sala", () -> servicio.actualizarSala(sala.getId(), nombre.getText(), tipo.getValue()));
        }
    }
}
