package co.edu.uniquindio.cinemauq.patrones.creacionales.factorymethod;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.model.Administrador;
import co.edu.uniquindio.cinemauq.model.Usuario;

/** Patrón FACTORY METHOD (creador concreto): crea administradores. */
public class AdministradorFactory extends UsuarioFactory {

    @Override
    public Usuario crearUsuario(String nombre, String documento, String correoElectronico, String contrasena)
            throws DatosInvalidosException {
        return crearAdministrador(nombre, documento, correoElectronico, contrasena);
    }

    public Administrador crearAdministrador(String nombre, String documento, String correoElectronico, String contrasena)
            throws DatosInvalidosException {
        validarDatos(nombre, documento, correoElectronico, contrasena);
        return new Administrador(nombre, documento, correoElectronico, contrasena);
    }
}
