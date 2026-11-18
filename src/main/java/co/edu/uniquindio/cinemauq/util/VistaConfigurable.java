package co.edu.uniquindio.cinemauq.util;

import javafx.scene.Parent;

/**
 * Los controladores que necesiten agregar o conectar nodos del FXML que no tiene fx:id (las
 * vistas del equipo no se modifican) implementan esta interfaz: Navigator la llama al cargar.
 */
public interface VistaConfigurable {

    void alCargar(Parent raiz);
}
