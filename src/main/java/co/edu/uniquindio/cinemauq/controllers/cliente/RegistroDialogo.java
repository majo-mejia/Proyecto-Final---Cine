package co.edu.uniquindio.cinemauq.controllers.cliente;

import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.controllers.comun.Tablas;
import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.model.Cliente;
import co.edu.uniquindio.cinemauq.patrones.estructurales.facade.CineFacade;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Optional;

/**
 * Registro de un cliente nuevo. Usa CineFacade.registrarCliente (que crea el cliente con
 * ClienteFactory y su tarjeta virtual en $0); devuelve el cliente creado o vacío si se canceló.
 */
public final class RegistroDialogo {

    private RegistroDialogo() { }

    public static Optional<Cliente> mostrar(CineFacade fachada) {
        TextField nombre = new TextField();
        nombre.setPrefColumnCount(22);
        TextField documento = new TextField();
        TextField correo = new TextField();
        PasswordField contrasena = new PasswordField();
        PasswordField confirmacion = new PasswordField();
        while (true) {
            if (!Tablas.pedir("Crear cuenta de cliente", Tablas.formulario(
                    new Label("Nombre completo"), nombre, new Label("Documento"), documento,
                    new Label("Correo"), correo, new Label("Contraseña"), contrasena,
                    new Label("Confirmar contraseña"), confirmacion))) {
                return Optional.empty();
            }
            try {
                if (!contrasena.getText().equals(confirmacion.getText())) {
                    throw new DatosInvalidosException("Las contraseñas no coinciden.");
                }
                Cliente cliente = fachada.registrarCliente(nombre.getText().trim(), documento.getText().trim(),
                        correo.getText().trim(), contrasena.getText());
                Dialogos.info("Cuenta creada", "Ya puede ingresar como cliente con " + cliente.getCorreoElectronico()
                        + ".\nSu tarjeta virtual empieza en $0; solicite una recarga al administrador.");
                return Optional.of(cliente);
            } catch (CinemaUQException e) {
                Dialogos.error("No se pudo crear la cuenta", e.getMessage());
            }
        }
    }
}
