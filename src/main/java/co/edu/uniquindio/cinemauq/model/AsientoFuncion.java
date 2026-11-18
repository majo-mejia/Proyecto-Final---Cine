package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.AsientoNoDisponibleException;

/** Estado de un asiento en una función concreta. Garantiza que no se venda dos veces (regla 1). */
public class AsientoFuncion {

    private final Asiento asiento;
    private boolean ocupado;

    public AsientoFuncion(Asiento asiento) {
        this.asiento = asiento;
    }

    public boolean estaDisponible() { return !ocupado; }

    public void marcarOcupado() throws AsientoNoDisponibleException {
        if (ocupado) {
            throw new AsientoNoDisponibleException("El asiento " + asiento.obtenerIdentificador() + " ya está ocupado.");
        }
        ocupado = true;
    }

    public void liberar() { ocupado = false; }

    public Asiento getAsiento() { return asiento; }
    public boolean getOcupado() { return ocupado; }
    public void setOcupado(boolean ocupado) { this.ocupado = ocupado; }
}
