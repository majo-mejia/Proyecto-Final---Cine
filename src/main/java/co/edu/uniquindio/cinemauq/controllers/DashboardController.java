package co.edu.uniquindio.cinemauq.controllers;

import co.edu.uniquindio.cinemauq.controllers.admin.ComprasVentana;
import co.edu.uniquindio.cinemauq.controllers.admin.ConfiguracionVentana;
import co.edu.uniquindio.cinemauq.controllers.admin.GestionClientesVentana;
import co.edu.uniquindio.cinemauq.controllers.admin.GestionConfiteriaVentana;
import co.edu.uniquindio.cinemauq.controllers.admin.GestionFuncionesVentana;
import co.edu.uniquindio.cinemauq.controllers.admin.GestionPeliculasVentana;
import co.edu.uniquindio.cinemauq.controllers.admin.GestionPromocionesVentana;
import co.edu.uniquindio.cinemauq.controllers.admin.GestionSalasVentana;
import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.controllers.comun.Vistas;
import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.model.Compra;
import co.edu.uniquindio.cinemauq.model.Funcion;
import co.edu.uniquindio.cinemauq.model.Pelicula;
import co.edu.uniquindio.cinemauq.model.Reporte;
import co.edu.uniquindio.cinemauq.model.enums.MetodoPago;
import co.edu.uniquindio.cinemauq.model.enums.TipoReporte;
import co.edu.uniquindio.cinemauq.patrones.estructurales.adapter.Exportador;
import co.edu.uniquindio.cinemauq.patrones.estructurales.adapter.ExportadorPDFAdapter;
import co.edu.uniquindio.cinemauq.patrones.estructurales.adapter.ExportadorTXT;
import co.edu.uniquindio.cinemauq.patrones.estructurales.facade.CineFacade;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracion;
import co.edu.uniquindio.cinemauq.util.CinemaUQApp;
import co.edu.uniquindio.cinemauq.util.Navigator;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Panel del administrador. Los indicadores y gráficos salen de los reportes y consultas de
 * ServicioAdministracion (el proxy: si la sesión no es de administrador, lanza la excepción).
 * Muestra los últimos 7 días.
 */
public class DashboardController {

    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("EEE d", Locale.of("es", "CO"));
    private static final int DIAS = 7;

    @FXML private ComboBox<TipoReporte> cmbTipoReporte;
    @FXML private BarChart<String, Number> barChartVentas;
    @FXML private AreaChart<String, Number> areaChartOcupacion;
    @FXML private PieChart pieChartGeneros;
    @FXML private PieChart pieChartHorarios;
    @FXML private BarChart<String, Number> barChartAudiencia;

    private final CineFacade fachada = CinemaUQApp.obtenerFachada();
    private final ServicioAdministracion servicio = fachada.obtenerServicioAdministracion();
    /** Contenido del ScrollPane central, donde están las tarjetas y los gráficos. */
    private Parent contenido;
    private boolean menuConectado;

    @FXML
    public void initialize() {
        contenido = Vistas.raiz(cmbTipoReporte);
        cmbTipoReporte.setItems(FXCollections.observableArrayList(TipoReporte.values()));
        cmbTipoReporte.getSelectionModel().selectFirst();
        // Todos los fx:id están dentro del ScrollPane, que no enlaza su contenido con el resto de la
        // vista hasta que la ventana se muestra; el menú lateral se conecta cuando ya hay escena.
        cmbTipoReporte.sceneProperty().addListener((obs, anterior, escena) -> {
            if (escena != null && !menuConectado) {
                conectarMenu(escena.getRoot());
            }
        });
        cargarDashboard();
    }

