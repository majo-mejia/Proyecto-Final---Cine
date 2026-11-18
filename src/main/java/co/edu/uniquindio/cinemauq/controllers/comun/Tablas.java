package co.edu.uniquindio.cinemauq.controllers.comun;

import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/** Piezas comunes de las ventanas con tablas (columnas, carga de datos, botones y formularios). */
public final class Tablas {

    private Tablas() { }

    /** Operación de un servicio que puede lanzar una excepción de dominio. */
    public interface Operacion {
        void ejecutar() throws CinemaUQException;
    }

    /** Consulta de un servicio que puede lanzar una excepción de dominio. */
    public interface Consulta<T> {
        List<T> consultar() throws CinemaUQException;
    }

    public static <T> TableColumn<T, String> columna(String titulo, Function<T, String> valor) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(d -> new ReadOnlyStringWrapper(valor.apply(d.getValue())));
        return columna;
    }

    @SafeVarargs
    public static <T> TableView<T> tabla(TableColumn<T, String>... columnas) {
        TableView<T> tabla = new TableView<>();
        tabla.getColumns().addAll(columnas);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return tabla;
    }

    /**
     * Carga (o recarga) la tabla. Como las filas son los mismos objetos del modelo, JavaFX no
     * repinta las celdas por sí solo cuando cambian sus datos (p. ej. el saldo tras una recarga):
     * por eso se llama a refresh(). Se conserva la fila que estaba seleccionada.
     */
    public static <T> void cargar(TableView<T> tabla, Consulta<T> consulta) {
        try {
            T seleccionado = tabla.getSelectionModel().getSelectedItem();
            tabla.setItems(FXCollections.observableArrayList(consulta.consultar()));
            tabla.refresh();
            if (seleccionado != null && tabla.getItems().contains(seleccionado)) {
                tabla.getSelectionModel().select(seleccionado);
            }
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo consultar", e.getMessage());
        }
    }

    /** Ejecuta la operación y muestra el mensaje de la excepción de dominio si falla. */
    public static boolean ejecutar(String tituloError, Operacion operacion) {
        try {
            operacion.ejecutar();
            return true;
        } catch (CinemaUQException e) {
            Dialogos.error(tituloError, e.getMessage());
            return false;
        }
    }

    /** Aplica la acción a la fila seleccionada (o avisa si no hay) y luego recarga. */
    public static <T> void conSeleccion(TableView<T> tabla, String nombre, Consumer<T> accion, Runnable recargar) {
        Optional<T> seleccion = Optional.ofNullable(tabla.getSelectionModel().getSelectedItem());
        if (seleccion.isEmpty()) {
            Dialogos.advertencia("Sin selección", "Seleccione " + nombre + " de la tabla.");
            return;
        }
        accion.accept(seleccion.get());
        recargar.run();
    }

    /** Botón que solo se habilita cuando hay una fila seleccionada en la tabla. */
    public static <T> Button botonConSeleccion(String texto, TableView<T> tabla, String nombre,
                                               Consumer<T> accion, Runnable recargar) {
        Button boton = boton(texto, () -> conSeleccion(tabla, nombre, accion, recargar));
        boton.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        return boton;
    }

    public static VBox panel(Node contenido, Button... botones) {
        VBox panel = new VBox(10, contenido, new HBox(10, botones));
        panel.setPadding(new Insets(10));
        return panel;
    }

    /** Formulario de dos columnas: etiqueta, control, etiqueta, control... */
    public static GridPane formulario(Node... nodos) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        for (int i = 0; i < nodos.length; i += 2) {
            grid.addRow(i / 2, nodos[i], nodos[i + 1]);
        }
        return grid;
    }

    public static Button boton(String texto, Runnable accion) {
        Button boton = new Button(texto);
        boton.setOnAction(e -> accion.run());
        return boton;
    }

    /** Ventana modal con el contenido y un botón Cerrar. */
    public static void mostrarVentana(String titulo, Node contenido) {
        Dialog<Void> dialogo = Dialogos.conDueno(new Dialog<>());
        dialogo.setTitle("CinemaUQ");
        dialogo.setHeaderText(titulo);
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogo.getDialogPane().setPrefSize(920, 540);
        dialogo.setResizable(true);
        dialogo.showAndWait();
    }

    /** Muestra el formulario en un diálogo con Guardar/Cancelar; true si el usuario guardó. */
    public static boolean pedir(String titulo, Node formulario) {
        Dialog<ButtonType> dialogo = Dialogos.conDueno(new Dialog<>());
        dialogo.setTitle("CinemaUQ");
        dialogo.setHeaderText(titulo);
        dialogo.getDialogPane().setContent(formulario);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return dialogo.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    public static int entero(String valor, String campo) throws DatosInvalidosException {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new DatosInvalidosException("\"" + campo + "\" debe ser un número entero.");
        }
    }

    public static double decimal(String valor, String campo) throws DatosInvalidosException {
        try {
            return Double.parseDouble(valor.trim());
        } catch (NumberFormatException e) {
            throw new DatosInvalidosException("\"" + campo + "\" debe ser un número.");
        }
    }

    /** Siguiente identificador libre con el prefijo dado (P1, P2... -> P4). */
    public static String siguienteId(String prefijo, Collection<String> existentes) {
        int mayor = 0;
        for (String id : existentes) {
            if (id.startsWith(prefijo) && id.substring(prefijo.length()).matches("\\d+")) {
                mayor = Math.max(mayor, Integer.parseInt(id.substring(prefijo.length())));
            }
        }
        return prefijo + (mayor + 1);
    }
}
