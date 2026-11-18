package co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy;

import co.edu.uniquindio.cinemauq.model.Funcion;
import co.edu.uniquindio.cinemauq.model.enums.MotivoCancelacion;
import co.edu.uniquindio.cinemauq.patrones.creacionales.singleton.ConfiguracionCine;

import java.time.LocalDateTime;

/**
 * Patrón STRATEGY.
 * Problema: cuánto se devuelve al cancelar depende del tiempo que falta para la función y de
 * quién cancela (regla 4). Cada regla es una estrategia intercambiable; la compra solo pregunta
 * "¿se puede cancelar?" y "¿cuánto devuelvo?". Agregar una regla nueva no obliga a tocar Compra (OCP).
 */
public interface PoliticaReembolso {

    boolean permitirCancelar();

    /** Fracción del total que se devuelve: 1.0 = 100 %. */
    double obtenerPorcentaje();

    default double calcularMonto(double total) {
        return permitirCancelar() ? total * obtenerPorcentaje() : 0;
    }

    /**
     * Elige la estrategia según la regla 4:
     * función cancelada por el cine → 100 %; más de 24 h → 100 %; entre 2 y 24 h → 80 %; menos de 2 h → no se permite.
     */
    static PoliticaReembolso seleccionar(Funcion funcion, MotivoCancelacion motivo, LocalDateTime ahora) {
        if (motivo == MotivoCancelacion.FUNCION_CANCELADA) {
            return new ReembolsoTotal();
        }
        ConfiguracionCine config = ConfiguracionCine.getInstancia();
        long minutos = funcion.minutosParaInicio(ahora);
        if (minutos > config.getHorasReembolsoTotal() * 60L) {
            return new ReembolsoTotal();
        }
        if (minutos >= config.getHorasMinimasCancelacion() * 60L) {
            return new ReembolsoParcial(config.getPorcentajeReembolsoParcial());
        }
        return new SinReembolso();
    }
}
