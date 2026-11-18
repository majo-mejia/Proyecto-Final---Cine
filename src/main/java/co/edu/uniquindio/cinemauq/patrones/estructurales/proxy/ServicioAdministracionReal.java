package co.edu.uniquindio.cinemauq.patrones.estructurales.proxy;

import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.exceptions.OperacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.model.*;
import co.edu.uniquindio.cinemauq.model.enums.*;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy.ReembolsoTotal;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Combo;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Producto;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Patrón PROXY (sujeto real): hace el trabajo; no verifica quién lo llama. */
public class ServicioAdministracionReal implements ServicioAdministracion {

    private final Cine cine;
    private final Clock reloj;

    public ServicioAdministracionReal(Cine cine, Clock reloj) {
        this.cine = cine;
        this.reloj = reloj;
    }

    private LocalDateTime ahora() { return LocalDateTime.now(reloj); }

    private Cliente buscarCliente(String documento) throws DatosInvalidosException {
        return cine.buscarClientePorDocumento(documento)
                .orElseThrow(() -> new DatosInvalidosException("No existe un cliente con documento " + documento + "."));
    }

    // ---------------- Tarjetas y recargas ----------------

    @Override
    public void recargarTarjeta(String documentoCliente, double monto) throws CinemaUQException {
        buscarCliente(documentoCliente).obtenerTarjeta()
                .acreditar(monto, TipoMovimiento.RECARGA, "Recarga del administrador", ahora());
    }

    @Override
    public void aprobarSolicitudRecarga(String idSolicitud) throws CinemaUQException {
        SolicitudRecarga solicitud = buscarSolicitud(idSolicitud);
        Cliente cliente = cine.buscarUsuarioPorCorreo(solicitud.getCorreoElectronico())
                .filter(u -> u instanceof Cliente).map(u -> (Cliente) u)
                .orElseThrow(() -> new DatosInvalidosException("El cliente de la solicitud ya no existe."));
        TarjetaVirtual tarjeta = cliente.obtenerTarjeta();
        if (!solicitud.estaPendiente()) {
            throw new OperacionNoPermitidaException("La solicitud " + idSolicitud + " ya fue respondida.");
        }
        tarjeta.acreditar(solicitud.getMontoSolicitado(), TipoMovimiento.RECARGA,
                "Recarga aprobada (solicitud " + idSolicitud + ")", ahora());
        solicitud.aprobar();
    }

    @Override
    public void rechazarSolicitudRecarga(String idSolicitud) throws CinemaUQException {
        buscarSolicitud(idSolicitud).rechazar();
    }

    private SolicitudRecarga buscarSolicitud(String id) throws DatosInvalidosException {
        return cine.buscarSolicitudRecarga(id)
                .orElseThrow(() -> new DatosInvalidosException("No existe la solicitud " + id + "."));
    }

    @Override
    public void bloquearTarjeta(String documentoCliente) throws CinemaUQException {
        buscarCliente(documentoCliente).obtenerTarjeta().bloquear();
    }

    @Override
    public void activarTarjeta(String documentoCliente) throws CinemaUQException {
        buscarCliente(documentoCliente).obtenerTarjeta().activar();
    }

    // ---------------- Catálogo y funciones ----------------

    /**
     * Cancela la función: los clientes suscritos reciben la notificación (Observer) y cada compra
     * pagada se reembolsa al 100 % (regla 4).
     */
    @Override
    public void cancelarFuncion(String idFuncion) throws CinemaUQException {
        Funcion funcion = cine.buscarFuncion(idFuncion)
                .orElseThrow(() -> new DatosInvalidosException("No existe la función " + idFuncion + "."));
        funcion.cancelar();
        for (Compra compra : cine.getListaCompras()) {
            if (compra.getFuncion().equals(funcion) && compra.estaPagada()) {
                compra.cancelar(new ReembolsoTotal(), MotivoCancelacion.FUNCION_CANCELADA, ahora());
                for (LineaConfiteria linea : compra.getListaLineasConfiteria()) {
                    linea.getItem().devolverStock(linea.getCantidad());
                }
            }
        }
    }

