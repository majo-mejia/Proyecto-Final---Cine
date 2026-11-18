package co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy;

/** Patrón STRATEGY: devuelve un porcentaje parcial (80 % entre 2 y 24 h antes). */
public class ReembolsoParcial implements PoliticaReembolso {

    private final double porcentaje;

    public ReembolsoParcial(double porcentaje) {
        this.porcentaje = porcentaje;
    }

    @Override
    public boolean permitirCancelar() { return true; }

    @Override
    public double obtenerPorcentaje() { return porcentaje; }
}
