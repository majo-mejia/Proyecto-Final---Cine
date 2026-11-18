package co.edu.uniquindio.cinemauq.controllers.cliente;

import co.edu.uniquindio.cinemauq.model.Promocion;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;

import java.util.List;
import java.util.Map;

/** Lo que el cliente agrega a la compra además de las entradas: confitería y promoción. */
public record SeleccionConfiteria(Map<ItemVendible, Integer> items, List<Promocion> promociones) {

    /** Valor de la confitería antes de promociones (cada ítem, sea producto o combo, da su precio). */
    public double valor() {
        return items.entrySet().stream().mapToDouble(e -> e.getKey().obtenerPrecio() * e.getValue()).sum();
    }

    /** Puntos que cuesta la confitería si se paga con puntos. */
    public int costoEnPuntos() {
        return items.entrySet().stream().mapToInt(e -> e.getKey().obtenerCostoEnPuntos() * e.getValue()).sum();
    }
}
