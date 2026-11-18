package co.edu.uniquindio.cinemauq.patrones.estructurales.composite;

import co.edu.uniquindio.cinemauq.exceptions.StockInsuficienteException;

import java.util.Objects;

/** Patrón COMPOSITE (hoja): producto individual con su propio precio y stock. */
public class Producto implements ItemVendible {

    private final String id;
    private String nombre;
    private double precio;
    private int stock;
    private int costoEnPuntos;

    public Producto(String id, String nombre, double precio, int stock, int costoEnPuntos) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
        this.costoEnPuntos = costoEnPuntos;
    }

    @Override
    public String obtenerId() { return id; }

    @Override
    public String obtenerNombre() { return nombre; }

    @Override
    public double obtenerPrecio() { return precio; }

    @Override
    public int obtenerCostoEnPuntos() { return costoEnPuntos; }

    @Override
    public boolean hayStock(int cantidad) { return cantidad > 0 && stock >= cantidad; }

    @Override
    public void descontarStock(int cantidad) throws StockInsuficienteException {
        if (!hayStock(cantidad)) {
            throw new StockInsuficienteException("No hay " + cantidad + " unidades de " + nombre
                    + " (quedan " + stock + ").");
        }
        stock -= cantidad;
    }

    @Override
    public void devolverStock(int cantidad) {
        if (cantidad > 0) {
            stock += cantidad;
        }
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPrecio(double precio) { this.precio = precio; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public void setCostoEnPuntos(int costoEnPuntos) { this.costoEnPuntos = costoEnPuntos; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Producto p && id.equals(p.id));
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return nombre; }
}
