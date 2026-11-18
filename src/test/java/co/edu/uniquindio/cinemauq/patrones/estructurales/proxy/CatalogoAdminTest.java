package co.edu.uniquindio.cinemauq.patrones.estructurales.proxy;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.exceptions.OperacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.model.Cine;
import co.edu.uniquindio.cinemauq.model.Pelicula;
import co.edu.uniquindio.cinemauq.model.Promocion;
import co.edu.uniquindio.cinemauq.model.Sala;
import co.edu.uniquindio.cinemauq.model.enums.TipoSala;
import co.edu.uniquindio.cinemauq.patrones.creacionales.factorymethod.AdministradorFactory;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Combo;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Producto;
import co.edu.uniquindio.cinemauq.patrones.estructurales.facade.CineFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/** CRUD de catálogo que usan las ventanas de administración (películas, salas, confitería, promociones). */
class CatalogoAdminTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 10, 0);

    private CineFacade facade;
    private ServicioAdministracion admin;

    @BeforeEach
    void setUp() throws Exception {
        Cine cine = new Cine("CinemaUQ", "900.123.456-7", "Armenia", "6067350000", "contacto@cinemauq.co");
        cine.registrarUsuario(new AdministradorFactory().crearAdministrador("Admin", "1", "admin@cinemauq.co", "admin123"));
        ZoneId zona = ZoneId.systemDefault();
        facade = new CineFacade(cine, Clock.fixed(AHORA.atZone(zona).toInstant(), zona));
        facade.registrarCliente("Ana", "100", "ana@correo.com", "1234");
        facade.iniciarSesion("admin@cinemauq.co", "admin123");
        admin = facade.obtenerServicioAdministracion();
    }

    @Test
    void peliculaSeAgregaEditaYSePublicaORetira() throws Exception {
        admin.agregarPelicula(new Pelicula("P1", "Titulo", "sinopsis", 100, "Drama", "+12", null));
        admin.actualizarPelicula("P1", "Nuevo titulo", "otra", 90, "Comedia", "+7", "");
        Pelicula p = admin.consultarPeliculas().get(0);
        assertEquals("Nuevo titulo", p.getTitulo());
        assertEquals(90, p.getDuracionMinutos());
        assertNull(p.getImagenPelicula());

        admin.cambiarCartelera("P1", false);
        assertTrue(facade.consultarCartelera().isEmpty());
        admin.cambiarCartelera("P1", true);
        assertEquals(1, facade.consultarCartelera().size());
    }

    @Test
    void peliculaConDatosInvalidosSeRechaza() {
        assertThrows(DatosInvalidosException.class,
                () -> admin.agregarPelicula(new Pelicula("P1", " ", "s", 100, "Drama", "+12", null)));
        assertThrows(DatosInvalidosException.class,
                () -> admin.agregarPelicula(new Pelicula("P2", "T", "s", 0, "Drama", "+12", null)));
    }

    @Test
    void salaSeCreaConAsientosYSeEditaSuTipo() throws Exception {
        Sala sala = new Sala("S1", "Sala 1", TipoSala.SALA_2D);
        sala.crearAsientos(3, 4, 1);
        admin.agregarSala(sala);
        assertEquals(12, admin.consultarSalas().get(0).obtenerCapacidad());

        admin.actualizarSala("S1", "Sala Premium", TipoSala.SALA_3D);
        assertEquals("Sala Premium", sala.getNombre());
        assertEquals(TipoSala.SALA_3D, sala.getTipo());
        assertThrows(DatosInvalidosException.class, () -> admin.actualizarSala("S1", " ", TipoSala.SALA_2D));
        assertThrows(DatosInvalidosException.class, () -> admin.actualizarSala("S9", "x", TipoSala.SALA_2D));
    }

    @Test
    void productoSeEditaYSeValidan() throws Exception {
        admin.agregarItem(new Producto("PR1", "Crispetas", 12_000, 10, 90));
        admin.actualizarItem("PR1", "Crispetas grandes", 15_000, 120, 25);
        Producto p = (Producto) admin.consultarItems().get(0);
        assertEquals("Crispetas grandes", p.obtenerNombre());
        assertEquals(15_000, p.obtenerPrecio());
        assertEquals(25, p.getStock());

        assertThrows(DatosInvalidosException.class, () -> admin.actualizarItem("PR1", "x", 0, 0, 1));
        assertThrows(DatosInvalidosException.class, () -> admin.actualizarItem("PR1", "x", 100, 0, -1));
        assertThrows(DatosInvalidosException.class,
                () -> admin.agregarItem(new Producto("PR2", "", 1_000, 1, 0)));
    }

    @Test
    void productoDentroDeUnComboNoSePuedeEliminarPeroElComboSi() throws Exception {
        Producto crispetas = new Producto("PR1", "Crispetas", 12_000, 10, 90);
        Combo combo = new Combo("CO1", "Combo", 20_000, 150);
        combo.agregarItem(crispetas);
        admin.agregarItem(crispetas);
        admin.agregarItem(combo);

        assertThrows(OperacionNoPermitidaException.class, () -> admin.eliminarItem("PR1"));
        admin.eliminarItem("CO1");
        admin.eliminarItem("PR1");
        assertTrue(admin.consultarItems().isEmpty());
    }

    @Test
    void comboIgnoraElStockAlEditar() throws Exception {
        Producto crispetas = new Producto("PR1", "Crispetas", 12_000, 10, 90);
        Combo combo = new Combo("CO1", "Combo", 20_000, 150);
        combo.agregarItem(crispetas);
        admin.agregarItem(crispetas);
        admin.agregarItem(combo);
        admin.actualizarItem("CO1", "Combo grande", 22_000, 160, 999);
        assertEquals("Combo grande", combo.obtenerNombre());
        assertEquals(10, crispetas.getStock());
    }

    @Test
    void promocionSeEditaYSeElimina() throws Exception {
        LocalDate hoy = AHORA.toLocalDate();
        admin.agregarPromocion(new Promocion("PM1", "Promo", 0.10, hoy, hoy.plusDays(5)));
        admin.actualizarPromocion("PM1", "Promo 25", 0.25, hoy, hoy.plusDays(10));
        Promocion p = admin.consultarPromociones().get(0);
        assertEquals(0.25, p.getPorcentajeDescuento());
        assertEquals(hoy.plusDays(10), p.getFechaFin());

        assertThrows(DatosInvalidosException.class,
                () -> admin.actualizarPromocion("PM1", "x", 0.2, hoy, hoy.minusDays(1)));
        assertThrows(DatosInvalidosException.class,
                () -> admin.actualizarPromocion("PM1", "x", 1.5, hoy, hoy.plusDays(1)));
        assertEquals("Promo 25", p.getNombre());

        admin.eliminarPromocion("PM1");
        assertTrue(admin.consultarPromociones().isEmpty());
    }

    @Test
    void soloElAdministradorPuedeModificarElCatalogo() throws Exception {
        facade.cerrarSesion();
        facade.iniciarSesion("ana@correo.com", "1234");
        assertThrows(OperacionNoPermitidaException.class, () -> admin.consultarItems());
        assertThrows(OperacionNoPermitidaException.class, () -> admin.eliminarPromocion("PM1"));
        assertThrows(OperacionNoPermitidaException.class,
                () -> admin.actualizarSala("S1", "x", TipoSala.SALA_2D));
    }
}
