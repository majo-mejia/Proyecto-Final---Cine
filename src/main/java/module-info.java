module co.edu.uniquindio.poo.proyectofinalcine {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.github.librepdf.openpdf;

    // Clase principal (Application) accesible para el lanzador de JavaFX
    exports co.edu.uniquindio.cinemauq.util;

    // Controladores cargados por FXMLLoader (acceso reflexivo a @FXML)
    opens co.edu.uniquindio.cinemauq.controllers to javafx.fxml;
    opens co.edu.uniquindio.cinemauq.controllers.admin to javafx.fxml;
    opens co.edu.uniquindio.cinemauq.controllers.cliente to javafx.fxml;
    opens co.edu.uniquindio.cinemauq.controllers.comun to javafx.fxml;
}