    /** Los botones del menú sin onAction en el FXML se conectan aquí a las ventanas del administrador. */
    private void conectarMenu(Parent raiz) {
        menuConectado = true;
        agregarOpcionesDeCatalogo(raiz);
        for (var nodo : raiz.lookupAll(".nav-btn")) {
            if (!(nodo instanceof Button boton) || boton.getOnAction() != null) continue;
            String texto = boton.getText();
            if (texto.contains("Overview")) {
                boton.setOnAction(e -> cargarDashboard());
            } else if (texto.contains("Screenings")) {
                boton.setOnAction(e -> { new GestionFuncionesVentana(servicio).mostrar(); cargarDashboard(); });
            } else if (texto.contains("Tickets")) {
                boton.setOnAction(e -> new ComprasVentana(servicio).mostrar());
            } else if (texto.contains("Customers")) {
                boton.setOnAction(e -> { new GestionClientesVentana(servicio).mostrar(); cargarDashboard(); });
            } else if (texto.contains("Settings")) {
                boton.setOnAction(e -> new ConfiguracionVentana().mostrar());
            }
        }
    }

    /**
     * El menú del FXML no trae las opciones del catálogo (películas, salas, confitería y
     * promociones); se agregan aquí justo después de "Screenings", con el mismo estilo.
     */
    private void agregarOpcionesDeCatalogo(Parent raiz) {
        if (!(raiz.lookup(".sidebar") instanceof VBox menu)) return;
        int posicion = menu.getChildren().size();
        for (int i = 0; i < menu.getChildren().size(); i++) {
            if (menu.getChildren().get(i) instanceof Button b && b.getText().contains("Screenings")) {
                posicion = i + 1;
                break;
            }
        }
        Button[] nuevos = {
            opcionMenu("🎞️  Movies", () -> { new GestionPeliculasVentana(servicio).mostrar(); cargarDashboard(); }),
            opcionMenu("🏛️  Rooms", () -> { new GestionSalasVentana(servicio).mostrar(); cargarDashboard(); }),
            opcionMenu("🍿  Concessions", () -> new GestionConfiteriaVentana(servicio).mostrar()),
            opcionMenu("🏷️  Promotions", () -> new GestionPromocionesVentana(servicio).mostrar())
        };
        menu.getChildren().addAll(posicion, List.of(nuevos));
    }

    private Button opcionMenu(String texto, Runnable accion) {
        Button boton = new Button(texto);
        boton.getStyleClass().add("nav-btn");
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setOnAction(e -> accion.run());
        return boton;
    }

    // ---------------- Indicadores y gráficos ----------------

    private void cargarDashboard() {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.minusDays(DIAS - 1);
        try {
            Reporte ventas = servicio.generarReporte(TipoReporte.VENTAS, desde, hoy);
            Reporte entradas = servicio.generarReporte(TipoReporte.ENTRADAS_POR_PELICULA, desde, hoy);
            Reporte ocupacion = servicio.generarReporte(TipoReporte.OCUPACION, desde, hoy);
            List<Pelicula> peliculas = servicio.consultarPeliculas();
            List<Compra> compras = servicio.consultarCompras().stream()
                    .filter(c -> c.estaPagada() && !c.getFechaHora().toLocalDate().isBefore(desde)
                            && !c.getFechaHora().toLocalDate().isAfter(hoy))
                    .toList();

            long enCartelera = peliculas.stream().filter(Pelicula::estaEnCartelera).count();
            ponerTextos(".kpi-title", "Entradas vendidas", "Ventas netas", "Ocupación", "Películas en cartelera");
            ponerTextos(".kpi-value", entradas.getTotales().get("Entradas vendidas"),
                    ventas.getTotales().get("Ventas netas"), ocupacion.getTotales().get("Ocupación general"),
                    String.valueOf(enCartelera));
            ponerTextos(".kpi-trend-up", "Compras vigentes, últimos 7 días", "Pagos con tarjeta menos reembolsos",
                    "Funciones de los últimos 7 días");
            ponerTextos(".kpi-trend-neutral", peliculas.size() + " películas registradas");
            ponerTextos(".meta-badge", "📅 Últimos 7 días", "👤 " + fachada.getUsuarioActual().getNombre());
            ponerTextos(".chart-title", "Ventas netas por día", "Ocupación promedio por día (%)",
                    "Entradas por género", "Ingresos por franja horaria", "Clientes con más entradas");

            graficoVentas(ventas, desde);
            graficoOcupacion(desde, hoy);
            graficoGeneros(entradas, peliculas);
            graficoFranjas(compras);
            graficoClientes(compras);
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo cargar el panel", e.getMessage());
        }
    }

