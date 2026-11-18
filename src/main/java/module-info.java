module co.edu.uniquindio.poo.proyectofinalcine {
    requires javafx.controls;
    requires javafx.fxml;


    opens co.edu.uniquindio.cinemauq to javafx.fxml;
    exports co.edu.uniquindio.cinemauq;
}