    @Override
    public Funcion programarFuncion(String idPelicula, String idSala, LocalDateTime fechaHora, double precioBase)
            throws CinemaUQException {
        Pelicula pelicula = cine.buscarPelicula(idPelicula)
                .orElseThrow(() -> new DatosInvalidosException("No existe la película " + idPelicula + "."));
        Sala sala = cine.buscarSala(idSala)
                .orElseThrow(() -> new DatosInvalidosException("No existe la sala " + idSala + "."));
        if (precioBase <= 0) {
            throw new DatosInvalidosException("El precio base debe ser mayor que cero.");
        }
        if (!fechaHora.isAfter(ahora())) {
            throw new DatosInvalidosException("La función debe programarse en una fecha futura.");
        }
        Funcion funcion = new Funcion(cine.generarId("F"), pelicula, sala, fechaHora, precioBase);
        cine.programarFuncion(funcion);
        return funcion;
    }

    @Override
    public void agregarPelicula(Pelicula pelicula) throws CinemaUQException {
        validarPelicula(pelicula.getTitulo(), pelicula.getDuracionMinutos(), pelicula.getGenero());
        cine.agregarPelicula(pelicula);
    }

    @Override
    public void actualizarPelicula(String idPelicula, String titulo, String sinopsis, int duracionMinutos,
                                   String genero, String clasificacion, String imagen) throws CinemaUQException {
        Pelicula pelicula = cine.buscarPelicula(idPelicula)
                .orElseThrow(() -> new DatosInvalidosException("No existe la película " + idPelicula + "."));
        validarPelicula(titulo, duracionMinutos, genero);
        pelicula.setTitulo(titulo.trim());
        pelicula.setSinopsis(sinopsis);
        pelicula.setDuracionMinutos(duracionMinutos);
        pelicula.setGenero(genero.trim());
        pelicula.setClasificacion(clasificacion);
        pelicula.setImagenPelicula(imagen == null || imagen.isBlank() ? null : imagen.trim());
    }

    @Override
    public void cambiarCartelera(String idPelicula, boolean enCartelera) throws CinemaUQException {
        Pelicula pelicula = cine.buscarPelicula(idPelicula)
                .orElseThrow(() -> new DatosInvalidosException("No existe la película " + idPelicula + "."));
        if (enCartelera) {
            pelicula.publicarEnCartelera();
        } else {
            pelicula.retirarDeCartelera();
        }
    }

    private void validarPelicula(String titulo, int duracionMinutos, String genero) throws DatosInvalidosException {
        if (titulo == null || titulo.isBlank() || genero == null || genero.isBlank()) {
            throw new DatosInvalidosException("El título y el género de la película son obligatorios.");
        }
        if (duracionMinutos <= 0) {
            throw new DatosInvalidosException("La duración debe ser mayor que cero.");
        }
    }

    @Override
    public void eliminarPelicula(String idPelicula) throws CinemaUQException { cine.retirarPelicula(idPelicula, ahora()); }

    @Override
    public void agregarSala(Sala sala) throws CinemaUQException {
        if (sala.getNombre() == null || sala.getNombre().isBlank()) {
            throw new DatosInvalidosException("La sala necesita un nombre.");
        }
        cine.agregarSala(sala);
    }

