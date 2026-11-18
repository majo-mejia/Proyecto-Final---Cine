package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.model.enums.*;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Combo;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Calcula los 7 reportes a partir de las listas del Cine. Está separado de
 * ServicioAdministracionReal para que ese servicio no crezca con la lógica de cada reporte (SRP).
 *
 * Periodo: [desde 00:00, hasta 23:59:59]. Cada reporte filtra por la fecha que le corresponde:
 * la de la compra, la de la función, la del movimiento, la del reembolso o la del lote de puntos.
 */
public class GeneradorReportes {

    private final Cine cine;

    public GeneradorReportes(Cine cine) {
        this.cine = cine;
    }

    public Reporte generar(TipoReporte tipo, LocalDate desde, LocalDate hasta, LocalDateTime ahora)
            throws DatosInvalidosException {
        if (tipo == null || desde == null || hasta == null) {
            throw new DatosInvalidosException("Debe elegir el tipo de reporte y el rango de fechas.");
        }
        if (desde.isAfter(hasta)) {
            throw new DatosInvalidosException("La fecha inicial no puede ser posterior a la final.");
        }
        Periodo periodo = new Periodo(desde, hasta);
        return switch (tipo) {
            case VENTAS -> ventas(periodo, ahora);
            case ENTRADAS_POR_PELICULA -> entradasPorPelicula(periodo, ahora);
            case OCUPACION -> ocupacion(periodo, ahora);
            case PRODUCTOS_Y_COMBOS -> productosYCombos(periodo, ahora);
            case RECARGAS -> recargas(periodo, ahora);
            case CANCELACIONES_Y_REEMBOLSOS -> cancelacionesYReembolsos(periodo, ahora);
            case PUNTOS -> puntos(periodo, ahora);
        };
    }

    /** Rango de fechas del reporte. */
    private record Periodo(LocalDate desde, LocalDate hasta) {
        boolean contiene(LocalDateTime momento) {
            return contiene(momento.toLocalDate());
        }

        boolean contiene(LocalDate fecha) {
            return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
        }
    }

    private Reporte nuevo(TipoReporte tipo, Periodo periodo, LocalDateTime ahora, String... columnas) {
        return new Reporte(tipo, ahora, periodo.desde(), periodo.hasta(), List.of(columnas));
    }

    private List<Compra> comprasEn(Periodo periodo) {
        return cine.getListaCompras().stream().filter(c -> periodo.contiene(c.getFechaHora())).toList();
    }

    private static double reembolsado(Compra compra) {
        return compra.getReembolso() == null ? 0 : compra.getReembolso().getMonto();
    }

    // ---------------- 1. Ventas por periodo ----------------

    /**
     * Una fila por día con las compras pagadas con tarjeta (las de puntos no mueven dinero).
     * Neto = lo cobrado menos lo reembolsado de esas compras.
     */
    private Reporte ventas(Periodo periodo, LocalDateTime ahora) {
        Reporte r = nuevo(TipoReporte.VENTAS, periodo, ahora, "Fecha", "Compras", "Entradas", "Bruto", "Reembolsos", "Neto");
        r.definirGrafico("Ventas netas por día");

        Map<LocalDate, List<Compra>> porDia = new TreeMap<>();
        int comprasConPuntos = 0;
        int puntosRedimidos = 0;
        for (Compra c : comprasEn(periodo)) {
            if (c.getMetodo() == MetodoPago.TARJETA) {
                porDia.computeIfAbsent(c.getFechaHora().toLocalDate(), d -> new ArrayList<>()).add(c);
            } else {
                comprasConPuntos++;
                puntosRedimidos += c.getPuntosUtilizados();
            }
        }

        int compras = 0;
        double bruto = 0;
        double reembolsos = 0;
        for (Map.Entry<LocalDate, List<Compra>> dia : porDia.entrySet()) {
            List<Compra> lista = dia.getValue();
            int entradas = lista.stream().mapToInt(c -> c.getListaEntradas().size()).sum();
            double brutoDia = lista.stream().mapToDouble(Compra::getTotal).sum();
            double reembolsoDia = lista.stream().mapToDouble(GeneradorReportes::reembolsado).sum();
            String fecha = dia.getKey().format(Reporte.FORMATO_FECHA);
            r.agregarRegistro(fecha, String.valueOf(lista.size()), String.valueOf(entradas),
                    Reporte.dinero(brutoDia), Reporte.dinero(reembolsoDia), Reporte.dinero(brutoDia - reembolsoDia));
            r.agregarDatoGrafico(fecha, brutoDia - reembolsoDia);
            compras += lista.size();
            bruto += brutoDia;
            reembolsos += reembolsoDia;
        }
        r.agregarTotal("Compras con tarjeta", String.valueOf(compras));
        r.agregarTotal("Ventas brutas", Reporte.dinero(bruto));
        r.agregarTotal("Reembolsos", Reporte.dinero(reembolsos));
        r.agregarTotal("Ventas netas", Reporte.dinero(bruto - reembolsos));
        r.agregarTotal("Compras pagadas con puntos", String.valueOf(comprasConPuntos));
        r.agregarTotal("Puntos redimidos en compras", String.valueOf(puntosRedimidos));
        return r;
    }

