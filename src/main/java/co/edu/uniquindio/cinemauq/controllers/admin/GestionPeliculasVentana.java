package co.edu.uniquindio.cinemauq.controllers.admin;

import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.controllers.comun.Tablas;
import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.model.Pelicula;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracion;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;

import static co.edu.uniquindio.cinemauq.controllers.comun.Tablas.columna;

/**
 * Películas del cine: agregar, editar, publicar o retirar de la cartelera y eliminar.
 * Eliminar solo se permite si la película no tiene funciones programadas (regla del modelo).
 */
public class GestionPeliculasVentana {

    private static final List<String> CLASIFICACIONES = List.of("Todo público", "+7", "+12", "+15", "+18");

    private final ServicioAdministracion servicio;
    private final TableView<Pelicula> tabla = Tablas.tabla(
            columna("Id", Pelicula::getId),
            columna("Título", Pelicula::getTitulo),
            columna("Género", Pelicula::getGenero),
            columna("Clasificación", Pelicula::getClasificacion),
            columna("Duración", p -> p.getDuracionMinutos() + " min"),
            columna("Cartelera", p -> p.estaEnCartelera() ? "Publicada" : "Retirada"),
            columna("Funciones", p -> String.valueOf(p.getListaFunciones().size())));

    public GestionPeliculasVentana(ServicioAdministracion servicio) {
        this.servicio = servicio;
    }

    public void mostrar() {
        recargar();
        Tablas.mostrarVentana("Películas", Tablas.panel(tabla,
                Tablas.boton("Agregar película", () -> { agregar(); recargar(); }),
                Tablas.botonConSeleccion("Editar", tabla, "una película", this::editar, this::recargar),
                Tablas.botonConSeleccion("Publicar / retirar de cartelera", tabla, "una película", this::alternarCartelera, this::recargar),
                Tablas.botonConSeleccion("Eliminar", tabla, "una película", this::eliminar, this::recargar)));
    }

    private void recargar() {
        Tablas.cargar(tabla, servicio::consultarPeliculas);
    }

    private void alternarCartelera(Pelicula p) {
        Tablas.ejecutar("No se pudo cambiar la cartelera", () -> servicio.cambiarCartelera(p.getId(), !p.estaEnCartelera()));
    }

    private void eliminar(Pelicula p) {
        if (Dialogos.confirmar("Eliminar película", "Se eliminará \"" + p.getTitulo() + "\" del cine.")) {
            Tablas.ejecutar("No se pudo eliminar la película", () -> servicio.eliminarPelicula(p.getId()));
        }
    }

    private void agregar() {
        formulario("Agregar película", null);
    }

    private void editar(Pelicula p) {
        formulario("Editar película", p);
    }

    /** Un mismo formulario sirve para crear (existente == null) y para editar. */
    private void formulario(String titulo, Pelicula existente) {
        TextField nombre = new TextField(existente == null ? "" : existente.getTitulo());
        nombre.setPrefColumnCount(26);
        TextArea sinopsis = new TextArea(existente == null ? "" : existente.getSinopsis());
        sinopsis.setPrefRowCount(3);
        sinopsis.setWrapText(true);
        TextField duracion = new TextField(existente == null ? "120" : String.valueOf(existente.getDuracionMinutos()));
        TextField genero = new TextField(existente == null ? "" : existente.getGenero());
        ComboBox<String> clasificacion = new ComboBox<>(FXCollections.observableArrayList(CLASIFICACIONES));
        clasificacion.getSelectionModel().select(existente == null ? "+12" : existente.getClasificacion());
        TextField imagen = new TextField(existente == null || existente.getImagenPelicula() == null
                ? "" : existente.getImagenPelicula());
        imagen.setPromptText("Opcional");

        if (!Tablas.pedir(titulo, Tablas.formulario(
                new Label("Título"), nombre, new Label("Sinopsis"), sinopsis,
                new Label("Duración (min)"), duracion, new Label("Género"), genero,
                new Label("Clasificación"), clasificacion, new Label("Póster (ruta o URL)"), imagen))) {
            return;
        }
        try {
            int minutos = Tablas.entero(duracion.getText(), "Duración");
            String clasificacionElegida = clasificacion.getValue() == null ? "+12" : clasificacion.getValue();
            if (existente == null) {
                String id = Tablas.siguienteId("P", servicio.consultarPeliculas().stream().map(Pelicula::getId).toList());
                servicio.agregarPelicula(new Pelicula(id, nombre.getText().trim(), sinopsis.getText().trim(), minutos,
                        genero.getText().trim(), clasificacionElegida, imagen.getText().isBlank() ? null : imagen.getText().trim()));
            } else {
                servicio.actualizarPelicula(existente.getId(), nombre.getText(), sinopsis.getText().trim(), minutos,
                        genero.getText(), clasificacionElegida, imagen.getText());
            }
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo guardar la película", e.getMessage());
        }
    }
}
