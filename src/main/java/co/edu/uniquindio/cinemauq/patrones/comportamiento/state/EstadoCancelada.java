package co.edu.uniquindio.cinemauq.patrones.comportamiento.state;

import co.edu.uniquindio.cinemauq.exceptions.OperacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.exceptions.ReembolsoYaRealizadoException;
import co.edu.uniquindio.cinemauq.model.Compra;
import co.edu.uniquindio.cinemauq.model.enums.MotivoCancelacion;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy.PoliticaReembolso;

import java.time.LocalDateTime;

/** Patrón STATE: estado final. Impide pagar de nuevo y reembolsar dos veces (regla 4). */
public class EstadoCancelada implements EstadoCompra {

    @Override
    public void pagar(Compra compra) throws OperacionNoPermitidaException {
        throw new OperacionNoPermitidaException("La compra " + compra.getId() + " está cancelada.");
    }

    @Override
    public void cancelar(Compra compra, PoliticaReembolso politica, MotivoCancelacion motivo, LocalDateTime ahora)
            throws ReembolsoYaRealizadoException {
        throw new ReembolsoYaRealizadoException(
                "La compra " + compra.getId() + " ya fue cancelada y no puede reembolsarse otra vez.");
    }

    @Override
    public String getNombre() { return "Cancelada"; }
}
