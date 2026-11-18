package co.edu.uniquindio.cinemauq.patrones.comportamiento.state;

import co.edu.uniquindio.cinemauq.exceptions.CancelacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.exceptions.OperacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.model.Compra;
import co.edu.uniquindio.cinemauq.model.CuentaPuntos;
import co.edu.uniquindio.cinemauq.model.Reembolso;
import co.edu.uniquindio.cinemauq.model.enums.MetodoPago;
import co.edu.uniquindio.cinemauq.model.enums.MotivoCancelacion;
import co.edu.uniquindio.cinemauq.model.enums.TipoMovimiento;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy.PoliticaReembolso;
import co.edu.uniquindio.cinemauq.patrones.creacionales.singleton.ConfiguracionCine;

import java.time.LocalDateTime;

/**
 * Patrón STATE: compra cobrada. No se puede volver a pagar; al cancelarla devuelve lo que
 * diga la política de reembolso (al medio con que se pagó), anula los puntos que la compra
 * había ganado y pasa a EstadoCancelada.
 */
public class EstadoPagada implements EstadoCompra {

    @Override
    public void pagar(Compra compra) throws OperacionNoPermitidaException {
        throw new OperacionNoPermitidaException("La compra " + compra.getId() + " ya está pagada.");
    }

    @Override
    public void cancelar(Compra compra, PoliticaReembolso politica, MotivoCancelacion motivo, LocalDateTime ahora)
            throws CinemaUQException {
        if (!politica.permitirCancelar()) {
            throw new CancelacionNoPermitidaException("La compra " + compra.getId()
                    + " ya no se puede cancelar: faltan menos de "
                    + ConfiguracionCine.getInstancia().getHorasMinimasCancelacion() + " horas para la función.");
        }
        double monto = politica.calcularMonto(compra.getTotal());
        CuentaPuntos cuenta = compra.getCliente().obtenerCuentaPuntos();
        if (compra.getMetodo() == MetodoPago.PUNTOS) {
            // Se devuelven los puntos gastados (según el porcentaje) como un lote nuevo.
            int puntos = (int) Math.round(compra.getPuntosUtilizados() * politica.obtenerPorcentaje());
            cuenta.acumular(puntos, ahora.toLocalDate(), ConfiguracionCine.getInstancia().getVigenciaPuntosAnios());
        } else {
            if (monto > 0) {
                compra.getCliente().obtenerTarjeta().acreditar(monto, TipoMovimiento.REEMBOLSO,
                        "Reembolso compra " + compra.getId(), ahora);
            }
            // La compra deja de existir, así que se anulan los puntos que había ganado.
            cuenta.anularPuntosDeCompra(compra.getId());
        }
        compra.registrarReembolso(new Reembolso(compra.getId(), ahora, politica.obtenerPorcentaje(), monto, motivo));
        compra.cambiarEstado(new EstadoCancelada());
    }

    @Override
    public String getNombre() { return "Pagada"; }
}
