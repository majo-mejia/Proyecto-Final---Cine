package co.edu.uniquindio.cinemauq.controllers;

import co.edu.uniquindio.cinemauq.controllers.cliente.RegistroDialogo;
import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.exceptions.CredencialesInvalidasException;
import co.edu.uniquindio.cinemauq.model.Usuario;
import co.edu.uniquindio.cinemauq.model.enums.TipoUsuario;
import co.edu.uniquindio.cinemauq.patrones.estructurales.facade.CineFacade;
import co.edu.uniquindio.cinemauq.util.CinemaUQApp;
import co.edu.uniquindio.cinemauq.util.DatosIniciales;
import co.edu.uniquindio.cinemauq.util.Navigator;
import co.edu.uniquindio.cinemauq.util.VistaConfigurable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * Inicio de sesión. Cada botón pide correo y contraseña (sugiere el usuario de demostración) y
 * valida las credenciales con CineFacade; el tipo del usuario decide a qué vista entra.
 */
public class LoginController implements VistaConfigurable {

    private final CineFacade fachada = CinemaUQApp.obtenerFachada();

    /** El FXML no trae botón de registro: se agrega debajo de los botones de ingreso. */
    @Override
    public void alCargar(Parent raiz) {
        if (raiz.lookup(".btn-dark-admin") instanceof Button botonAdmin && botonAdmin.getParent() instanceof VBox botones) {
            Button registro = new Button("📝   Crear cuenta de cliente");
            registro.getStyleClass().add("btn-dark-outline");
            registro.setMaxWidth(Double.MAX_VALUE);
            registro.setOnAction(e -> RegistroDialogo.mostrar(fachada));
            botones.getChildren().add(registro);
        }
        if (raiz.lookup(".login-card") instanceof Region tarjeta) {
            tarjeta.setMaxHeight(Region.USE_PREF_SIZE);
        }
    }

    @FXML
    public void loginCliente(ActionEvent event) {
        ingresar(event, "Ingreso de cliente", DatosIniciales.CORREO_CLIENTE, DatosIniciales.CONTRASENA_CLIENTE,
                TipoUsuario.CLIENTE, Navigator.CARTELERA);
    }

    @FXML
    public void loginAdmin(ActionEvent event) {
        ingresar(event, "Ingreso de administrador", DatosIniciales.CORREO_ADMIN, DatosIniciales.CONTRASENA_ADMIN,
                TipoUsuario.ADMINISTRADOR, Navigator.DASHBOARD);
    }

    private void ingresar(ActionEvent event, String titulo, String correo, String contrasena,
                          TipoUsuario tipoEsperado, String vista) {
        Optional<String[]> credenciales = Dialogos.pedirCredenciales(titulo, correo, contrasena);
        if (credenciales.isEmpty()) {
            return;
        }
        try {
            Usuario usuario = fachada.iniciarSesion(credenciales.get()[0], credenciales.get()[1]);
            if (usuario.getTipo() != tipoEsperado) {
                fachada.cerrarSesion();
                Dialogos.advertencia("Perfil equivocado", "La cuenta " + usuario.getCorreoElectronico()
                        + " no es de " + (tipoEsperado == TipoUsuario.CLIENTE ? "cliente" : "administrador") + ".");
                return;
            }
            Navigator.navigate(event, vista);
        } catch (CredencialesInvalidasException e) {
            Dialogos.error("No se pudo iniciar sesión", e.getMessage());
        }
    }
}
