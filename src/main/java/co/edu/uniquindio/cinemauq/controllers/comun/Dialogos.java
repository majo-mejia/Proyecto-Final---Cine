package co.edu.uniquindio.cinemauq.controllers.comun;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

/** Alertas y diálogos que comparten todas las vistas. */
public final class Dialogos {

    private Dialogos() { }

    public static void info(String titulo, String mensaje) {
        crear(Alert.AlertType.INFORMATION, titulo, mensaje).showAndWait();
    }

    public static void error(String titulo, String mensaje) {
        crear(Alert.AlertType.ERROR, titulo, mensaje).showAndWait();
    }

    public static void advertencia(String titulo, String mensaje) {
        crear(Alert.AlertType.WARNING, titulo, mensaje).showAndWait();
    }

    public static boolean confirmar(String titulo, String mensaje) {
        return crear(Alert.AlertType.CONFIRMATION, titulo, mensaje).showAndWait()
                .filter(b -> b == ButtonType.OK).isPresent();
    }

    /** Pide correo y contraseña; vienen sugeridos los del usuario de demostración. */
    public static Optional<String[]> pedirCredenciales(String titulo, String correoSugerido, String contrasenaSugerida) {
        Dialog<String[]> dialogo = conDueno(new Dialog<>());
        dialogo.setTitle("CinemaUQ");
        dialogo.setHeaderText(titulo);
        ButtonType ingresar = new ButtonType("Ingresar", ButtonType.OK.getButtonData());
        dialogo.getDialogPane().getButtonTypes().addAll(ingresar, ButtonType.CANCEL);

        TextField correo = new TextField(correoSugerido);
        correo.setPrefColumnCount(22);
        PasswordField contrasena = new PasswordField();
        contrasena.setText(contrasenaSugerida);
        GridPane formulario = new GridPane();
        formulario.setHgap(10);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(10));
        formulario.addRow(0, new Label("Correo"), correo);
        formulario.addRow(1, new Label("Contraseña"), contrasena);
        dialogo.getDialogPane().setContent(formulario);
        dialogo.setResultConverter(b -> b == ingresar ? new String[]{correo.getText().trim(), contrasena.getText()} : null);
        return dialogo.showAndWait();
    }

    /**
     * Le da al diálogo como dueña la ventana que tiene el foco. Sin dueña, en Windows un diálogo
     * abierto desde otra ventana modal puede quedar detrás de ella y bloquear la aplicación.
     */
    public static <D extends Dialog<?>> D conDueno(D dialogo) {
        Window.getWindows().stream()
                .filter(w -> w.isShowing() && w.isFocused())
                .findFirst()
                .or(() -> Window.getWindows().stream().filter(Window::isShowing).reduce((a, b) -> b))
                .ifPresent(dialogo::initOwner);
        return dialogo;
    }

    private static Alert crear(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = conDueno(new Alert(tipo));
        alerta.setTitle("CinemaUQ");
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje);
        alerta.getDialogPane().setMinWidth(420);
        return alerta;
    }
}
