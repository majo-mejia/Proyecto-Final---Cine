package co.edu.uniquindio.cinemauq.patrones.comportamiento.state;

import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.model.Compra;
import co.edu.uniquindio.cinemauq.model.enums.MetodoPago;
import co.edu.uniquindio.cinemauq.model.enums.MotivoCancelacion;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy.PoliticaReembolso;

import java.time.LocalDateTime;

/** Patrón STATE: compra armada y aún sin cobrar. Al pagar pasa a EstadoPagada. */
public class EstadoPendiente implements EstadoCompra {

    @Override
    public void pagar(Compra compra) throws CinemaUQException {
        if (compra.getMetodo() == MetodoPago.PUNTOS) {
            compra.getCliente().obtenerCuentaPuntos()
                    .redimir(compra.getPuntosUtilizados(), compra.getFechaHora().toLocalDate());
        } else {
            compra.getCliente().obtenerTarjeta().debitar(compra.getTotal(), "Pago compra " + compra.getId(),
                    compra.getFechaHora());
        }
        compra.cambiarEstado(new EstadoPagada());
    }

    @Override
    public void cancelar(Compra compra, PoliticaReembolso politica, MotivoCancelacion motivo, LocalDateTime ahora) {
        // No se cobró nada, así que no hay nada que devolver.
        compra.cambiarEstado(new EstadoCancelada());
    }

    @Override
    public String getNombre() { return "Pendiente"; }
}
