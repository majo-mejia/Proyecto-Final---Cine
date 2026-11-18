package co.edu.uniquindio.cinemauq.model;

import java.util.Objects;

/**
 * Asiento físico de una sala. Si está ocupado o no se guarda en AsientoFuncion,
 * porque el mismo asiento se vende una vez por cada función.
 */
public class Asiento {

    private final char fila;
    private int numero;
    private boolean accesible;

    public Asiento(char fila, int numero, boolean accesible) {
        this.fila = Character.toUpperCase(fila);
        this.numero = numero;
        this.accesible = accesible;
    }

    /** Asiento para personas con movilidad reducida. */
    public boolean esAccesible() { return accesible; }

    /** Código visible del asiento  */
    public String obtenerIdentificador() { return "" + fila + numero; }

    public char getFila() { return fila; }
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public void setAccesible(boolean accesible) { this.accesible = accesible; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Asiento a && fila == a.fila && numero == a.numero);
    }

    @Override
    public int hashCode() { return Objects.hash(fila, numero); }

    @Override
    public String toString() { return obtenerIdentificador(); }
}
