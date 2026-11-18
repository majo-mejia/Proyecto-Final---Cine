package co.edu.uniquindio.cinemauq.util;

import co.edu.uniquindio.cinemauq.model.Cine;
import co.edu.uniquindio.cinemauq.patrones.estructurales.facade.CineFacade;
import javafx.application.Application;
import javafx.stage.Stage;

import java.time.Clock;


public class CinemaUQApp extends Application {

    private static CineFacade fachada;


    public static CineFacade obtenerFachada() {
        return fachada;
    }

    @Override
    public void init() throws Exception {
        Clock reloj = Clock.systemDefaultZone();
        Cine cine = DatosIniciales.crearCine(reloj);
        fachada = new CineFacade(cine, reloj);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("CinemaUQ");
        TemaDialogos.instalar();
        Navigator.mostrar(stage, Navigator.LOGIN);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
