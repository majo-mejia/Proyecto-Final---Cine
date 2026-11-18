package co.edu.uniquindio.cinemauq.patrones.creacionales.factorymethod;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.model.Usuario;

/**
 * Patrón FACTORY METHOD.
 * Problema: crear un cliente implica más que un "new" (tarjeta virtual, cuenta de puntos,
 * fecha de registro) y crear un administrador no. Cada fábrica concreta decide qué Usuario
 * construye en crearUsuario(); quien registra usuarios solo depende de esta clase abstracta.
 */
public abstract class UsuarioFactory {

    public abstract Usuario crearUsuario(String nombre, String documento, String correoElectronico, String contrasena)
            throws DatosInvalidosException;

    /** Validaciones comunes a cualquier usuario. */
    protected void validarDatos(String nombre, String documento, String correoElectronico, String contrasena)
            throws DatosInvalidosException {
        if (esVacio(nombre) || esVacio(documento) || esVacio(correoElectronico) || esVacio(contrasena)) {
            throw new DatosInvalidosException("Nombre, documento, correo y contraseña son obligatorios.");
        }
        if (!correoElectronico.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new DatosInvalidosException("El correo electrónico no es válido.");
        }
        if (contrasena.length() < 4) {
            throw new DatosInvalidosException("La contraseña debe tener al menos 4 caracteres.");
        }
    }

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
