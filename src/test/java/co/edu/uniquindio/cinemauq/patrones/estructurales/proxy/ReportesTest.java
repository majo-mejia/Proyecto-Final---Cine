package co.edu.uniquindio.cinemauq.patrones.estructurales.proxy;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.exceptions.OperacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.model.Cine;
import co.edu.uniquindio.cinemauq.model.Cliente;
import co.edu.uniquindio.cinemauq.model.Compra;
import co.edu.uniquindio.cinemauq.model.Funcion;
import co.edu.uniquindio.cinemauq.model.Pelicula;
import co.edu.uniquindio.cinemauq.model.Reporte;
import co.edu.uniquindio.cinemauq.model.Sala;
import co.edu.uniquindio.cinemauq.model.enums.MetodoPago;
import co.edu.uniquindio.cinemauq.model.enums.TipoReporte;
import co.edu.uniquindio.cinemauq.model.enums.TipoSala;
import co.edu.uniquindio.cinemauq.patrones.creacionales.factorymethod.AdministradorFactory;
import co.edu.uniquindio.cinemauq.patrones.estructurales.adapter.ExportadorPDFAdapter;
import co.edu.uniquindio.cinemauq.patrones.estructurales.adapter.ExportadorTXT;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Combo;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Producto;
import co.edu.uniquindio.cinemauq.patrones.estructurales.facade.CineFacade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Escenario del 2026-11-20 (cliente Ana, recargada con $100.000):
 * - Compra 1, tarjeta: A1, A2 + combo = $46.000 → gana 46 puntos.
 * - Compra 2, tarjeta: B1 + crispetas = $24.000 → gana 24 puntos y luego la cancela (100 %).
 * - Compra 3, puntos: C1 = 120 puntos (tenía 500 ganados el 2026-10-01, fuera del periodo).
 * - Una solicitud de recarga pendiente de $30.000.
 */
class ReportesTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 10, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();

    private CineFacade facade;
    private ServicioAdministracion admin;

    @BeforeEach
    void setUp() throws Exception {
        Cine cine = new Cine("CinemaUQ", "900.123.456-7", "Armenia", "6067350000", "contacto@cinemauq.co");
        cine.registrarUsuario(new AdministradorFactory()
                .crearAdministrador("Admin", "1", "admin@cinemauq.co", "admin123"));
        ZoneId zona = ZoneId.systemDefault();
        facade = new CineFacade(cine, Clock.fixed(AHORA.atZone(zona).toInstant(), zona));
        admin = facade.obtenerServicioAdministracion();

        facade.iniciarSesion("admin@cinemauq.co", "admin123");
        Sala sala = new Sala("S1", "Sala 1", TipoSala.SALA_2D);
        sala.crearAsientos(3, 4, 0);
        admin.agregarSala(sala);
        admin.agregarPelicula(new Pelicula("P1", "Dune", "", 100, "Ciencia ficción", "+12", null));
        Funcion funcion = admin.programarFuncion("P1", "S1", AHORA.plusDays(3), 12_000);
        Producto crispetas = new Producto("PR1", "Crispetas", 10_000, 10, 80);
        Producto gaseosa = new Producto("PR2", "Gaseosa", 6_000, 10, 50);
        Combo combo = new Combo("CO1", "Combo", 20_000, 200);
        combo.agregarItem(crispetas);
        combo.agregarItem(gaseosa);
        admin.agregarItem(crispetas);
        admin.agregarItem(gaseosa);
        admin.agregarItem(combo);
        facade.cerrarSesion();

        Cliente ana = facade.registrarCliente("Ana", "100", "ana@uq.edu.co", "1234");
        facade.iniciarSesion("admin@cinemauq.co", "admin123");
        admin.recargarTarjeta("100", 100_000);
        facade.cerrarSesion();

        facade.iniciarSesion("ana@uq.edu.co", "1234");
        facade.comprar(funcion, List.of("A1", "A2"), Map.of(combo, 1), List.of(), MetodoPago.TARJETA);
        Compra cancelada = facade.comprar(funcion, List.of("B1"), Map.of(crispetas, 1), List.of(), MetodoPago.TARJETA);
        facade.cancelarCompra(cancelada);
        ana.obtenerCuentaPuntos().acumular(500, LocalDate.of(2026, 10, 1), 1);
        facade.comprar(funcion, List.of("C1"), Map.<ItemVendible, Integer>of(), List.of(), MetodoPago.PUNTOS);
        facade.solicitarRecarga(30_000);
        facade.cerrarSesion();

        facade.iniciarSesion("admin@cinemauq.co", "admin123");
    }

    private Reporte reporteDeHoy(TipoReporte tipo) throws Exception {
        return admin.generarReporte(tipo, HOY, HOY);
    }

    @Test
    void ventasSeparaBrutoReembolsosYNeto() throws Exception {
        Reporte r = reporteDeHoy(TipoReporte.VENTAS);

        assertEquals(1, r.getRegistros().size()); // un solo día
        assertEquals(Reporte.dinero(70_000), r.getTotales().get("Ventas brutas"));
        assertEquals(Reporte.dinero(24_000), r.getTotales().get("Reembolsos"));
        assertEquals(Reporte.dinero(46_000), r.getTotales().get("Ventas netas"));
        assertEquals("1", r.getTotales().get("Compras pagadas con puntos"));
        assertEquals(46_000, admin.obtenerTotalVentas(HOY.atStartOfDay(), HOY.atTime(23, 59)));
        assertEquals(46_000, r.getDatosGrafico().get("2026-11-20"));
    }

    @Test
    void entradasCuentaSoloComprasVigentes() throws Exception {
        Reporte r = reporteDeHoy(TipoReporte.ENTRADAS_POR_PELICULA);

        assertEquals(1, r.getRegistros().size()); // una función
        assertEquals("3", r.getTotales().get("Entradas vendidas")); // A1, A2 y C1; B1 se canceló
        assertEquals(Reporte.dinero(24_000), r.getTotales().get("Valor de entradas con tarjeta"));
        assertEquals(3, r.getDatosGrafico().get("Dune"));
    }

    @Test
    void ocupacionUsaLaFechaDeLaFuncion() throws Exception {
        assertTrue(reporteDeHoy(TipoReporte.OCUPACION).getRegistros().isEmpty()); // la función es el 23

        Reporte r = admin.generarReporte(TipoReporte.OCUPACION, HOY, HOY.plusDays(10));
        assertEquals("3", r.getTotales().get("Asientos ocupados"));
        assertEquals("12", r.getTotales().get("Capacidad ofrecida"));
        assertEquals(25.0, r.getDatosGrafico().get("Sala 1"));
    }

    @Test
    void productosNoCuentaLosDeComprasCanceladas() throws Exception {
        Reporte r = reporteDeHoy(TipoReporte.PRODUCTOS_Y_COMBOS);

        assertEquals(List.of(List.of("Combo", "Combo", "1", Reporte.dinero(20_000))), r.getRegistros());
        assertEquals("0", r.getTotales().get("Productos vendidos"));
    }

    @Test
    void recargasIncluyeMovimientosYSolicitudes() throws Exception {
        Reporte r = reporteDeHoy(TipoReporte.RECARGAS);

        assertEquals("1", r.getTotales().get("Recargas realizadas"));
        assertEquals(Reporte.dinero(100_000), r.getTotales().get("Monto recargado"));
        assertEquals("1", r.getTotales().get("Solicitudes recibidas"));
        assertEquals("1", r.getTotales().get("  Pendiente"));
    }

    @Test
    void cancelacionesMuestraMotivoYMonto() throws Exception {
        Reporte r = reporteDeHoy(TipoReporte.CANCELACIONES_Y_REEMBOLSOS);

        assertEquals(1, r.getRegistros().size());
        assertEquals("Cliente", r.getRegistros().get(0).get(3));
        assertEquals(Reporte.dinero(24_000), r.getTotales().get("Dinero reembolsado"));
        assertEquals(24_000, admin.obtenerTotalReembolsos());
    }

    @Test
    void puntosGanadosRedimidosYAnulados() throws Exception {
        Reporte r = reporteDeHoy(TipoReporte.PUNTOS);

        // Ganados 46 + 24; redimidos 120; anulados 24 (compra cancelada); disponibles 46 + 380
        assertEquals(List.of(List.of("Ana", "100", "70", "0", "120", "24", "426")), r.getRegistros());
        assertEquals(24, r.getDatosGrafico().get("Anulados"));
    }

    @Test
    void elReporteTieneEncabezadoTablaYTotales() throws Exception {
        List<String> lineas = reporteDeHoy(TipoReporte.VENTAS).obtenerLineas();

        assertTrue(lineas.get(0).startsWith("Fecha de generación: 2026-11-20 10:00"));
        assertEquals("Periodo: 2026-11-20 a 2026-11-20", lineas.get(1));
        assertTrue(lineas.get(3).startsWith("Fecha"));
        assertTrue(lineas.contains("TOTALES"));

        List<String> vacio = admin.generarReporte(TipoReporte.VENTAS, HOY.minusDays(30), HOY.minusDays(20)).obtenerLineas();
        assertTrue(vacio.contains("Sin registros en el periodo."));
    }

    @Test
    void seExportaEnTXTyPDF(@TempDir Path carpeta) throws Exception {
        Reporte r = reporteDeHoy(TipoReporte.PUNTOS);

        Path txt = r.exportar(new ExportadorTXT(), carpeta.resolve("puntos"));
        Path pdf = r.exportar(new ExportadorPDFAdapter(), carpeta.resolve("puntos"));
        assertEquals(TipoReporte.PUNTOS.getTitulo(), Files.readAllLines(txt).get(0));
        assertTrue(Files.size(pdf) > 0);
    }

    @Test
    void validaRangoYPermisos() throws Exception {
        assertThrows(DatosInvalidosException.class, () -> admin.generarReporte(TipoReporte.VENTAS, HOY, HOY.minusDays(1)));

        facade.cerrarSesion();
        facade.iniciarSesion("ana@uq.edu.co", "1234");
        assertThrows(OperacionNoPermitidaException.class, () -> admin.generarReporte(TipoReporte.VENTAS, HOY, HOY));
    }
}
