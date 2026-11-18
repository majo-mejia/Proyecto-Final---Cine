package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.AsientoNoDisponibleException;
import co.edu.uniquindio.cinemauq.exceptions.CancelacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.exceptions.FuncionNoDisponibleException;
import co.edu.uniquindio.cinemauq.exceptions.OperacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.exceptions.PuntosInsuficientesException;
import co.edu.uniquindio.cinemauq.exceptions.ReembolsoYaRealizadoException;
import co.edu.uniquindio.cinemauq.exceptions.SaldoInsuficienteException;
import co.edu.uniquindio.cinemauq.exceptions.StockInsuficienteException;
import co.edu.uniquindio.cinemauq.exceptions.TarjetaInactivaException;
import co.edu.uniquindio.cinemauq.model.enums.MotivoCancelacion;
import co.edu.uniquindio.cinemauq.model.enums.TipoMovimiento;
import co.edu.uniquindio.cinemauq.model.enums.TipoSala;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy.PoliticaReembolso;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy.ReembolsoParcial;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy.ReembolsoTotal;
import co.edu.uniquindio.cinemauq.patrones.comportamiento.strategy.SinReembolso;
import co.edu.uniquindio.cinemauq.patrones.creacionales.builder.CompraBuilder;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Combo;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Producto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de las reglas de negocio que viven en el modelo y en los patrones de comportamiento. */
class ModeloReglasTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 10, 0);

    private Sala sala;
    private Funcion funcion;
    private Cliente cliente;

    @BeforeEach
    void setUp() throws Exception {
        Pelicula pelicula = new Pelicula("P1", "Película de prueba", "", 120, "Acción", "+12", null);
        sala = new Sala("S1", "Sala 1", TipoSala.SALA_2D);
        sala.crearAsientos(5, 8, 1);
        funcion = new Funcion("F1", pelicula, sala, AHORA.plusDays(2), 12_000);
        cliente = new Cliente("Ana", "100", "ana@uq.edu.co", "1234", LocalDate.of(2026, 1, 1),
                new TarjetaVirtual("TV-100", 50_000));
    }

    // ---------- Regla 1: asientos ----------

    @Test
    void salaCreaAsientosYMarcaAccesiblesLasPrimerasFilas() {
        assertEquals(40, sala.obtenerCapacidad());
        assertTrue(sala.buscarAsiento("A1").orElseThrow().esAccesible());
        assertFalse(sala.buscarAsiento("E8").orElseThrow().esAccesible());
        assertThrows(DatosInvalidosException.class, () -> sala.crearAsiento('A', 1, false));
    }

    @Test
    void unAsientoNoSeVendeDosVecesEnLaMismaFuncion() throws Exception {
        funcion.ocuparAsiento("A1", AHORA);
        assertThrows(AsientoNoDisponibleException.class, () -> funcion.ocuparAsiento("A1", AHORA));
        assertEquals(1, funcion.contarOcupados());
    }

    @Test
    void noSeVendeEnFuncionYaIniciadaOCancelada() throws Exception {
        assertThrows(FuncionNoDisponibleException.class, () -> funcion.ocuparAsiento("A1", AHORA.plusDays(5)));
        funcion.cancelar();
        assertThrows(FuncionNoDisponibleException.class, () -> funcion.ocuparAsiento("A1", AHORA));
    }

    @Test
    void cancelarFuncionNotificaAlClienteObservador() throws Exception {
        funcion.agregarObservador(cliente);
        funcion.cancelar();
        assertEquals(1, cliente.contarNotificacionesNoLeidas());
        assertThrows(OperacionNoPermitidaException.class, funcion::cancelar);
    }

    // ---------- Composite y stock ----------

    @Test
    void comboDescuentaElStockDeSusProductos() throws Exception {
        Producto crispetas = new Producto("PR1", "Crispetas", 10_000, 3, 80);
        Producto gaseosa = new Producto("PR2", "Gaseosa", 6_000, 10, 50);
        Combo combo = new Combo("CO1", "Combo pareja", 28_000, 200);
        combo.agregarItem(crispetas);
        combo.agregarItem(crispetas);
        combo.agregarItem(gaseosa);

        assertTrue(combo.hayStock(1));
        assertFalse(combo.hayStock(2)); // necesita 4 crispetas y hay 3
        combo.descontarStock(1);
        assertEquals(1, crispetas.getStock());
        assertEquals(9, gaseosa.getStock());
        assertThrows(StockInsuficienteException.class, () -> combo.descontarStock(1));
        assertThrows(DatosInvalidosException.class, () -> combo.agregarItem(combo));
    }

    // ---------- Reglas 2 y 3: tarjeta ----------

    @Test
    void pagoValidaTarjetaActivaYSaldo() throws Exception {
        TarjetaVirtual t = cliente.obtenerTarjeta();
        assertThrows(SaldoInsuficienteException.class, () -> t.debitar(60_000, "x", AHORA));
        t.debitar(20_000, "Compra", AHORA);
        assertEquals(30_000, t.getSaldo());
        assertEquals(TipoMovimiento.PAGO, t.obtenerMovimientos().get(0).getTipo());

        t.bloquear();
        assertThrows(TarjetaInactivaException.class, () -> t.debitar(1_000, "x", AHORA));
        assertThrows(TarjetaInactivaException.class, () -> t.acreditar(1_000, TipoMovimiento.RECARGA, "x", AHORA));
    }

    @Test
    void administradorRecargaYQuedaElMovimiento() throws Exception {
        Administrador admin = new Administrador("Admin", "1", "admin@uq.edu.co", "admin");
        admin.recargarTarjetaVirtual(cliente.obtenerTarjeta(), 10_000, AHORA);
        assertEquals(60_000, cliente.obtenerTarjeta().getSaldo());
        assertEquals(TipoMovimiento.RECARGA, cliente.obtenerTarjeta().obtenerMovimientos().get(0).getTipo());
    }

    @Test
    void solicitudDeRecargaSoloSeRespondeUnaVez() throws Exception {
        SolicitudRecarga s = new SolicitudRecarga("SR1", 20_000, AHORA, cliente.getCorreoElectronico());
        s.aprobar();
        assertThrows(OperacionNoPermitidaException.class, s::rechazar);
    }

    // ---------- Regla 4: Strategy + State ----------

    @Test
    void politicaDeReembolsoSegunHorasQueFaltan() {
        LocalDateTime inicio = funcion.getFechaHora();
        assertInstanceOf(ReembolsoTotal.class, PoliticaReembolso.seleccionar(
                funcion, MotivoCancelacion.CANCELACION_CLIENTE, inicio.minusHours(25)));
        assertInstanceOf(ReembolsoParcial.class, PoliticaReembolso.seleccionar(
                funcion, MotivoCancelacion.CANCELACION_CLIENTE, inicio.minusHours(24)));
        assertInstanceOf(ReembolsoParcial.class, PoliticaReembolso.seleccionar(
                funcion, MotivoCancelacion.CANCELACION_CLIENTE, inicio.minusHours(2)));
        assertInstanceOf(SinReembolso.class, PoliticaReembolso.seleccionar(
                funcion, MotivoCancelacion.CANCELACION_CLIENTE, inicio.minusMinutes(119)));
        assertInstanceOf(ReembolsoTotal.class, PoliticaReembolso.seleccionar(
                funcion, MotivoCancelacion.FUNCION_CANCELADA, inicio.minusMinutes(10)));
    }

    @Test
    void compraPagadaSeReembolsaUnaSolaVez() throws Exception {
        Compra compra = new CompraBuilder().paraCliente(cliente).enFuncion(funcion).enFecha(AHORA)
                .agregarAsiento("A1").build(); // 12.000 + 2.000 de servicio
        assertEquals(14_000, compra.getTotal());

        compra.pagar();
        assertEquals("Pagada", compra.getNombreEstado());
        assertEquals(36_000, cliente.obtenerTarjeta().getSaldo());
        assertThrows(OperacionNoPermitidaException.class, compra::pagar);

        compra.cancelar(new ReembolsoParcial(0.8), MotivoCancelacion.CANCELACION_CLIENTE, AHORA);
        assertEquals("Cancelada", compra.getNombreEstado());
        assertEquals(47_200, cliente.obtenerTarjeta().getSaldo());
        assertEquals(11_200, compra.getReembolso().getMonto());

        assertThrows(ReembolsoYaRealizadoException.class,
                () -> compra.cancelar(new ReembolsoTotal(), MotivoCancelacion.CANCELACION_CLIENTE, AHORA));
        assertEquals(47_200, cliente.obtenerTarjeta().getSaldo());
    }

    @Test
    void sinReembolsoNoPermiteCancelar() throws Exception {
        Compra compra = new CompraBuilder().paraCliente(cliente).enFuncion(funcion).enFecha(AHORA)
                .agregarAsiento("A1").build();
        compra.pagar();
        assertThrows(CancelacionNoPermitidaException.class,
                () -> compra.cancelar(new SinReembolso(), MotivoCancelacion.CANCELACION_CLIENTE, AHORA));
        assertEquals("Pagada", compra.getNombreEstado());
    }

    // ---------- Regla 5: puntos ----------

    @Test
    void puntosSeRedimenPrimeroLosProximosAVencer() throws Exception {
        CuentaPuntos cuenta = cliente.obtenerCuentaPuntos();
        LotePuntos viejo = cuenta.acumular(30, LocalDate.of(2026, 1, 10), 1);
        LotePuntos nuevo = cuenta.acumular(50, LocalDate.of(2026, 6, 1), 1);
        LocalDate hoy = LocalDate.of(2026, 11, 20);

        cuenta.redimir(40, hoy);
        assertEquals(0, viejo.getPuntosDisponibles());
        assertEquals(40, nuevo.getPuntosDisponibles());
        assertEquals(40, cuenta.obtenerPuntosDisponibles(hoy));
    }

    @Test
    void puntosVencidosNoSePuedenUsar() {
        CuentaPuntos cuenta = cliente.obtenerCuentaPuntos();
        cuenta.acumular(100, LocalDate.of(2025, 11, 1), 1);
        cuenta.acumular(20, LocalDate.of(2026, 10, 1), 1);
        LocalDate hoy = LocalDate.of(2026, 11, 20);

        assertEquals(20, cuenta.obtenerPuntosDisponibles(hoy));
        assertEquals(100, cuenta.obtenerPuntosVencidos(hoy));
        assertThrows(PuntosInsuficientesException.class, () -> cuenta.redimir(50, hoy));
        assertEquals(0, cuenta.obtenerTotalRedimido());
    }

    @Test
    void anularPuntosDeCompraSoloQuitaLoQueQuedaDisponible() throws Exception {
        CuentaPuntos cuenta = cliente.obtenerCuentaPuntos();
        cuenta.acumular(30, LocalDate.of(2026, 9, 1), 1);               // no viene de una compra
        LotePuntos deCompra = cuenta.acumular(50, LocalDate.of(2026, 10, 1), 1, "C-1");
        LocalDate hoy = LocalDate.of(2026, 11, 20);

        cuenta.redimir(40, hoy); // FIFO: 30 del lote viejo y 10 del lote de C-1
        assertEquals(40, deCompra.getPuntosDisponibles());

        assertEquals(40, cuenta.anularPuntosDeCompra("C-1"));
        assertEquals(0, cuenta.obtenerPuntosDisponibles(hoy));
        assertEquals(40, cuenta.obtenerTotalAnulado());
        assertEquals(0, cuenta.anularPuntosDeCompra("C-1")); // anular dos veces no quita más
    }

    // ---------- Compra: validación y cálculos ----------

    @Test
    void compraCalculaDescuentoYTotal() throws Exception {
        Promocion promo = new Promocion("PM1", "10 %", 0.10, AHORA.toLocalDate(), AHORA.toLocalDate().plusDays(5));
        Compra compra = new CompraBuilder().paraCliente(cliente).enFuncion(funcion).enFecha(AHORA)
                .agregarAsiento("A1").agregarAsiento("A2").agregarPromocion(promo).build();

        assertEquals(24_000, compra.getSubtotal());
        assertEquals(2_400, compra.calcularDescuentoTotal());
        assertEquals(23_600, compra.calcularTotal()); // 24.000 − 2.400 + 2.000 de servicio
        assertEquals(compra.calcularTotal(), compra.getTotal());
    }

    @Test
    void compraValidaSuContenido() throws Exception {
        assertThrows(DatosInvalidosException.class, () -> new CompraBuilder().paraCliente(cliente)
                .enFuncion(funcion).enFecha(AHORA).build()); // sin asientos
        Promocion vencida = new Promocion("PM2", "Vencida", 0.2, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        assertThrows(DatosInvalidosException.class, () -> new CompraBuilder().paraCliente(cliente)
                .enFuncion(funcion).enFecha(AHORA).agregarAsiento("A1").agregarPromocion(vencida).build());
    }

    // ---------- Función, película y sala ----------

    @Test
    void funcionSabeSiEstaProgramadaYSiYaComenzo() throws Exception {
        assertTrue(funcion.estaProgramada());
        assertFalse(funcion.yaComenzo(AHORA));
        assertTrue(funcion.yaComenzo(funcion.getFechaHora()));
        assertEquals(12_000, funcion.calcularPrecioEntrada());
        funcion.cancelar();
        assertFalse(funcion.estaProgramada());
    }

    @Test
    void cineEnlazaLaFuncionConSuPeliculaYSalaYNoRetiraPeliculasConFunciones() throws Exception {
        Cine cine = new Cine("CinemaUQ", "1", "Armenia", "1", "c@uq.co");
        Pelicula pelicula = funcion.getPelicula();
        cine.agregarPelicula(pelicula);
        cine.agregarSala(sala);
        cine.programarFuncion(funcion);

        assertEquals(List.of(funcion), pelicula.getListaFunciones());
        assertEquals(List.of(funcion), sala.getListaFunciones());
        assertThrows(OperacionNoPermitidaException.class, () -> cine.retirarPelicula("P1", AHORA));

        pelicula.retirarDeCartelera();
        assertFalse(pelicula.estaEnCartelera());
    }
}
