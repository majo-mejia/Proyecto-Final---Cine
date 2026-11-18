package co.edu.uniquindio.cinemauq.patrones.estructurales.facade;

import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.exceptions.CredencialesInvalidasException;
import co.edu.uniquindio.cinemauq.exceptions.OperacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.model.*;
import co.edu.uniquindio.cinemauq.model.enums.MetodoPago;
import co.edu.uniquindio.cinemauq.model.enums.MotivoCancelacion;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy.PoliticaReembolso;
import co.edu.uniquindio.cinemauq.patrones.creacionales.builder.CompraBuilder;
import co.edu.uniquindio.cinemauq.patrones.creacionales.factorymethod.ClienteFactory;
import co.edu.uniquindio.cinemauq.patrones.creacionales.singleton.ConfiguracionCine;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ProxyAdministracion;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracion;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracionReal;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Patrón FACADE.
 * Problema: comprar implica validar la función y los asientos, armar la compra, cobrar, marcar
 * asientos, descontar stock, acumular puntos y suscribir al cliente a la función. Sin una fachada,
 * cada controlador tendría que coordinar todas esas clases. CineFacade ofrece operaciones simples
 * (iniciarSesion, comprar, cancelarCompra...) y esconde esa coordinación.
 */
public class CineFacade {

    private final Cine cine;
    private final Clock reloj;
    private final ProxyAdministracion servicioAdministracion;
    private Usuario usuarioActual;

    public CineFacade(Cine cine) {
        this(cine, Clock.systemDefaultZone());
    }

    /** El reloj se puede fijar para probar las reglas que dependen de la hora. */
    public CineFacade(Cine cine, Clock reloj) {
        this.cine = cine;
        this.reloj = reloj;
        this.servicioAdministracion = new ProxyAdministracion(new ServicioAdministracionReal(cine, reloj));
    }

    private LocalDateTime ahora() { return LocalDateTime.now(reloj); }

    // ---------------- Sesión ----------------

    public Cliente registrarCliente(String nombre, String documento, String correoElectronico, String contrasena)
            throws CinemaUQException {
        Cliente cliente = new ClienteFactory().crearCliente(nombre, documento, correoElectronico, contrasena);
        cine.registrarUsuario(cliente);
        return cliente;
    }

    public Usuario iniciarSesion(String correoElectronico, String contrasena) throws CredencialesInvalidasException {
        Usuario usuario = cine.getListaUsuarios().stream()
                .filter(u -> u.validarCredenciales(correoElectronico, contrasena))
                .findFirst()
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos."));
        usuarioActual = usuario;
        servicioAdministracion.setUsuario(usuario);
        return usuario;
    }

    public void cerrarSesion() {
        usuarioActual = null;
        servicioAdministracion.setUsuario(null);
    }

    public Usuario getUsuarioActual() { return usuarioActual; }

    /** Operaciones del administrador, protegidas por el proxy. */
    public ServicioAdministracion obtenerServicioAdministracion() { return servicioAdministracion; }

    private Cliente clienteActual() throws OperacionNoPermitidaException {
        if (usuarioActual instanceof Cliente cliente) {
            return cliente;
        }
        throw new OperacionNoPermitidaException("Debe iniciar sesión como cliente.");
    }

    // ---------------- Cartelera ----------------

    public List<Pelicula> consultarCartelera() {
        return cine.obtenerPeliculas().stream().filter(Pelicula::estaEnCartelera).toList();
    }

    /** Funciones programadas y futuras de una película, ordenadas por fecha. */
    public List<Funcion> consultarFunciones(Pelicula pelicula) {
        LocalDateTime ahora = ahora();
        return pelicula.getListaFunciones().stream()
                .filter(f -> f.estaProgramada() && !f.yaComenzo(ahora))
                .sorted(Comparator.comparing(Funcion::getFechaHora))
                .toList();
    }

    public Collection<AsientoFuncion> consultarAsientos(Funcion funcion) {
        return funcion.obtenerAsientos();
    }

    public List<ItemVendible> consultarProductos() {
        return cine.getListaItems().stream().filter(i -> i.hayStock(1)).toList();
    }