    private void graficoVentas(Reporte ventas, LocalDate desde) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (int i = 0; i < DIAS; i++) {
            LocalDate dia = desde.plusDays(i);
            double valor = ventas.getDatosGrafico().getOrDefault(dia.format(Reporte.FORMATO_FECHA), 0.0);
            serie.getData().add(new XYChart.Data<>(dia.format(FORMATO_DIA), valor));
        }
        reemplazar(barChartVentas, serie);
    }

    private void graficoOcupacion(LocalDate desde, LocalDate hoy) throws CinemaUQException {
        Map<LocalDate, List<Funcion>> porDia = servicio.consultarFunciones().stream()
                .filter(f -> f.estaProgramada())
                .filter(f -> !f.getFechaHora().toLocalDate().isBefore(desde) && !f.getFechaHora().toLocalDate().isAfter(hoy))
                .collect(Collectors.groupingBy(f -> f.getFechaHora().toLocalDate(), TreeMap::new, Collectors.toList()));
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (int i = 0; i < DIAS; i++) {
            LocalDate dia = desde.plusDays(i);
            double promedio = porDia.getOrDefault(dia, List.of()).stream()
                    .mapToDouble(Funcion::obtenerPorcentajeOcupacion).average().orElse(0);
            serie.getData().add(new XYChart.Data<>(dia.format(FORMATO_DIA), promedio));
        }
        areaChartOcupacion.setAnimated(false);
        areaChartOcupacion.getData().setAll(List.of(serie));
    }

    private void graficoGeneros(Reporte entradas, List<Pelicula> peliculas) {
        Map<String, String> generoPorTitulo = peliculas.stream()
                .collect(Collectors.toMap(Pelicula::getTitulo, Pelicula::getGenero, (a, b) -> a));
        Map<String, Double> porGenero = new LinkedHashMap<>();
        entradas.getDatosGrafico().forEach((titulo, cantidad) ->
                porGenero.merge(generoPorTitulo.getOrDefault(titulo, "Otro"), cantidad, Double::sum));
        llenarPastel(pieChartGeneros, porGenero, false);
    }

    /** Ingresos con tarjeta según la hora de la función: mañana (antes de 12), tarde (12 a 18) y noche. */
    private void graficoFranjas(List<Compra> compras) {
        Map<String, Double> porFranja = new LinkedHashMap<>();
        porFranja.put("Mañana", 0.0);
        porFranja.put("Tarde", 0.0);
        porFranja.put("Noche", 0.0);
        for (Compra c : compras) {
            if (c.getMetodo() != MetodoPago.TARJETA) continue;
            int hora = c.getFuncion().getFechaHora().getHour();
            porFranja.merge(hora < 12 ? "Mañana" : hora < 18 ? "Tarde" : "Noche", c.getTotal(), Double::sum);
        }
        llenarPastel(pieChartHorarios, porFranja, true);
    }

    private void graficoClientes(List<Compra> compras) {
        Map<String, Integer> porCliente = compras.stream().collect(Collectors.groupingBy(
                c -> c.getCliente().getNombre(), Collectors.summingInt(c -> c.getListaEntradas().size())));
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        porCliente.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> serie.getData().add(new XYChart.Data<>(e.getKey().split(" ")[0], e.getValue())));
        reemplazar(barChartAudiencia, serie);
    }

    private static void reemplazar(BarChart<String, Number> grafico, XYChart.Series<String, Number> serie) {
        grafico.setAnimated(false);
        grafico.getData().setAll(List.of(serie));
    }

    private static void llenarPastel(PieChart grafico, Map<String, Double> datos, boolean dinero) {
        double total = datos.values().stream().mapToDouble(Double::doubleValue).sum();
        grafico.setAnimated(false);
        grafico.setData(FXCollections.observableArrayList(datos.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new PieChart.Data(e.getKey() + " · " + (dinero ? Reporte.dinero(e.getValue())
                        : String.valueOf(e.getValue().intValue())) + " (" + Math.round(e.getValue() * 100 / total) + " %)",
                        e.getValue()))
                .toList()));
    }

    private void ponerTextos(String selector, String... textos) {
        Vistas.ponerTextos(contenido, selector, textos);
    }

    // ---------------- Reportes ----------------

    /** Genera el reporte elegido para el rango que indique el administrador y lo exporta a TXT o PDF. */
    @FXML
    public void exportarReporteTxt(ActionEvent event) {
        TipoReporte tipo = cmbTipoReporte.getValue();
        Optional<LocalDate[]> rango = pedirRango(tipo);
        if (rango.isEmpty()) {
            return;
        }
        try {
            Reporte reporte = servicio.generarReporte(tipo, rango.get()[0], rango.get()[1]);

            FileChooser selector = new FileChooser();
            selector.setTitle("Guardar reporte");
            selector.setInitialFileName(tipo.name().toLowerCase() + "_" + rango.get()[0] + "_" + rango.get()[1]);
            FileChooser.ExtensionFilter txt = new FileChooser.ExtensionFilter("Texto (*.txt)", "*.txt");
            FileChooser.ExtensionFilter pdf = new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf");
            selector.getExtensionFilters().addAll(txt, pdf);
            File archivo = selector.showSaveDialog(cmbTipoReporte.getScene().getWindow());
            if (archivo == null) {
                return;
            }
            boolean esPdf = selector.getSelectedExtensionFilter() == pdf || archivo.getName().toLowerCase().endsWith(".pdf");
            Exportador exportador = esPdf ? new ExportadorPDFAdapter() : new ExportadorTXT();
            Path guardado = reporte.exportar(exportador, sinExtension(archivo.toPath()));

            String totales = reporte.getTotales().entrySet().stream()
                    .map(e -> e.getKey().strip() + ": " + e.getValue()).collect(Collectors.joining("\n"));
            Dialogos.info("Reporte exportado", reporte.getTitulo() + " (" + reporte.getRegistros().size()
                    + " registros)\nGuardado en: " + guardado + "\n\n" + totales);
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo generar el reporte", e.getMessage());
        } catch (IOException e) {
            Dialogos.error("No se pudo guardar el archivo", e.getMessage());
        }
    }

    private static Path sinExtension(Path archivo) {
        String nombre = archivo.getFileName().toString().replaceFirst("(?i)\\.(txt|pdf)$", "");
        return archivo.resolveSibling(nombre);
    }

    private static Optional<LocalDate[]> pedirRango(TipoReporte tipo) {
        LocalDate hoy = LocalDate.now();
        // La ocupación se mide sobre la fecha de las funciones, así que por defecto incluye la próxima semana.
        DatePicker desde = new DatePicker(hoy.minusDays(DIAS - 1));
        DatePicker hasta = new DatePicker(tipo == TipoReporte.OCUPACION ? hoy.plusDays(DIAS) : hoy);
        GridPane formulario = new GridPane();
        formulario.setHgap(10);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(10));
        formulario.addRow(0, new Label("Desde"), desde);
        formulario.addRow(1, new Label("Hasta"), hasta);

        Dialog<LocalDate[]> dialogo = Dialogos.conDueno(new Dialog<>());
        dialogo.setTitle("CinemaUQ");
        dialogo.setHeaderText(tipo.getTitulo() + ": periodo del reporte");
        dialogo.getDialogPane().setContent(formulario);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogo.setResultConverter(b -> b == ButtonType.OK ? new LocalDate[]{desde.getValue(), hasta.getValue()} : null);
        return dialogo.showAndWait();
    }

    @FXML
    public void cerrarSesion(ActionEvent event) {
        fachada.cerrarSesion();
        Navigator.navigate(event, Navigator.LOGIN);
    }
}
