package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.exceptions.StockInsuficienteException;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;

/** Línea de la compra: un ítem vendible y su cantidad. El precio unitario queda congelado. */
public class LineaConfiteria {

    private final ItemVendible item;
    private int cantidad;
    private final double precioUnitario;
    private double subtotal;

    public LineaConfiteria(ItemVendible item, int cantidad) throws DatosInvalidosException, StockInsuficienteException {
        this.item = item;
        this.precioUnitario = item.obtenerPrecio();
        actualizarCantidad(cantidad);
    }

    /** Cambia la cantidad validando que sea positiva y que haya stock, y recalcula el subtotal. */
    public final int actualizarCantidad(int cantidad) throws DatosInvalidosException, StockInsuficienteException {
        if (cantidad <= 0) {
            throw new DatosInvalidosException("La cantidad debe ser mayor que cero.");
        }
        if (!item.hayStock(cantidad)) {
            throw new StockInsuficienteException("No hay stock suficiente de " + item.obtenerNombre() + ".");
        }
        this.cantidad = cantidad;
        this.subtotal = calcularSubtotal();
        return this.cantidad;
    }

    public final double calcularSubtotal() { return precioUnitario * cantidad; }

    public ItemVendible getItem() { return item; }
    public int getCantidad() { return cantidad; }
    public double getPrecioUnitario() { return precioUnitario; }
    public double getSubtotal() { return subtotal; }

    @Override
    public String toString() { return cantidad + " x " + item.obtenerNombre(); }
}
