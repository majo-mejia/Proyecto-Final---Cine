package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.model.enums.TipoReporte;
import co.edu.uniquindio.cinemauq.patrones.estructurales.adapter.Exportador;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Resultado de un reporte: título, fecha de generación, periodo, registros (filas de una tabla)
 * y totales. Además guarda los datos que se dibujan en el gráfico JavaFX.
 * No sabe en qué formato se exporta: lo convierte en líneas de texto y se las entrega a
 * cualquier Exportador (TXT o PDF).
 */
public class Reporte {

    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Locale COLOMBIA = Locale.of("es", "CO");

    private final TipoReporte tipo;
    private final LocalDateTime fechaGeneracion;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final List<String> columnas;
    private final List<List<String>> registros = new ArrayList<>();
    private final Map<String, String> totales = new LinkedHashMap<>();
    private String tituloGrafico = "";
    private final Map<String, Double> datosGrafico = new LinkedHashMap<>();

    public Reporte(TipoReporte tipo, LocalDateTime fechaGeneracion, LocalDate desde, LocalDate hasta,
                   List<String> columnas) {
        this.tipo = tipo;
        this.fechaGeneracion = fechaGeneracion;
        this.desde = desde;
        this.hasta = hasta;
        this.columnas = List.copyOf(columnas);
    }

    // ---------------- Construcción (la usa GeneradorReportes) ----------------

    /** Agrega una fila; debe tener un valor por columna. */
    public void agregarRegistro(String... valores) {
        if (valores.length != columnas.size()) {
            throw new IllegalArgumentException("El registro tiene " + valores.length + " valores y el reporte "
                    + columnas.size() + " columnas.");
        }
        registros.add(List.of(valores));
    }

    public void agregarTotal(String nombre, String valor) { totales.put(nombre, valor); }

    public void definirGrafico(String titulo) { this.tituloGrafico = titulo; }

    /** Suma el valor a la etiqueta (si la etiqueta se repite, se acumula). */
    public void agregarDatoGrafico(String etiqueta, double valor) { datosGrafico.merge(etiqueta, valor, Double::sum); }

    // ---------------- Salida ----------------

    /**
     * El reporte como líneas de texto: encabezado (fecha de generación y periodo), tabla de
     * registros con columnas alineadas y la sección de totales.
     */
    public List<String> obtenerLineas() {
        List<String> lineas = new ArrayList<>();
        lineas.add("Fecha de generación: " + fechaGeneracion.format(FORMATO_FECHA_HORA));
        lineas.add("Periodo: " + desde.format(FORMATO_FECHA) + " a " + hasta.format(FORMATO_FECHA));
        lineas.add("");

        if (registros.isEmpty()) {
            lineas.add("Sin registros en el periodo.");
        } else {
            int[] anchos = calcularAnchos();
            boolean[] derecha = columnasNumericas();
            lineas.add(formatearFila(columnas, anchos, derecha));
            int anchoTotal = 0;
            for (int ancho : anchos) anchoTotal += ancho;
            lineas.add("-".repeat(anchoTotal + 2 * (anchos.length - 1)));
            for (List<String> registro : registros) {
                lineas.add(formatearFila(registro, anchos, derecha));
            }
        }

        if (!totales.isEmpty()) {
            lineas.add("");
            lineas.add("TOTALES");
            int anchoNombre = totales.keySet().stream().mapToInt(String::length).max().orElse(0);
            totales.forEach((nombre, valor) -> lineas.add("  " + rellenar(nombre + ":", anchoNombre + 1, false) + " " + valor));
        }
        return lineas;
    }

    /** Exporta con el exportador recibido; el reporte no depende de si es TXT o PDF (DIP). */
    public Path exportar(Exportador exportador, Path destino) throws IOException {
        return exportador.exportar(getTitulo(), obtenerLineas(), destino);
    }

    private int[] calcularAnchos() {
        int[] anchos = new int[columnas.size()];
        for (int i = 0; i < anchos.length; i++) {
            anchos[i] = columnas.get(i).length();
            for (List<String> registro : registros) {
                anchos[i] = Math.max(anchos[i], registro.get(i).length());
            }
        }
        return anchos;
    }

    /** Una columna se alinea a la derecha si todos sus valores son números o dinero. */
    private boolean[] columnasNumericas() {
        boolean[] derecha = new boolean[columnas.size()];
        for (int i = 0; i < derecha.length; i++) {
            final int columna = i;
            derecha[i] = registros.stream().allMatch(r -> r.get(columna).matches("[-$]?[$\\d][\\d.,%\\s]*"));
        }
        return derecha;
    }

    private static String formatearFila(List<String> valores, int[] anchos, boolean[] derecha) {
        StringBuilder fila = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) fila.append("  ");
            fila.append(rellenar(valores.get(i), anchos[i], derecha[i]));
        }
        return fila.toString().stripTrailing();
    }

    private static String rellenar(String texto, int ancho, boolean derecha) {
        String espacios = " ".repeat(Math.max(0, ancho - texto.length()));
        return derecha ? espacios + texto : texto + espacios;
    }

    // ---------------- Formatos comunes ----------------

    public static String dinero(double valor) { return String.format(COLOMBIA, "$%,.0f", valor); }

    public static String porcentaje(double valor) { return String.format(COLOMBIA, "%.1f %%", valor); }

    // ---------------- Getters ----------------

    public TipoReporte getTipo() { return tipo; }
    public String getTitulo() { return tipo.getTitulo(); }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
    public List<String> getColumnas() { return columnas; }
    public List<List<String>> getRegistros() { return Collections.unmodifiableList(registros); }
    public Map<String, String> getTotales() { return Collections.unmodifiableMap(totales); }
    public String getTituloGrafico() { return tituloGrafico; }
    public Map<String, Double> getDatosGrafico() { return Collections.unmodifiableMap(datosGrafico); }

    @Override
    public String toString() { return getTitulo() + " (" + desde + " a " + hasta + ")"; }
}
