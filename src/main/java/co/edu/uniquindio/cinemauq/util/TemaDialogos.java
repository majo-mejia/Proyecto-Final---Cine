package co.edu.uniquindio.cinemauq.util;

import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

import java.net.URL;

/**
 * Da el tema oscuro (DialogosOscuros.css) a todos los diálogos y ventanas emergentes de la
 * aplicación sin tener que tocar cada uno: se engancha a la lista de ventanas de JavaFX.
 */
public final class TemaDialogos {

    private static final String HOJA = "/co/edu/uniquindio/cinemauq/view/styles/DialogosOscuros.css";
    private static boolean instalado;

    private TemaDialogos() { }

    public static void instalar() {
        if (instalado) {
            return;
        }
        instalado = true;
        Window.getWindows().addListener((ListChangeListener<Window>) cambio -> {
            while (cambio.next()) {
                if (cambio.wasAdded()) {
                    cambio.getAddedSubList().forEach(TemaDialogos::aplicar);
                }
            }
        });
    }

    private static void aplicar(Window ventana) {
        agregarHoja(ventana.getScene());
        ventana.sceneProperty().addListener((obs, anterior, escena) -> agregarHoja(escena));
    }

    private static void agregarHoja(Scene escena) {
        URL url = TemaDialogos.class.getResource(HOJA);
        if (escena != null && url != null && escena.getRoot() instanceof DialogPane panel
                && !panel.getStylesheets().contains(url.toExternalForm())) {
            panel.getStylesheets().add(url.toExternalForm());
        }
    }
}