    // ---------------- 2. Entradas por película y función ----------------

    /** Entradas de compras vigentes (no canceladas), agrupadas por función. */
    private Reporte entradasPorPelicula(Periodo periodo, LocalDateTime ahora) {
        Reporte r = nuevo(TipoReporte.ENTRADAS_POR_PELICULA, periodo, ahora,
                "Película", "Sala", "Función", "Entradas", "Valor");
        r.definirGrafico("Entradas vendidas por película");

        Map<Funcion, int[]> entradasPorFuncion = new LinkedHashMap<>();
        Map<Funcion, double[]> valorPorFuncion = new LinkedHashMap<>();
        for (Compra c : comprasEn(periodo)) {
            if (!c.estaPagada()) continue;
            entradasPorFuncion.computeIfAbsent(c.getFuncion(), f -> new int[1])[0] += c.getListaEntradas().size();
            if (c.getMetodo() == MetodoPago.TARJETA) {
                valorPorFuncion.computeIfAbsent(c.getFuncion(), f -> new double[1])[0] +=
                        c.getListaEntradas().stream().mapToDouble(Entrada::getPrecio).sum();
            }
        }

        List<Funcion> funciones = new ArrayList<>(entradasPorFuncion.keySet());
        funciones.sort(Comparator.comparing((Funcion f) -> f.getPelicula().getTitulo()).thenComparing(Funcion::getFechaHora));
        int totalEntradas = 0;
        double totalValor = 0;
        for (Funcion f : funciones) {
            int entradas = entradasPorFuncion.get(f)[0];
            double valor = valorPorFuncion.getOrDefault(f, new double[1])[0];
            r.agregarRegistro(f.getPelicula().getTitulo(), f.getSala().getNombre(),
                    f.getFechaHora().format(Reporte.FORMATO_FECHA_HORA), String.valueOf(entradas), Reporte.dinero(valor));
            r.agregarDatoGrafico(f.getPelicula().getTitulo(), entradas);
            totalEntradas += entradas;
            totalValor += valor;
        }
        r.agregarTotal("Funciones con ventas", String.valueOf(funciones.size()));
        r.agregarTotal("Entradas vendidas", String.valueOf(totalEntradas));
        r.agregarTotal("Valor de entradas con tarjeta", Reporte.dinero(totalValor));
        return r;
    }

    // ---------------- 3. Ocupación de salas ----------------

