package co.edu.uniquindio.cinemauq.patrones.estructurales.composite;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.exceptions.StockInsuficienteException;

import java.util.*;

/**
 * Patrón COMPOSITE (compuesto): agrupa ítems vendibles. Tiene precio propio, pero el stock
 * lo delega en sus ítems: vender un combo descuenta cada producto que contiene.
 */
public class Combo implements ItemVendible {

    private final String id;
    private String nombre;
    private double precio;
    private int costoEnPuntos;
    private final List<ItemVendible> listaItems = new ArrayList<>();

    public Combo(String id, String nombre, double precio, int costoEnPuntos) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.costoEnPuntos = costoEnPuntos;
    }

    public void agregarItem(ItemVendible item) throws DatosInvalidosException {
        if (item == this || (item instanceof Combo c && c.contiene(this))) {
            throw new DatosInvalidosException("Un combo no puede contenerse a sí mismo.");
        }
        listaItems.add(item);
    }

    public void quitarItem(ItemVendible item) { listaItems.remove(item); }

    public List<ItemVendible> obtenerItems() { return Collections.unmodifiableList(listaItems); }

    public boolean contiene(ItemVendible item) {
        for (ItemVendible hijo : listaItems) {
            if (hijo.equals(item) || (hijo instanceof Combo c && c.contiene(item))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String obtenerId() { return id; }

    @Override
    public String obtenerNombre() { return nombre; }

    @Override
    public double obtenerPrecio() { return precio; }

    @Override
    public int obtenerCostoEnPuntos() { return costoEnPuntos; }

    /** Hay stock si cada ítem alcanza para "cantidad" combos (contando ítems repetidos). */
    @Override
    public boolean hayStock(int cantidad) {
        if (cantidad <= 0 || listaItems.isEmpty()) {
            return false;
        }
        Map<ItemVendible, Integer> conteo = new LinkedHashMap<>();
        listaItems.forEach(i -> conteo.merge(i, 1, Integer::sum));
        return conteo.entrySet().stream().allMatch(e -> e.getKey().hayStock(e.getValue() * cantidad));
    }

    @Override
    public void descontarStock(int cantidad) throws StockInsuficienteException {
        if (!hayStock(cantidad)) {
            throw new StockInsuficienteException("No hay stock suficiente para " + cantidad + " " + nombre + ".");
        }
        for (ItemVendible item : listaItems) {
            item.descontarStock(cantidad);
        }
    }

    @Override
    public void devolverStock(int cantidad) {
        listaItems.forEach(i -> i.devolverStock(cantidad));
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPrecio(double precio) { this.precio = precio; }
    public void setCostoEnPuntos(int costoEnPuntos) { this.costoEnPuntos = costoEnPuntos; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Combo c && id.equals(c.id));
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return nombre; }
}
