package co.edu.uniquindio.cinemauq.patrones.creacionales.builder;

import co.edu.uniquindio.cinemauq.exceptions.AsientoNoDisponibleException;
import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.model.*;
import co.edu.uniquindio.cinemauq.model.enums.MetodoPago;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Patrón BUILDER.
 * Problema: una compra tiene muchas partes opcionales (asientos, confitería, promociones,
 * método de pago) que el cliente va eligiendo pantalla por pantalla. El builder acumula las
 * elecciones paso a paso y en build() arma las entradas y líneas de confitería y crea la Compra
 * con new Compra(builder); la Compra valida su contenido y calcula descuento, total y puntos.
 */
public class CompraBuilder {

    private static final AtomicInteger SECUENCIA_COMPRA = new AtomicInteger();
    private static final AtomicInteger SECUENCIA_ENTRADA = new AtomicInteger();

    // Elecciones
    private String id;
    private Cliente cliente;
    private Funcion funcion;
    private LocalDateTime fechaHora;
    private MetodoPago metodo = MetodoPago.TARJETA;
    private final Set<String> codigosAsientos = new LinkedHashSet<>();
    private final Map<ItemVendible, Integer> items = new LinkedHashMap<>();
    private final List<Promocion> listaPromociones = new ArrayList<>();

    // Partes armadas en build()
    private final List<Entrada> listaEntradas = new ArrayList<>();
    private final List<LineaConfiteria> listaLineasConfiteria = new ArrayList<>();

    public CompraBuilder conId(String id) { this.id = id; return this; }

    public CompraBuilder paraCliente(Cliente cliente) { this.cliente = cliente; return this; }

    public CompraBuilder enFuncion(Funcion funcion) { this.funcion = funcion; return this; }

    public CompraBuilder enFecha(LocalDateTime fechaHora) { this.fechaHora = fechaHora; return this; }

    public CompraBuilder pagarCon(MetodoPago metodo) { this.metodo = metodo; return this; }

    public CompraBuilder agregarAsiento(String codigo) throws DatosInvalidosException {
        if (!codigosAsientos.add(codigo.toUpperCase())) {
            throw new DatosInvalidosException("El asiento " + codigo + " ya está en la compra.");
        }
        return this;
    }

    /** Si el ítem ya estaba, suma la cantidad. */
    public CompraBuilder agregarItem(ItemVendible item, int cantidad) {
        items.merge(item, cantidad, Integer::sum);
        return this;
    }

    public CompraBuilder agregarPromocion(Promocion promocion) {
        listaPromociones.add(promocion);
        return this;
    }

    /**
     * Arma las partes: verifica que la función se pueda vender, convierte cada código en una
     * Entrada (el asiento debe existir y estar libre) y cada ítem en una LineaConfiteria (valida
     * cantidad y stock). Las reglas del conjunto y los totales los resuelve la propia Compra.
     */
    public Compra build() throws CinemaUQException {
        if (funcion == null || fechaHora == null) {
            throw new DatosInvalidosException("La compra necesita cliente, función y fecha.");
        }
        funcion.validarVendible(fechaHora);

        listaEntradas.clear();
        for (String codigo : codigosAsientos) {
            Asiento asiento = funcion.getSala().buscarAsiento(codigo).orElseThrow(() ->
                    new AsientoNoDisponibleException("El asiento " + codigo + " no existe en la sala."));
            if (!funcion.estaDisponible(codigo)) {
                throw new AsientoNoDisponibleException("El asiento " + codigo + " ya está ocupado.");
            }
            listaEntradas.add(new Entrada("E-" + SECUENCIA_ENTRADA.incrementAndGet(), funcion, asiento,
                    funcion.calcularPrecioEntrada()));
        }

        listaLineasConfiteria.clear();
        for (Map.Entry<ItemVendible, Integer> e : items.entrySet()) {
            listaLineasConfiteria.add(new LineaConfiteria(e.getKey(), e.getValue()));
        }

        if (id == null) {
            id = "C-" + SECUENCIA_COMPRA.incrementAndGet();
        }
        return new Compra(this);
    }

    public String getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Funcion getFuncion() { return funcion; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public MetodoPago getMetodo() { return metodo; }
    public List<Entrada> getListaEntradas() { return listaEntradas; }
    public List<LineaConfiteria> getListaLineasConfiteria() { return listaLineasConfiteria; }
    public List<Promocion> getListaPromociones() { return listaPromociones; }
}
