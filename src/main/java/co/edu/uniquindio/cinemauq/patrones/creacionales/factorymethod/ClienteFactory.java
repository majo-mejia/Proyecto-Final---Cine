package co.edu.uniquindio.cinemauq.patrones.creacionales.factorymethod;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.model.Cliente;
import co.edu.uniquindio.cinemauq.model.TarjetaVirtual;
import co.edu.uniquindio.cinemauq.model.Usuario;

import java.time.LocalDate;

/** Patrón FACTORY METHOD (creador concreto): crea clientes con su tarjeta en $0. */
public class ClienteFactory extends UsuarioFactory {

    @Override
    public Usuario crearUsuario(String nombre, String documento, String correoElectronico, String contrasena)
            throws DatosInvalidosException {
        return crearCliente(nombre, documento, correoElectronico, contrasena);
    }

    public Cliente crearCliente(String nombre, String documento, String correoElectronico, String contrasena)
            throws DatosInvalidosException {
        validarDatos(nombre, documento, correoElectronico, contrasena);
        return new Cliente(nombre, documento, correoElectronico, contrasena, LocalDate.now(),
                new TarjetaVirtual("TV-" + documento, 0));
    }
}
