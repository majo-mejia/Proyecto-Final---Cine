package co.edu.uniquindio.cinemauq.patrones.estructurales.proxy;

import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.model.*;
import co.edu.uniquindio.cinemauq.model.enums.TipoReporte;
import co.edu.uniquindio.cinemauq.model.enums.TipoSala;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Operaciones del perfil administrador. La implementan el servicio real y el proxy que
 * controla el acceso (patrón Proxy); los controladores del admin solo ven esta interfaz (DIP).
 */
public interface ServicioAdministracion {

    void recargarTarjeta(String documentoCliente, double monto) throws CinemaUQException;

    void aprobarSolicitudRecarga(String idSolicitud) throws CinemaUQException;

    void rechazarSolicitudRecarga(String idSolicitud) throws CinemaUQException;

    void bloquearTarjeta(String documentoCliente) throws CinemaUQException;

    void activarTarjeta(String documentoCliente) throws CinemaUQException;

    void cancelarFuncion(String idFuncion) throws CinemaUQException;

    Funcion programarFuncion(String idPelicula, String idSala, LocalDateTime fechaHora, double precioBase)
            throws CinemaUQException;

    void agregarPelicula(Pelicula pelicula) throws CinemaUQException;

    void eliminarPelicula(String idPelicula) throws CinemaUQException;

    void agregarSala(Sala sala) throws CinemaUQException;

    void agregarItem(ItemVendible item) throws CinemaUQException;

    void agregarPromocion(Promocion promocion) throws CinemaUQException;

    void actualizarPelicula(String idPelicula, String titulo, String sinopsis, int duracionMinutos,
                            String genero, String clasificacion, String imagen) throws CinemaUQException;

    /** Publica o retira la película de la cartelera (sin borrarla). */
    void cambiarCartelera(String idPelicula, boolean enCartelera) throws CinemaUQException;

    void actualizarSala(String idSala, String nombre, TipoSala tipo) throws CinemaUQException;

    /** Edita un producto o combo; el stock solo aplica a productos (en un combo se ignora). */
    void actualizarItem(String idItem, String nombre, double precio, int costoEnPuntos, int stock) throws CinemaUQException;

    void eliminarItem(String idItem) throws CinemaUQException;

    void actualizarPromocion(String idPromocion, String nombre, double porcentajeDescuento,
                             LocalDate fechaInicio, LocalDate fechaFin) throws CinemaUQException;

    void eliminarPromocion(String idPromocion) throws CinemaUQException;

    List<ItemVendible> consultarItems() throws CinemaUQException;

    List<Promocion> consultarPromociones() throws CinemaUQException;

    List<Cliente> consultarClientes() throws CinemaUQException;

    List<Pelicula> consultarPeliculas() throws CinemaUQException;

    List<Sala> consultarSalas() throws CinemaUQException;

    /** Todas las funciones (pasadas, futuras y canceladas), ordenadas por fecha. */
    List<Funcion> consultarFunciones() throws CinemaUQException;

    List<Compra> consultarCompras() throws CinemaUQException;

    List<SolicitudRecarga> consultarSolicitudesRecarga() throws CinemaUQException;

    double obtenerTotalVentas(LocalDateTime desde, LocalDateTime hasta) throws CinemaUQException;

    /** Ocupación promedio (0 a 100) de las funciones de cada sala. */
    Map<Sala, Double> obtenerOcupacionSalas() throws CinemaUQException;

    /** Entradas vendidas (no reembolsadas) por película, de mayor a menor. */
    Map<Pelicula, Long> obtenerPeliculasMasVendidas() throws CinemaUQException;

    /** Unidades vendidas (no reembolsadas) por producto o combo, de mayor a menor. */
    Map<String, Integer> obtenerProductosMasVendidos() throws CinemaUQException;

    /** Dinero devuelto a tarjetas por cancelaciones (los reembolsos en puntos no cuentan). */
    double obtenerTotalReembolsos() throws CinemaUQException;

    /**
     * Genera el reporte del tipo elegido para el rango [desde, hasta] (fechas incluidas).
     * El reporte resultante se puede exportar con cualquier Exportador (TXT o PDF).
     */
    Reporte generarReporte(TipoReporte tipo, LocalDate desde, LocalDate hasta) throws CinemaUQException;
}
