package co.edu.uniquindio.cinemauq.patrones.estructurales.composite;

import co.edu.uniquindio.cinemauq.exceptions.StockInsuficienteException;

/**
 * Patrón COMPOSITE (componente).
 * Problema: un combo agrupa productos (y podría agrupar otros combos), pero la compra, el stock
 * y los puntos deben tratar igual a un producto suelto y a un combo. Producto es la hoja y
 * Combo el compuesto; el resto del sistema solo conoce esta interfaz.
 */
public interface ItemVendible {

    String obtenerId();

    String obtenerNombre();

    double obtenerPrecio();

    /** Puntos necesarios para pagar una unidad con puntos (0 = no se puede pagar con puntos). */
    int obtenerCostoEnPuntos();

    boolean hayStock(int cantidad);

    void descontarStock(int cantidad) throws StockInsuficienteException;

    void devolverStock(int cantidad);
}
