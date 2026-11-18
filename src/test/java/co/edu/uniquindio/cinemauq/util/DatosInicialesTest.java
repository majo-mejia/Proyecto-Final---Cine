package co.edu.uniquindio.cinemauq.util;

import co.edu.uniquindio.cinemauq.model.Cine;
import co.edu.uniquindio.cinemauq.model.Funcion;
import co.edu.uniquindio.cinemauq.model.Pelicula;
import co.edu.uniquindio.cinemauq.model.Reporte;
import co.edu.uniquindio.cinemauq.model.enums.EstadoSolicitudRecarga;
import co.edu.uniquindio.cinemauq.model.enums.TipoReporte;
import co.edu.uniquindio.cinemauq.patrones.estructurales.facade.CineFacade;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracion;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Los datos iniciales se crean sin errores y alcanzan para demostrar cartelera, compras y reportes. */
class DatosInicialesTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 18, 21, 30);

    private static Clock reloj() {
        ZoneId zona = ZoneId.systemDefault();
        return Clock.fixed(AHORA.atZone(zona).toInstant(), zona);
    }

    @Test
    void creaCarteleraConFuncionesYAsientosVendidos() throws Exception {
        CineFacade fachada = new CineFacade(DatosIniciales.crearCine(reloj()), reloj());
        fachada.iniciarSesion(DatosIniciales.CORREO_CLIENTE, DatosIniciales.CONTRASENA_CLIENTE);

        List<Pelicula> cartelera = fachada.consultarCartelera();
        assertEquals(List.of("Beyond Horizons", "Future We Build"), cartelera.stream().map(Pelicula::getTitulo).toList());
        List<Funcion> funciones = fachada.consultarFunciones(cartelera.get(0));
        assertEquals(6, funciones.size()); // 2 por día durante 3 días
        assertTrue(funciones.stream().anyMatch(f -> !f.estaDisponible("B4")));
        assertTrue(fachada.consultarSaldo() > 100_000);
    }

    @Test
    void dejaHistoriaParaLosReportesYUnaSolicitudPendiente() throws Exception {
        Cine cine = DatosIniciales.crearCine(reloj());
        CineFacade fachada = new CineFacade(cine, reloj());
        fachada.iniciarSesion(DatosIniciales.CORREO_ADMIN, DatosIniciales.CONTRASENA_ADMIN);
        ServicioAdministracion admin = fachada.obtenerServicioAdministracion();
        LocalDate hoy = AHORA.toLocalDate();

        Reporte ventas = admin.generarReporte(TipoReporte.VENTAS, hoy.minusDays(6), hoy);
        assertEquals(7, ventas.getRegistros().size()); // 6 días de historia + las ventas de hoy
        assertEquals(1, admin.generarReporte(TipoReporte.CANCELACIONES_Y_REEMBOLSOS, hoy.minusDays(6), hoy)
                .getRegistros().size());
        assertEquals("1", ventas.getTotales().get("Compras pagadas con puntos"));
        assertEquals(1, admin.consultarSolicitudesRecarga().stream()
                .filter(s -> s.getEstado() == EstadoSolicitudRecarga.PENDIENTE).count());
    }
}
