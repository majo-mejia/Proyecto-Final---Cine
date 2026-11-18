package co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy;

/** Patrón STRATEGY: devuelve el 100 % (más de 24 h antes, o función cancelada por el cine). */
public class ReembolsoTotal implements PoliticaReembolso {

    @Override
    public boolean permitirCancelar() { return true; }

    @Override
    public double obtenerPorcentaje() { return 1.0; }
}