    public List<Promocion> consultarPromocionesVigentes() {
        return cine.getListaPromociones().stream().filter(p -> p.estaVigente(ahora().toLocalDate())).toList();
    }

    // ---------------- Compra ----------------

    /**
     * Compra completa. Todo se valida antes de cobrar (asientos, stock, saldo o puntos); si algo
     * falla se lanza la excepción y no queda nada modificado.
     */
    public Compra comprar(Funcion funcion, List<String> codigosAsientos, Map<ItemVendible, Integer> items,
                          List<Promocion> promociones, MetodoPago metodo) throws CinemaUQException {
        Cliente cliente = clienteActual();
        LocalDateTime ahora = ahora();

        CompraBuilder builder = new CompraBuilder()
                .conId(cine.generarId("C"))
                .paraCliente(cliente)
                .enFuncion(funcion)
                .enFecha(ahora)
                .pagarCon(metodo);
        for (String codigo : codigosAsientos) {
            builder.agregarAsiento(codigo);
        }
        items.forEach(builder::agregarItem);
        promociones.forEach(builder::agregarPromocion);
        Compra compra = builder.build();

        compra.pagar();

        for (Entrada entrada : compra.getListaEntradas()) {
            funcion.ocuparAsiento(entrada.getAsiento().obtenerIdentificador(), ahora);
        }
        for (LineaConfiteria linea : compra.getListaLineasConfiteria()) {
            linea.getItem().descontarStock(linea.getCantidad());
        }
        if (metodo == MetodoPago.TARJETA) {
            // El lote queda marcado con el id de la compra para poder anularlo si se cancela.
            ConfiguracionCine config = ConfiguracionCine.getInstancia();
            int puntos = (int) (compra.getTotal() / config.getValorPorPuntos());
            cliente.obtenerCuentaPuntos().acumular(puntos, ahora.toLocalDate(), config.getVigenciaPuntosAnios(),
                    compra.getId());
        }
        cine.registrarCompra(compra);
        cliente.agregarCompra(compra);
        funcion.agregarObservador(cliente);
        return compra;
    }

    /** Cancelación pedida por el cliente; el porcentaje lo decide la política de reembolso (regla 4). */
    public void cancelarCompra(Compra compra) throws CinemaUQException {
        Cliente cliente = clienteActual();
        if (!compra.getCliente().equals(cliente)) {
            throw new OperacionNoPermitidaException("Solo puede cancelar sus propias compras.");
        }
        LocalDateTime ahora = ahora();
        PoliticaReembolso politica = PoliticaReembolso.seleccionar(
                compra.getFuncion(), MotivoCancelacion.CANCELACION_CLIENTE, ahora);
        compra.cancelar(politica, MotivoCancelacion.CANCELACION_CLIENTE, ahora);

        for (Entrada entrada : compra.getListaEntradas()) {
            compra.getFuncion().liberarAsiento(entrada.getAsiento().obtenerIdentificador());
        }
        for (LineaConfiteria linea : compra.getListaLineasConfiteria()) {
            linea.getItem().devolverStock(linea.getCantidad());
        }
    }

    // ---------------- Tarjeta, puntos y notificaciones ----------------

    public double consultarSaldo() throws OperacionNoPermitidaException {
        return clienteActual().obtenerTarjeta().getSaldo();
    }

    public List<MovimientoTarjeta> consultarMovimientos() throws OperacionNoPermitidaException {
        return clienteActual().obtenerTarjeta().obtenerMovimientos();
    }

    public int consultarPuntos() throws OperacionNoPermitidaException {
        return clienteActual().obtenerCuentaPuntos().obtenerPuntosDisponibles(ahora().toLocalDate());
    }

    public List<Compra> consultarHistorialCompras() throws OperacionNoPermitidaException {
        return clienteActual().consultarHistorialCompras();
    }

    public SolicitudRecarga solicitarRecarga(double monto) throws CinemaUQException {
        SolicitudRecarga solicitud = clienteActual().solicitarRecarga(cine.generarId("SR"), monto, ahora());
        cine.registrarSolicitudRecarga(solicitud);
        return solicitud;
    }

    public List<Notificacion> consultarNotificaciones() throws OperacionNoPermitidaException {
        return clienteActual().getListaNotificaciones();
    }
}
