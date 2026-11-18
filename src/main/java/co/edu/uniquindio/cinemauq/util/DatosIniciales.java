package co.edu.uniquindio.cinemauq.util;

import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.model.*;
import co.edu.uniquindio.cinemauq.model.enums.MetodoPago;
import co.edu.uniquindio.cinemauq.model.enums.TipoSala;
import co.edu.uniquindio.cinemauq.patrones.creacionales.factorymethod.AdministradorFactory;
import co.edu.uniquindio.cinemauq.patrones.creacionales.singleton.ConfiguracionCine;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Combo;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Producto;
import co.edu.uniquindio.cinemauq.patrones.estructurales.facade.CineFacade;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DatosIniciales {

    public static final String CORREO_ADMIN = "admin@cinemauq.co";
    public static final String CONTRASENA_ADMIN = "admin123";
    public static final String CORREO_CLIENTE = "ana.gomez@correo.com";
    public static final String CONTRASENA_CLIENTE = "1234";

    private static final String[][] CLIENTES = {
            {"Ana Gómez", "1094001001", CORREO_CLIENTE, CONTRASENA_CLIENTE},
            {"Carlos Ruiz", "1094001002", "carlos.ruiz@correo.com", "1234"},
            {"Laura Méndez", "1094001003", "laura.mendez@correo.com", "1234"},
            {"Juan Pérez", "1094001004", "juan.perez@correo.com", "1234"},
    };

    private DatosIniciales() { }


    public static Cine crearCine(Clock reloj) throws CinemaUQException {
        Cine cine = new Cine("CinemaUQ", "900.123.456-7", "Cra. 15 # 12N-20, Armenia", "6067359300",
                "contacto@cinemauq.co");
        cine.registrarUsuario(new AdministradorFactory()
                .crearAdministrador("Administrador CinemaUQ", "1000000000", CORREO_ADMIN, CONTRASENA_ADMIN));

        LocalDate hoy = LocalDate.now(reloj);
        Catalogo catalogo = crearCatalogo(cine, reloj, hoy.minusDays(7).atTime(8, 0));
        crearHistoria(cine, reloj, hoy, catalogo);
        crearCartelera(cine, reloj, hoy, catalogo);
        catalogo.peliculaAntigua().retirarDeCartelera();
        return cine;
    }


    private record Catalogo(Pelicula beyond, Pelicula future, Pelicula peliculaAntigua,
                            Producto crispetas, Producto gaseosa, Producto nachos,
                            Combo comboPareja, Combo comboIndividual, Promocion estudiante) { }

    private static Catalogo crearCatalogo(Cine cine, Clock reloj, LocalDateTime momento) throws CinemaUQException {
        CineFacade fachada = fachadaEn(cine, reloj, momento);
        ServicioAdministracion admin = comoAdministrador(fachada);

        admin.agregarSala(crearSala("S1", "Sala 1", TipoSala.SALA_2D, Set.of("H1", "H2")));
        admin.agregarSala(crearSala("S2", "Sala 2", TipoSala.SALA_3D, Set.of("G7", "G8")));

        Pelicula beyond = new Pelicula("P1", "Beyond Horizons",
                "Una tripulación cruza los límites del sistema solar en busca de un nuevo hogar.",
                135, "Ciencia ficción", "+12", null);
        Pelicula future = new Pelicula("P2", "Future We Build",
                "Tres generaciones de una familia reconstruyen su pueblo después de una inundación.",
                118, "Drama", "+7", null);
        Pelicula antigua = new Pelicula("P3", "Noche de Acción",
                "Un escolta debe proteger a un testigo durante una noche en la ciudad.",
                104, "Acción", "+15", null);
        admin.agregarPelicula(beyond);
        admin.agregarPelicula(future);
        admin.agregarPelicula(antigua);

        Producto crispetas = new Producto("PR1", "Crispetas medianas", 12_000, 300, 90);
        Producto gaseosa = new Producto("PR2", "Gaseosa 16 oz", 7_000, 400, 60);
        Producto nachos = new Producto("PR3", "Nachos con queso", 14_000, 150, 100);
        Producto agua = new Producto("PR4", "Agua 600 ml", 4_000, 200, 30);
        Combo pareja = new Combo("CO1", "Combo Pareja", 30_000, 220);
        pareja.agregarItem(crispetas);
        pareja.agregarItem(crispetas);
        pareja.agregarItem(gaseosa);
        pareja.agregarItem(gaseosa);
        Combo individual = new Combo("CO2", "Combo Individual", 17_000, 130);
        individual.agregarItem(crispetas);
        individual.agregarItem(gaseosa);
        for (ItemVendible item : List.of(crispetas, gaseosa, nachos, agua, pareja, individual)) {
            admin.agregarItem(item);
        }

        LocalDate inicio = momento.toLocalDate();
        Promocion estudiante = new Promocion("PM1", "Estudiante UQ 15 %", 0.15, inicio, inicio.plusDays(45));
        admin.agregarPromocion(estudiante);
        admin.agregarPromocion(new Promocion("PM2", "Martes de cine 20 %", 0.20, inicio, inicio.plusDays(45)));

        for (String[] c : CLIENTES) {
            fachada.registrarCliente(c[0], c[1], c[2], c[3]);
        }
        fachada.iniciarSesion(CORREO_ADMIN, CONTRASENA_ADMIN);

        admin.recargarTarjeta(CLIENTES[0][1], 600_000);
        admin.recargarTarjeta(CLIENTES[1][1], 500_000);
        admin.recargarTarjeta(CLIENTES[2][1], 500_000);
        admin.recargarTarjeta(CLIENTES[3][1], 100_000);
        fachada.cerrarSesion();

        return new Catalogo(beyond, future, antigua, crispetas, gaseosa, nachos, pareja, individual, estudiante);
    }


    private static void crearHistoria(Cine cine, Clock reloj, LocalDate hoy, Catalogo c) throws CinemaUQException {
        for (int d = 6; d >= 1; d--) {
            LocalDate dia = hoy.minusDays(d);
            ServicioAdministracion admin = comoAdministrador(fachadaEn(cine, reloj, dia.atTime(8, 0)));
            Funcion noche = admin.programarFuncion(d % 2 == 0 ? "P1" : "P2", d % 2 == 0 ? "S2" : "S1",
                    dia.atTime(19, 15), d % 2 == 0 ? 18_000 : 14_000);
            Funcion tarde = admin.programarFuncion("P3", d % 2 == 0 ? "S1" : "S2", dia.atTime(15, 0), 12_000);
            Funcion manana = d % 3 == 0 ? admin.programarFuncion("P2", d % 2 == 0 ? "S1" : "S2",
                    dia.atTime(11, 0), 10_000) : null;

            CineFacade fachada = fachadaEn(cine, reloj, dia.atTime(10, 0));
            Asignador asientos = new Asignador();
            for (int k = 0; k < CLIENTES.length - 1; k++) {
                Funcion funcion = (k + d) % 3 == 0 ? tarde : noche;
                int cantidad = 1 + (k + d) % 3;
                Map<ItemVendible, Integer> items = switch ((k + d) % 4) {
                    case 0 -> Map.of(c.comboPareja(), 1);
                    case 1 -> Map.of(c.comboIndividual(), cantidad);
                    case 2 -> Map.of(c.nachos(), 1, c.gaseosa(), 1);
                    default -> Map.of();
                };
                List<Promocion> promos = k == 2 ? List.of(c.estudiante()) : List.of();
                comprar(fachada, CLIENTES[k], funcion, asientos.siguientes(funcion, cantidad), items, promos,
                        MetodoPago.TARJETA);
            }
            if (manana != null) {
                comprar(fachada, CLIENTES[3], manana, asientos.siguientes(manana, 2), Map.of(c.crispetas(), 1),
                        List.of(), MetodoPago.TARJETA);
            }

            if (d == 3) { // Carlos cancela su compra a las 10:30 (faltan menos de 24 h: reembolso del 80 %)
                CineFacade cancelacion = fachadaEn(cine, reloj, dia.atTime(10, 30));
                cancelacion.iniciarSesion(CLIENTES[1][2], CLIENTES[1][3]);
                Compra ultima = ultimaCompra(cancelacion);
                cancelacion.cancelarCompra(ultima);
                cancelacion.cerrarSesion();
            }
            if (d == 1) { // Ana usa sus puntos acumulados en la semana
                fachada.iniciarSesion(CLIENTES[0][2], CLIENTES[0][3]);
                int entradasConPuntos = fachada.consultarPuntos() / ConfiguracionCine.getInstancia().getCostoEntradaEnPuntos();
                fachada.cerrarSesion();
                if (entradasConPuntos > 0) {
                    comprar(fachada, CLIENTES[0], noche, asientos.siguientes(noche, Math.min(entradasConPuntos, 2)),
                            Map.of(), List.of(), MetodoPago.PUNTOS);
                }
            }
        }
    }


    private static void crearCartelera(Cine cine, Clock reloj, LocalDate hoy, Catalogo c) throws CinemaUQException {
        CineFacade fachada = new CineFacade(cine, reloj);
        ServicioAdministracion admin = comoAdministrador(fachada);
        Funcion primeraBeyond = null;
        for (int i = 1; i <= 3; i++) {
            LocalDate dia = hoy.plusDays(i);
            Funcion beyond = admin.programarFuncion("P1", "S2", dia.atTime(19, 15), 18_000);
            admin.programarFuncion("P1", "S2", dia.atTime(15, 0), 15_000);
            admin.programarFuncion("P2", "S1", dia.atTime(20, 30), 14_000);
            admin.programarFuncion("P2", "S1", dia.atTime(16, 30), 12_000);
            if (primeraBeyond == null) primeraBeyond = beyond;
        }
        fachada.cerrarSesion();

        comprar(fachada, CLIENTES[1], primeraBeyond, List.of("B4", "C7"), Map.of(c.comboPareja(), 1), List.of(),
                MetodoPago.TARJETA);
        comprar(fachada, CLIENTES[2], primeraBeyond, List.of("E4", "F6", "H4"), Map.of(), List.of(c.estudiante()),
                MetodoPago.TARJETA);

        fachada.iniciarSesion(CLIENTES[3][2], CLIENTES[3][3]);
        fachada.solicitarRecarga(50_000); // queda pendiente para que el administrador la apruebe
        fachada.cerrarSesion();
    }



    private static CineFacade fachadaEn(Cine cine, Clock reloj, LocalDateTime momento) {
        return new CineFacade(cine, Clock.fixed(momento.atZone(reloj.getZone()).toInstant(), reloj.getZone()));
    }

    private static ServicioAdministracion comoAdministrador(CineFacade fachada) throws CinemaUQException {
        fachada.iniciarSesion(CORREO_ADMIN, CONTRASENA_ADMIN);
        return fachada.obtenerServicioAdministracion();
    }

    private static void comprar(CineFacade fachada, String[] cliente, Funcion funcion, List<String> asientos,
                                Map<ItemVendible, Integer> items, List<Promocion> promos, MetodoPago metodo)
            throws CinemaUQException {
        fachada.iniciarSesion(cliente[2], cliente[3]);
        fachada.comprar(funcion, asientos, items, promos, metodo);
        fachada.cerrarSesion();
    }

    private static Compra ultimaCompra(CineFacade fachada) throws CinemaUQException {
        List<Compra> historial = fachada.consultarHistorialCompras();
        return historial.get(historial.size() - 1);
    }

    private static Sala crearSala(String id, String nombre, TipoSala tipo, Set<String> accesibles)
            throws CinemaUQException {
        Sala sala = new Sala(id, nombre, tipo);
        for (char fila = 'A'; fila <= 'H'; fila++) {
            for (int numero = 1; numero <= 12; numero++) {
                sala.crearAsiento(fila, numero, accesibles.contains("" + fila + numero));
            }
        }
        return sala;
    }


    private static final class Asignador {
        private final Map<Funcion, Integer> usados = new java.util.HashMap<>();

        List<String> siguientes(Funcion funcion, int cantidad) {
            List<String> codigos = new ArrayList<>();
            int inicio = usados.getOrDefault(funcion, 0);
            for (int i = inicio; i < inicio + cantidad; i++) {
                codigos.add("" + (char) ('A' + i / 12) + (i % 12 + 1));
            }
            usados.put(funcion, inicio + cantidad);
            return codigos;
        }
    }
}
