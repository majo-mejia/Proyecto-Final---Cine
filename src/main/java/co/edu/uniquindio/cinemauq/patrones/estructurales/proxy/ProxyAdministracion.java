package co.edu.uniquindio.cinemauq.patrones.estructurales.proxy;

import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.exceptions.OperacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.model.*;
import co.edu.uniquindio.cinemauq.model.enums.TipoReporte;
import co.edu.uniquindio.cinemauq.model.enums.TipoSala;
import co.edu.uniquindio.cinemauq.model.enums.TipoUsuario;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Patrón PROXY (proxy de protección).
 * Problema: las operaciones de administración (recargar tarjetas, cancelar funciones, ver
 * estadísticas) solo las puede hacer un administrador con sesión iniciada. El proxy tiene la
 * misma interfaz que el servicio real, verifica el usuario y solo entonces delega; así el
 * servicio real no se llena de validaciones de permisos (SRP).
 */
public class ProxyAdministracion implements ServicioAdministracion {

    private final ServicioAdministracion servicioReal;
    private Usuario usuario;

    public ProxyAdministracion(ServicioAdministracion servicioReal) {
        this.servicioReal = servicioReal;
    }

    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    private void verificarAdministrador() throws OperacionNoPermitidaException {
        if (usuario == null || usuario.getTipo() != TipoUsuario.ADMINISTRADOR) {
            throw new OperacionNoPermitidaException("Esta operación solo la puede realizar un administrador.");
        }
    }

    @Override
    public void recargarTarjeta(String documentoCliente, double monto) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.recargarTarjeta(documentoCliente, monto);
    }

    @Override
    public void aprobarSolicitudRecarga(String idSolicitud) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.aprobarSolicitudRecarga(idSolicitud);
    }

    @Override
    public void rechazarSolicitudRecarga(String idSolicitud) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.rechazarSolicitudRecarga(idSolicitud);
    }

    @Override
    public void bloquearTarjeta(String documentoCliente) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.bloquearTarjeta(documentoCliente);
    }

    @Override
    public void activarTarjeta(String documentoCliente) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.activarTarjeta(documentoCliente);
    }

    @Override
    public void cancelarFuncion(String idFuncion) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.cancelarFuncion(idFuncion);
    }

    @Override
    public Funcion programarFuncion(String idPelicula, String idSala, LocalDateTime fechaHora, double precioBase)
            throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.programarFuncion(idPelicula, idSala, fechaHora, precioBase);
    }

    @Override
    public void agregarPelicula(Pelicula pelicula) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.agregarPelicula(pelicula);
    }

    @Override
    public void eliminarPelicula(String idPelicula) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.eliminarPelicula(idPelicula);
    }

    @Override
    public void agregarSala(Sala sala) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.agregarSala(sala);
    }

    @Override
    public void agregarItem(ItemVendible item) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.agregarItem(item);
    }

    @Override
    public void agregarPromocion(Promocion promocion) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.agregarPromocion(promocion);
    }

    @Override
    public void actualizarPelicula(String idPelicula, String titulo, String sinopsis, int duracionMinutos,
                                   String genero, String clasificacion, String imagen) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.actualizarPelicula(idPelicula, titulo, sinopsis, duracionMinutos, genero, clasificacion, imagen);
    }

    @Override
    public void cambiarCartelera(String idPelicula, boolean enCartelera) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.cambiarCartelera(idPelicula, enCartelera);
    }

    @Override
    public void actualizarSala(String idSala, String nombre, TipoSala tipo) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.actualizarSala(idSala, nombre, tipo);
    }

    @Override
    public void actualizarItem(String idItem, String nombre, double precio, int costoEnPuntos, int stock)
            throws CinemaUQException {
        verificarAdministrador();
        servicioReal.actualizarItem(idItem, nombre, precio, costoEnPuntos, stock);
    }

    @Override
    public void eliminarItem(String idItem) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.eliminarItem(idItem);
    }

    @Override
    public void actualizarPromocion(String idPromocion, String nombre, double porcentajeDescuento,
                                    LocalDate fechaInicio, LocalDate fechaFin) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.actualizarPromocion(idPromocion, nombre, porcentajeDescuento, fechaInicio, fechaFin);
    }

    @Override
    public void eliminarPromocion(String idPromocion) throws CinemaUQException {
        verificarAdministrador();
        servicioReal.eliminarPromocion(idPromocion);
    }

    @Override
    public List<ItemVendible> consultarItems() throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.consultarItems();
    }

    @Override
    public List<Promocion> consultarPromociones() throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.consultarPromociones();
    }

    @Override
    public List<Cliente> consultarClientes() throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.consultarClientes();
    }

    @Override
    public List<Pelicula> consultarPeliculas() throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.consultarPeliculas();
    }

    @Override
    public List<Sala> consultarSalas() throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.consultarSalas();
    }

    @Override
    public List<Funcion> consultarFunciones() throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.consultarFunciones();
    }

    @Override
    public List<Compra> consultarCompras() throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.consultarCompras();
    }

    @Override
    public List<SolicitudRecarga> consultarSolicitudesRecarga() throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.consultarSolicitudesRecarga();
    }

    @Override
    public double obtenerTotalVentas(LocalDateTime desde, LocalDateTime hasta) throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.obtenerTotalVentas(desde, hasta);
    }

    @Override
    public Map<Sala, Double> obtenerOcupacionSalas() throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.obtenerOcupacionSalas();
    }

    @Override
    public Map<Pelicula, Long> obtenerPeliculasMasVendidas() throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.obtenerPeliculasMasVendidas();
    }

    @Override
    public Map<String, Integer> obtenerProductosMasVendidos() throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.obtenerProductosMasVendidos();
    }

    @Override
    public double obtenerTotalReembolsos() throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.obtenerTotalReembolsos();
    }

    @Override
    public Reporte generarReporte(TipoReporte tipo, LocalDate desde, LocalDate hasta) throws CinemaUQException {
        verificarAdministrador();
        return servicioReal.generarReporte(tipo, desde, hasta);
    }
}
