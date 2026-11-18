package co.edu.uniquindio.cinemauq.controllers.comun;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Chart;
import javafx.scene.control.Label;

import java.util.List;

/**
 * Acceso a nodos de los FXML que no tienen fx:id. Las vistas del equipo no se modifican, así que
 * esos nodos (títulos, indicadores, botones) se ubican por su clase CSS.
 */
public final class Vistas {

    private Vistas() { }

    /**
     * Sube por la jerarquía hasta el nodo más alto disponible. Ojo: el contenido de un ScrollPane
     * no está enlazado con el resto de la vista hasta que la ventana se muestra.
     */
    public static Parent raiz(Node nodo) {
        Parent actual = nodo instanceof Parent p ? p : nodo.getParent();
        while (actual.getParent() != null) {
            actual = actual.getParent();
        }
        return actual;
    }

    /**
     * Reemplaza en orden el texto de las etiquetas con esa clase CSS. Ignora las etiquetas internas
     * de los gráficos (que también usan "chart-title").
     */
    public static void ponerTextos(Node contenedor, String selector, String... textos) {
        List<Label> etiquetas = contenedor.lookupAll(selector).stream()
                .filter(n -> n instanceof Label && !(n.getParent() instanceof Chart))
                .map(n -> (Label) n)
                .toList();
        for (int i = 0; i < Math.min(textos.length, etiquetas.size()); i++) {
            etiquetas.get(i).setText(textos[i]);
        }
    }
}
