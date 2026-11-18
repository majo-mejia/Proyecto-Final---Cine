package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.exceptions.TarjetaInactivaException;
import co.edu.uniquindio.cinemauq.model.enums.TipoMovimiento;
import co.edu.uniquindio.cinemauq.model.enums.TipoUsuario;

import java.time.LocalDateTime;

public class Administrador extends Usuario {

    public Administrador(String nombre, String documento, String correoElectronico, String contrasena) {
        super(nombre, documento, correoElectronico, contrasena, TipoUsuario.ADMINISTRADOR);
    }

    public MovimientoTarjeta recargarTarjetaVirtual(TarjetaVirtual tarjeta, double monto, LocalDateTime fechaHora)
            throws TarjetaInactivaException, DatosInvalidosException {
        return tarjeta.acreditar(monto, TipoMovimiento.RECARGA, "Recarga realizada por " + getNombre(), fechaHora);
    }
}