    /** Funciones programadas cuya fecha cae en el periodo, con sus asientos ocupados. */
    private Reporte ocupacion(Periodo periodo, LocalDateTime ahora) {
        Reporte r = nuevo(TipoReporte.OCUPACION, periodo, ahora,
                "Sala", "Película", "Función", "Ocupados", "Capacidad", "Ocupación");
        r.definirGrafico("Ocupación promedio por sala (%)");

        List<Funcion> funciones = cine.getListaFunciones().stream()
                .filter(f -> f.estaProgramada() && periodo.contiene(f.getFechaHora()))
                .sorted(Comparator.comparing(Funcion::getFechaHora))
                .toList();
        long ocupados = 0;
        long capacidad = 0;
        Map<String, List<Double>> porcentajesPorSala = new LinkedHashMap<>();
        for (Funcion f : funciones) {
            int capacidadFuncion = f.getSala().obtenerCapacidad();
            r.agregarRegistro(f.getSala().getNombre(), f.getPelicula().getTitulo(),
                    f.getFechaHora().format(Reporte.FORMATO_FECHA_HORA), String.valueOf(f.contarOcupados()),
                    String.valueOf(capacidadFuncion), Reporte.porcentaje(f.obtenerPorcentajeOcupacion()));
            porcentajesPorSala.computeIfAbsent(f.getSala().getNombre(), s -> new ArrayList<>())
                    .add(f.obtenerPorcentajeOcupacion());
            ocupados += f.contarOcupados();
            capacidad += capacidadFuncion;
        }
        porcentajesPorSala.forEach((sala, lista) ->
                r.agregarDatoGrafico(sala, lista.stream().mapToDouble(Double::doubleValue).average().orElse(0)));

        r.agregarTotal("Funciones", String.valueOf(funciones.size()));
        r.agregarTotal("Asientos ocupados", String.valueOf(ocupados));
        r.agregarTotal("Capacidad ofrecida", String.valueOf(capacidad));
        r.agregarTotal("Ocupación general", Reporte.porcentaje(capacidad == 0 ? 0 : ocupados * 100.0 / capacidad));
        return r;
    }

    // ---------------- 4. Productos y combos vendidos ----------------

    /** Líneas de confitería de compras vigentes, agrupadas por ítem y ordenadas por unidades. */
    private Reporte productosYCombos(Periodo periodo, LocalDateTime ahora) {
        Reporte r = nuevo(TipoReporte.PRODUCTOS_Y_COMBOS, periodo, ahora, "Ítem", "Tipo", "Unidades", "Valor");
        r.definirGrafico("Unidades vendidas por ítem");

        Map<ItemVendible, int[]> unidades = new LinkedHashMap<>();
        Map<ItemVendible, double[]> valor = new LinkedHashMap<>();
        for (Compra c : comprasEn(periodo)) {
            if (!c.estaPagada()) continue;
            for (LineaConfiteria linea : c.getListaLineasConfiteria()) {
                unidades.computeIfAbsent(linea.getItem(), i -> new int[1])[0] += linea.getCantidad();
                if (c.getMetodo() == MetodoPago.TARJETA) {
                    valor.computeIfAbsent(linea.getItem(), i -> new double[1])[0] += linea.getSubtotal();
                }
            }
        }

        List<ItemVendible> items = new ArrayList<>(unidades.keySet());
        items.sort(Comparator.comparingInt((ItemVendible i) -> unidades.get(i)[0]).reversed());
        int totalProductos = 0;
        int totalCombos = 0;
        double totalValor = 0;
        for (ItemVendible item : items) {
            int cantidad = unidades.get(item)[0];
            double v = valor.getOrDefault(item, new double[1])[0];
            boolean esCombo = item instanceof Combo;
            r.agregarRegistro(item.obtenerNombre(), esCombo ? "Combo" : "Producto", String.valueOf(cantidad),
                    Reporte.dinero(v));
            r.agregarDatoGrafico(item.obtenerNombre(), cantidad);
            if (esCombo) totalCombos += cantidad; else totalProductos += cantidad;
            totalValor += v;
        }
        r.agregarTotal("Productos vendidos", String.valueOf(totalProductos));
        r.agregarTotal("Combos vendidos", String.valueOf(totalCombos));
        r.agregarTotal("Valor vendido con tarjeta (antes de promociones)", Reporte.dinero(totalValor));
        return r;
    }

    // ---------------- 5. Recargas ----------------

