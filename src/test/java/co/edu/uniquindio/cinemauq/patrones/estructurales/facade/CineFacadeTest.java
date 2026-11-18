package co.edu.uniquindio.cinemauq.patrones.estructurales.facade;

import co.edu.uniquindio.cinemauq.exceptions.CredencialesInvalidasException;
import co.edu.uniquindio.cinemauq.exceptions.OperacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.exceptions.SaldoInsuficienteException;
import co.edu.uniquindio.cinemauq.exceptions.UsuarioYaExisteException;
import co.edu.uniquindio.cinemauq.model.Cine;
import co.edu.uniquindio.cinemauq.model.Cliente;
import co.edu.uniquindio.cinemauq.model.Compra;
import co.edu.uniquindio.cinemauq.model.Funcion;
import co.edu.uniquindio.cinemauq.model.Pelicula;
import co.edu.uniquindio.cinemauq.model.Sala;
import co.edu.uniquindio.cinemauq.model.SolicitudRecarga;
import co.edu.uniquindio.cinemauq.model.enums.MetodoPago;
import co.edu.uniquindio.cinemauq.model.enums.MotivoCancelacion;
import co.edu.uniquindio.cinemauq.model.enums.TipoSala;
import co.edu.uniquindio.cinemauq.patrones.creacionales.factorymethod.AdministradorFactory;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Combo;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Producto;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Flujo completo a través de la fachada y del proxy de administración. */
class CineFacadeTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 10, 0);

    private Cine cine;
    private CineFacade facade;
    private Funcion funcion;
    private Producto crispetas;
    private Combo combo;
    private Cliente cliente;

    private static Clock relojEn(LocalDateTime momento) {
        ZoneId zona = ZoneId.systemDefault();
        return Clock.fixed(momento.atZone(zona).toInstant(), zona);
    }

    @BeforeEach
    void setUp() throws Exception {
        cine = new Cine("CinemaUQ", "900.123.456-7", "Armenia", "6067350000", "contacto@cinemauq.co");
        cine.registrarUsuario(new AdministradorFactory()
                .crearAdministrador("Admin", "1", "admin@cinemauq.co", "admin123"));
        facade = new CineFacade(cine, relojEn(AHORA));

        facade.iniciarSesion("admin@cinemauq.co", "admin123");
        ServicioAdministracion admin = facade.obtenerServicioAdministracion();
        Sala sala = new Sala("S1", "Sala 1", TipoSala.SALA_2D);
        sala.crearAsientos(3, 4, 0);
        admin.agregarSala(sala);
        admin.agregarPelicula(new Pelicula("P1", "Película", "", 100, "Drama", "+7", null));
        funcion = admin.programarFuncion("P1", "S1", AHORA.plusDays(3), 12_000);
        crispetas = new Producto("PR1", "Crispetas", 10_000, 10, 80);
        Producto gaseosa = new Producto("PR2", "Gaseosa", 6_000, 10, 50);
        combo = new Combo("CO1", "Combo", 20_000, 200);
        combo.agregarItem(crispetas);
        combo.agregarItem(gaseosa);
        admin.agregarItem(crispetas);
        admin.agregarItem(gaseosa);
        admin.agregarItem(combo);
        facade.cerrarSesion();

        cliente = facade.registrarCliente("Ana", "100", "ana@uq.edu.co", "1234");
        facade.iniciarSesion("admin@cinemauq.co", "admin123");
        facade.obtenerServicioAdministracion().recargarTarjeta("100", 100_000);
        facade.cerrarSesion();
        facade.iniciarSesion("ana@uq.edu.co", "1234");
    }

    @Test
    void registroYLoginValidanDatos() {
        assertThrows(UsuarioYaExisteException.class,
                () -> facade.registrarCliente("Otra", "100", "otra@uq.edu.co", "1234"));
        assertThrows(CredencialesInvalidasException.class, () -> facade.iniciarSesion("ana@uq.edu.co", "mala"));
    }

    @Test
    void compraConTarjetaCobraOcupaAsientosDescuentaStockYDaPuntos() throws Exception {
        Compra compra = facade.comprar(funcion, List.of("A1", "A2"), Map.of(combo, 1), List.of(), MetodoPago.TARJETA);

        // 2 x 12.000 + combo 20.000 + 2.000 de servicio
        assertEquals(46_000, compra.getTotal());
        assertEquals(54_000, facade.consultarSaldo());
        assertFalse(funcion.estaDisponible("A1"));
        assertEquals(9, crispetas.getStock());
        assertEquals(46, facade.consultarPuntos());
        assertEquals(1, facade.consultarHistorialCompras().size());
    }

    @Test
    void compraSinSaldoNoModificaNada() {
        assertThrows(SaldoInsuficienteException.class, () -> facade.comprar(funcion,
                List.of("A1", "A2", "A3", "A4", "B1", "B2", "B3", "B4", "C1"), Map.<ItemVendible, Integer>of(),
                List.of(), MetodoPago.TARJETA));
        assertTrue(funcion.estaDisponible("A1"));
        assertTrue(cine.getListaCompras().isEmpty());
    }

    @Test
    void cancelarConMasDe24HorasReembolsaTodoYLiberaAsientos() throws Exception {
        Compra compra = facade.comprar(funcion, List.of("A1"), Map.of(crispetas, 2), List.of(), MetodoPago.TARJETA);
        facade.cancelarCompra(compra);

        assertEquals(100_000, facade.consultarSaldo());
        assertTrue(funcion.estaDisponible("A1"));
        assertEquals(10, crispetas.getStock());
        assertEquals(1.0, compra.getReembolso().getPorcentaje());
    }

    @Test
    void cancelarCompraConTarjetaAnulaLosPuntosQueGano() throws Exception {
        Compra compra = facade.comprar(funcion, List.of("A1", "A2"), Map.of(combo, 1), List.of(), MetodoPago.TARJETA);
        assertEquals(46, facade.consultarPuntos());

        facade.cancelarCompra(compra);
        assertEquals(0, facade.consultarPuntos());
        assertEquals(46, cliente.obtenerCuentaPuntos().obtenerTotalAnulado());
    }

    @Test
    void compraConPuntosDescuentaPuntosYNoDinero() throws Exception {
        cliente.obtenerCuentaPuntos().acumular(500, AHORA.toLocalDate(), 1);
        facade.comprar(funcion, List.of("A1", "A2"), Map.of(crispetas, 1), List.of(), MetodoPago.PUNTOS);

        // 2 entradas x 120 + crispetas 80
        assertEquals(180, facade.consultarPuntos());
        assertEquals(100_000, facade.consultarSaldo());
    }

    @Test
    void elProxyBloqueaAlClienteYPermiteAlAdministrador() throws Exception {
        assertThrows(OperacionNoPermitidaException.class,
                () -> facade.obtenerServicioAdministracion().recargarTarjeta("100", 1_000));

        SolicitudRecarga solicitud = facade.solicitarRecarga(30_000);
        facade.cerrarSesion();
        facade.iniciarSesion("admin@cinemauq.co", "admin123");
        facade.obtenerServicioAdministracion().aprobarSolicitudRecarga(solicitud.getId());
        assertEquals(130_000, cliente.obtenerTarjeta().getSaldo());
    }

    @Test
    void cancelarFuncionReembolsaTodoYNotificaAlCliente() throws Exception {
        Compra compra = facade.comprar(funcion, List.of("A1"), Map.<ItemVendible, Integer>of(), List.of(),
                MetodoPago.TARJETA);
        facade.cerrarSesion();
        facade.iniciarSesion("admin@cinemauq.co", "admin123");
        facade.obtenerServicioAdministracion().cancelarFuncion(funcion.getId());

        assertEquals(MotivoCancelacion.FUNCION_CANCELADA, compra.getReembolso().getMotivo());
        assertEquals(100_000, cliente.obtenerTarjeta().getSaldo());
        assertEquals(1, cliente.contarNotificacionesNoLeidas());
        assertEquals(14_000, facade.obtenerServicioAdministracion().obtenerTotalReembolsos());
    }
}