    @Override
    public void actualizarSala(String idSala, String nombre, TipoSala tipo) throws CinemaUQException {
        Sala sala = cine.buscarSala(idSala)
                .orElseThrow(() -> new DatosInvalidosException("No existe la sala " + idSala + "."));
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("La sala necesita un nombre.");
        }
        sala.setNombre(nombre.trim());
        sala.setTipo(tipo);
    }

    @Override
    public void agregarItem(ItemVendible item) throws CinemaUQException {
        validarItem(item.obtenerNombre(), item.obtenerPrecio(), item.obtenerCostoEnPuntos());
        cine.agregarItem(item);
    }

    @Override
    public void actualizarItem(String idItem, String nombre, double precio, int costoEnPuntos, int stock)
            throws CinemaUQException {
        ItemVendible item = cine.buscarItem(idItem)
                .orElseThrow(() -> new DatosInvalidosException("No existe el producto o combo " + idItem + "."));
        validarItem(nombre, precio, costoEnPuntos);
        if (item instanceof Producto producto) {
            if (stock < 0) {
                throw new DatosInvalidosException("El stock no puede ser negativo.");
            }
            producto.setNombre(nombre.trim());
            producto.setPrecio(precio);
            producto.setCostoEnPuntos(costoEnPuntos);
            producto.setStock(stock);
        } else if (item instanceof Combo combo) {
            combo.setNombre(nombre.trim());
            combo.setPrecio(precio);
            combo.setCostoEnPuntos(costoEnPuntos);
        }
    }

    @Override
    public void eliminarItem(String idItem) throws CinemaUQException { cine.retirarItem(idItem); }

    private void validarItem(String nombre, double precio, int costoEnPuntos) throws DatosInvalidosException {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre es obligatorio.");
        }
        if (precio <= 0) {
            throw new DatosInvalidosException("El precio debe ser mayor que cero.");
        }
        if (costoEnPuntos < 0) {
            throw new DatosInvalidosException("El costo en puntos no puede ser negativo (0 = no se paga con puntos).");
        }
    }

    @Override
    public void agregarPromocion(Promocion promocion) throws CinemaUQException { cine.agregarPromocion(promocion); }

    @Override
    public void actualizarPromocion(String idPromocion, String nombre, double porcentajeDescuento,
                                    LocalDate fechaInicio, LocalDate fechaFin) throws CinemaUQException {
        Promocion promocion = cine.buscarPromocion(idPromocion)
                .orElseThrow(() -> new DatosInvalidosException("No existe la promoción " + idPromocion + "."));
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre de la promoción es obligatorio.");
        }
        if (fechaInicio == null || fechaFin == null || fechaFin.isBefore(fechaInicio)) {
            throw new DatosInvalidosException("La fecha final de la promoción no puede ser anterior a la inicial.");
        }
        promocion.setPorcentajeDescuento(porcentajeDescuento);
        promocion.setNombre(nombre.trim());
        promocion.setFechaInicio(fechaInicio);
        promocion.setFechaFin(fechaFin);
    }

    @Override
    public void eliminarPromocion(String idPromocion) throws CinemaUQException { cine.retirarPromocion(idPromocion); }

    @Override
    public List<ItemVendible> consultarItems() { return cine.getListaItems(); }

    @Override
    public List<Promocion> consultarPromociones() { return cine.getListaPromociones(); }

    // ---------------- Consultas y estadísticas ----------------

    @Override
    public List<Cliente> consultarClientes() { return cine.obtenerClientes(); }

    @Override
    public List<Pelicula> consultarPeliculas() { return cine.obtenerPeliculas(); }

    @Override
    public List<Sala> consultarSalas() { return cine.getListaSalas(); }

    @Override
    public List<Funcion> consultarFunciones() {
        return cine.getListaFunciones().stream().sorted(Comparator.comparing(Funcion::getFechaHora)).toList();
    }

    @Override
    public List<Compra> consultarCompras() { return cine.getListaCompras(); }

    @Override
    public List<SolicitudRecarga> consultarSolicitudesRecarga() { return cine.getListaSolicitudesRecarga(); }

    @Override
    public double obtenerTotalVentas(LocalDateTime desde, LocalDateTime hasta) {
        return cine.calcularIngresoPeriodo(desde, hasta);
    }

    @Override
    public Map<Sala, Double> obtenerOcupacionSalas() {
        return cine.getListaFunciones().stream().collect(Collectors.groupingBy(Funcion::getSala,
                LinkedHashMap::new, Collectors.averagingDouble(Funcion::obtenerPorcentajeOcupacion)));
    }

    @Override
    public Map<Pelicula, Long> obtenerPeliculasMasVendidas() {
        Map<Pelicula, Long> conteo = cine.getListaCompras().stream()
                .filter(Compra::estaPagada)
                .collect(Collectors.groupingBy(c -> c.getFuncion().getPelicula(),
                        Collectors.summingLong(c -> c.getListaEntradas().size())));
        return ordenarDescendente(conteo);
    }

    @Override
    public Map<String, Integer> obtenerProductosMasVendidos() {
        Map<String, Integer> conteo = cine.getListaCompras().stream()
                .filter(Compra::estaPagada)
                .flatMap(c -> c.getListaLineasConfiteria().stream())
                .collect(Collectors.groupingBy(l -> l.getItem().obtenerNombre(),
                        Collectors.summingInt(LineaConfiteria::getCantidad)));
        return ordenarDescendente(conteo);
    }

    @Override
    public double obtenerTotalReembolsos() {
        return cine.getListaCompras().stream()
                .filter(c -> c.getMetodo() == MetodoPago.TARJETA)
                .map(Compra::getReembolso)
                .filter(r -> r != null)
                .mapToDouble(Reembolso::getMonto)
                .sum();
    }

    @Override
    public Reporte generarReporte(TipoReporte tipo, LocalDate desde, LocalDate hasta) throws CinemaUQException {
        return new GeneradorReportes(cine).generar(tipo, desde, hasta, ahora());
    }

    private static <K, V extends Comparable<V>> Map<K, V> ordenarDescendente(Map<K, V> mapa) {
        return mapa.entrySet().stream()
                .sorted(Map.Entry.<K, V>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }
}