    /** Movimientos RECARGA de todas las tarjetas en el periodo, más el estado de las solicitudes. */
    private Reporte recargas(Periodo periodo, LocalDateTime ahora) {
        Reporte r = nuevo(TipoReporte.RECARGAS, periodo, ahora, "Fecha", "Cliente", "Documento", "Tarjeta", "Monto");
        r.definirGrafico("Monto recargado por día");

        record Fila(Cliente cliente, MovimientoTarjeta movimiento) { }
        List<Fila> filas = new ArrayList<>();
        for (Cliente cliente : cine.obtenerClientes()) {
            for (MovimientoTarjeta m : cliente.obtenerTarjeta().obtenerMovimientos()) {
                if (m.getTipo() == TipoMovimiento.RECARGA && periodo.contiene(m.getFechaHora())) {
                    filas.add(new Fila(cliente, m));
                }
            }
        }
        filas.sort(Comparator.comparing(f -> f.movimiento().getFechaHora()));

        double total = 0;
        for (Fila f : filas) {
            MovimientoTarjeta m = f.movimiento();
            r.agregarRegistro(m.getFechaHora().format(Reporte.FORMATO_FECHA_HORA), f.cliente().getNombre(),
                    f.cliente().getDocumento(), f.cliente().obtenerTarjeta().getNumero(), Reporte.dinero(m.getMonto()));
            r.agregarDatoGrafico(m.getFechaHora().toLocalDate().format(Reporte.FORMATO_FECHA), m.getMonto());
            total += m.getMonto();
        }

        List<SolicitudRecarga> solicitudes = cine.getListaSolicitudesRecarga().stream()
                .filter(s -> periodo.contiene(s.getFechaSolicitud())).toList();
        r.agregarTotal("Recargas realizadas", String.valueOf(filas.size()));
        r.agregarTotal("Monto recargado", Reporte.dinero(total));
        r.agregarTotal("Solicitudes recibidas", String.valueOf(solicitudes.size()));
        for (EstadoSolicitudRecarga estado : EstadoSolicitudRecarga.values()) {
            long cantidad = solicitudes.stream().filter(s -> s.getEstado() == estado).count();
            r.agregarTotal("  " + estado.name().charAt(0) + estado.name().substring(1).toLowerCase(), String.valueOf(cantidad));
        }
        return r;
    }

    // ---------------- 6. Cancelaciones y reembolsos ----------------

    /**
     * Compras cuyo reembolso ocurrió en el periodo. Si se pagaron con puntos, lo devuelto son
     * puntos y no dinero, así que se suma aparte.
     */
    private Reporte cancelacionesYReembolsos(Periodo periodo, LocalDateTime ahora) {
        Reporte r = nuevo(TipoReporte.CANCELACIONES_Y_REEMBOLSOS, periodo, ahora,
                "Compra", "Cliente", "Fecha", "Motivo", "%", "Devuelto");
        r.definirGrafico("Cancelaciones por motivo");

        List<Compra> canceladas = cine.getListaCompras().stream()
                .filter(c -> c.getReembolso() != null && periodo.contiene(c.getReembolso().getFechaHora()))
                .sorted(Comparator.comparing(c -> c.getReembolso().getFechaHora()))
                .toList();
        double dinero = 0;
        int puntos = 0;
        int porCliente = 0;
        for (Compra c : canceladas) {
            Reembolso reembolso = c.getReembolso();
            boolean delCliente = reembolso.getMotivo() == MotivoCancelacion.CANCELACION_CLIENTE;
            String motivo = delCliente ? "Cliente" : "Función cancelada";
            String devuelto;
            if (c.getMetodo() == MetodoPago.PUNTOS) {
                int pts = (int) Math.round(c.getPuntosUtilizados() * reembolso.getPorcentaje());
                devuelto = pts + " pts";
                puntos += pts;
            } else {
                devuelto = Reporte.dinero(reembolso.getMonto());
                dinero += reembolso.getMonto();
            }
            r.agregarRegistro(c.getId(), c.getCliente().getNombre(),
                    reembolso.getFechaHora().format(Reporte.FORMATO_FECHA_HORA), motivo,
                    String.valueOf(Math.round(reembolso.getPorcentaje() * 100)), devuelto);
            r.agregarDatoGrafico(motivo, 1);
            if (delCliente) porCliente++;
        }
        r.agregarTotal("Cancelaciones", String.valueOf(canceladas.size()));
        r.agregarTotal("Pedidas por el cliente", String.valueOf(porCliente));
        r.agregarTotal("Por función cancelada", String.valueOf(canceladas.size() - porCliente));
        r.agregarTotal("Dinero reembolsado", Reporte.dinero(dinero));
        r.agregarTotal("Puntos devueltos", String.valueOf(puntos));
        return r;
    }

