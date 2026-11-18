package co.edu.uniquindio.cinemauq.patrones.comportamiento.state;

import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.model.Compra;
import co.edu.uniquindio.cinemauq.model.enums.MotivoCancelacion;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy.PoliticaReembolso;

import java.time.LocalDateTime;

/**
 * Patrón STATE.
 * Problema: lo que se puede hacer con una compra depende de su estado: una pendiente se paga,
 * una pagada se cancela con reembolso y una cancelada ya no admite nada. Cada estado decide
 * qué permite y a cuál pasa la compra; así es imposible pagar dos veces o reembolsar dos veces.
 */
public interface EstadoCompra {

    void pagar(Compra compra) throws CinemaUQException;

    void cancelar(Compra compra, PoliticaReembolso politica, MotivoCancelacion motivo, LocalDateTime ahora)
            throws CinemaUQException;

    String getNombre();
}
