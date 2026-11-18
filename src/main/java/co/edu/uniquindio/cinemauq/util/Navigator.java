package co.edu.uniquindio.cinemauq.util;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Cambia de vista: carga el FXML y devuelve su controlador para que la vista anterior le pase los
 * datos que necesite (por ejemplo, la función elegida). Cada FXML trae su hoja de estilos
 * (DarkTema.css); si alguno no la tiene, se le agrega para que todas se vean igual.
 */
public final class Navigator {

    private static final String BASE = "/co/edu/uniquindio/cinemauq/view/";
    public static final String LOGIN = BASE + "vistas/Login.fxml";
    public static final String CARTELERA = BASE + "vistas/Cartelera.fxml";
    public static final String ASIENTOS = BASE + "vistas/Asientos.fxml";
    public static final String DASHBOARD = BASE + "vistas/Dashboard.fxml";
    private static final String TEMA = BASE + "styles/DarkTema.css";

    private static final double ANCHO = 1100;
    private static final double ALTO = 780;

    private Navigator() { }

    /** Navega desde el botón que disparó el evento y devuelve el controlador de la nueva vista. */
    public static <T> T navigate(ActionEvent event, String fxmlPath) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        return mostrar(stage, fxmlPath);
    }

    public static <T> T mostrar(Stage stage, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(recurso(fxmlPath));
            Parent root = loader.load();
            if (root.getStylesheets().isEmpty()) {
                root.getStylesheets().add(recurso(TEMA).toExternalForm());
            }
            Object controlador = loader.getController();
            if (controlador instanceof VistaConfigurable configurable) {
                configurable.alCargar(root);
            }
            stage.setScene(new Scene(root, ANCHO, ALTO));
            stage.show();
            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alerta = new Alert(Alert.AlertType.ERROR, "No se pudo abrir la vista " + fxmlPath + ":\n" + e.getMessage());
            alerta.setHeaderText("Error de navegación");
            alerta.showAndWait();
            return null;
        }
    }

    private static URL recurso(String ruta) {
        URL url = Navigator.class.getResource(ruta);
        if (url == null) {
            throw new IllegalStateException("No existe el recurso " + ruta);
        }
        return url;
    }
}