    // ---------------- 7. Puntos acumulados y redimidos ----------------

    /**
     * Por cliente: ganados (lotes de compras obtenidos en el periodo), devueltos (lotes sin compra,
     * por cancelar compras pagadas con puntos), redimidos (compras con puntos del periodo),
     * anulados (puntos quitados por compras canceladas en el periodo) y disponibles hoy.
     */
    private Reporte puntos(Periodo periodo, LocalDateTime ahora) {
        Reporte r = nuevo(TipoReporte.PUNTOS, periodo, ahora,
                "Cliente", "Documento", "Ganados", "Devueltos", "Redimidos", "Anulados", "Disponibles");
        r.definirGrafico("Movimiento de puntos en el periodo");

        int[] totales = new int[5];
        LocalDate hoy = ahora.toLocalDate();
        for (Cliente cliente : cine.obtenerClientes()) {
            CuentaPuntos cuenta = cliente.obtenerCuentaPuntos();
            int ganados = 0;
            int devueltos = 0;
            for (LotePuntos lote : cuenta.getListaLotes()) {
                if (!periodo.contiene(lote.getFechaObtencion())) continue;
                if (lote.getIdCompra() != null) ganados += lote.getPuntosObtenidos();
                else devueltos += lote.getPuntosObtenidos();
            }
            int redimidos = 0;
            int anulados = 0;
            for (Compra c : cliente.consultarHistorialCompras()) {
                if (c.getMetodo() == MetodoPago.PUNTOS && periodo.contiene(c.getFechaHora())) {
                    redimidos += c.getPuntosUtilizados();
                }
                if (c.getMetodo() == MetodoPago.TARJETA && c.getReembolso() != null
                        && periodo.contiene(c.getReembolso().getFechaHora())) {
                    anulados += cuenta.getListaLotes().stream().filter(l -> l.esDeCompra(c.getId()))
                            .mapToInt(LotePuntos::getPuntosAnulados).sum();
                }
            }
            int disponibles = cuenta.obtenerPuntosDisponibles(hoy);
            if (ganados + devueltos + redimidos + anulados + disponibles == 0) continue;

            r.agregarRegistro(cliente.getNombre(), cliente.getDocumento(), String.valueOf(ganados),
                    String.valueOf(devueltos), String.valueOf(redimidos), String.valueOf(anulados),
                    String.valueOf(disponibles));
            int[] fila = {ganados, devueltos, redimidos, anulados, disponibles};
            for (int i = 0; i < fila.length; i++) totales[i] += fila[i];
        }
        r.agregarDatoGrafico("Ganados", totales[0]);
        r.agregarDatoGrafico("Devueltos", totales[1]);
        r.agregarDatoGrafico("Redimidos", totales[2]);
        r.agregarDatoGrafico("Anulados", totales[3]);

        r.agregarTotal("Puntos ganados", String.valueOf(totales[0]));
        r.agregarTotal("Puntos devueltos", String.valueOf(totales[1]));
        r.agregarTotal("Puntos redimidos", String.valueOf(totales[2]));
        r.agregarTotal("Puntos anulados", String.valueOf(totales[3]));
        r.agregarTotal("Puntos disponibles hoy", String.valueOf(totales[4]));
        return r;
    }
}
