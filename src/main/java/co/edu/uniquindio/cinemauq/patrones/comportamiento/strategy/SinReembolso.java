package co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy;

/** Patrón STRATEGY: faltan menos de 2 horas; no se permite cancelar. */
public class SinReembolso implements PoliticaReembolso {

    @Override
    public boolean permitirCancelar() { return false; }

    @Override
    public double obtenerPorcentaje() { return 0; }
}
