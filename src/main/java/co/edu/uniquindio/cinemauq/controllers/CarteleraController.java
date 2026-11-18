package co.edu.uniquindio.cinemauq.controllers;

import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.controllers.cliente.TarjetaClienteVentana;
import co.edu.uniquindio.cinemauq.model.Funcion;
import co.edu.uniquindio.cinemauq.model.Pelicula;
import co.edu.uniquindio.cinemauq.model.Reporte;
import co.edu.uniquindio.cinemauq.model.enums.TipoSala;
import co.edu.uniquindio.cinemauq.patrones.estructurales.facade.CineFacade;
import co.edu.uniquindio.cinemauq.util.CinemaUQApp;
import co.edu.uniquindio.cinemauq.util.Navigator;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Cartelera del cliente (Cartelera.fxml). Las tarjetas se generan con las películas y funciones
 * reales de CineFacade; el buscador y los filtros (formato, sala, género) se aplican en vivo.
 * Al comprar se elige la función y se abre la selección de asientos.
 */
public class CarteleraController {

    static final DateTimeFormatter FORMATO_FUNCION =
            DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", Locale.of("es", "CO"));
    private static final String TODOS = "Todos";
    private static final String[] COLORES_POSTER = {"#1E3A8A", "#065F46", "#78350F", "#312E81", "#4C1D95", "#7F1D1D"};

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbFormato;
    @FXML private ComboBox<String> cmbIdioma;
    @FXML private ComboBox<String> cmbSala;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private FlowPane gridPeliculas;

    private final CineFacade fachada = CinemaUQApp.obtenerFachada();
    /** Cartelera con sus funciones próximas, en el orden en que se muestran. */
    private final Map<Pelicula, List<Funcion>> cartelera = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        for (Pelicula p : fachada.consultarCartelera()) {
            cartelera.put(p, fachada.consultarFunciones(p));
        }
        TreeSet<String> formatos = new TreeSet<>();
        TreeSet<String> salas = new TreeSet<>();
        TreeSet<String> generos = new TreeSet<>();
        cartelera.forEach((pelicula, funciones) -> {
            generos.add(pelicula.getGenero());
            funciones.forEach(f -> {
                formatos.add(formato(f));
                salas.add(f.getSala().getNombre());
            });
        });
        llenar(cmbFormato, formatos);
        llenar(cmbSala, salas);
        llenar(cmbGenero, generos);
        // El modelo no registra el idioma de las películas, así que ese filtro no aplica.
        cmbIdioma.setItems(FXCollections.observableArrayList(TODOS));
        cmbIdioma.getSelectionModel().selectFirst();
        cmbIdioma.setDisable(true);
        cmbIdioma.setTooltip(new Tooltip("Las películas no tienen idioma registrado"));

        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> renderizarPeliculas());
        renderizarPeliculas();
    }

    private void llenar(ComboBox<String> combo, TreeSet<String> opciones) {
        List<String> items = new ArrayList<>();
        items.add(TODOS);
        items.addAll(opciones);
        combo.setItems(FXCollections.observableArrayList(items));
        combo.getSelectionModel().selectFirst();
        combo.valueProperty().addListener((obs, anterior, nuevo) -> renderizarPeliculas());
    }

    private static String formato(Funcion f) {
        return f.getSala().getTipo() == TipoSala.SALA_3D ? "3D" : "2D";
    }

    // ---------------- Tarjetas ----------------

    private void renderizarPeliculas() {
        gridPeliculas.getChildren().clear();
        String consulta = txtBuscar.getText() == null ? "" : txtBuscar.getText().toLowerCase().trim();
        int indice = 0;
        for (Map.Entry<Pelicula, List<Funcion>> e : cartelera.entrySet()) {
            Pelicula p = e.getKey();
            boolean coincideTexto = consulta.isEmpty() || p.getTitulo().toLowerCase().contains(consulta)
                    || p.getSinopsis().toLowerCase().contains(consulta) || p.getGenero().toLowerCase().contains(consulta);
            boolean coincideGenero = esTodos(cmbGenero) || p.getGenero().equals(cmbGenero.getValue());
            List<Funcion> funciones = e.getValue().stream()
                    .filter(f -> esTodos(cmbFormato) || formato(f).equals(cmbFormato.getValue()))
                    .filter(f -> esTodos(cmbSala) || f.getSala().getNombre().equals(cmbSala.getValue()))
                    .toList();
            if (coincideTexto && coincideGenero && !funciones.isEmpty()) {
                gridPeliculas.getChildren().add(crearTarjeta(p, funciones, COLORES_POSTER[indice % COLORES_POSTER.length]));
            }
            indice++;
        }
        if (gridPeliculas.getChildren().isEmpty()) {
            Label vacio = new Label("No hay funciones que coincidan con la búsqueda o los filtros.");
            vacio.getStyleClass().add("movie-card-subtitle");
            gridPeliculas.getChildren().add(vacio);
        }
    }

    private static boolean esTodos(ComboBox<String> combo) {
        return combo.getValue() == null || combo.getValue().equals(TODOS);
    }

    private VBox crearTarjeta(Pelicula p, List<Funcion> funciones, String color) {
        Funcion proxima = funciones.get(0);
        boolean preventa = proxima.getFechaHora().toLocalDate().isAfter(LocalDate.now().plusDays(1));

        StackPane poster = new StackPane();
        poster.setPrefHeight(270);
        poster.setStyle("-fx-background-color: linear-gradient(to bottom, " + color + ", #0F131A); -fx-background-radius: 8 8 0 0;");
        Label badge = new Label(preventa ? "PREVENTA" : "EN CARTELERA");
        badge.getStyleClass().add(preventa ? "badge-preventa" : "badge-estreno");
        StackPane.setAlignment(badge, Pos.TOP_CENTER);
        Label tituloPoster = new Label(p.getTitulo().toUpperCase());
        tituloPoster.setWrapText(true);
        tituloPoster.setAlignment(Pos.CENTER);
        tituloPoster.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 15; -fx-text-alignment: center;");
        poster.getChildren().addAll(tituloPoster, badge);

        Label titulo = new Label(p.getTitulo());
        titulo.getStyleClass().add("movie-card-title");
        Label detalle = new Label(p.getGenero() + " · " + p.getClasificacion() + " · " + p.getDuracionMinutos() + " min");
        detalle.getStyleClass().add("movie-card-subtitle");
        Label horario = new Label("🕒 " + proxima.getSala().getNombre() + " (" + formato(proxima) + ") • "
                + proxima.getFechaHora().format(FORMATO_FUNCION));
        horario.getStyleClass().add("movie-card-meta");
        Label otras = new Label(funciones.size() + " funciones · desde " + Reporte.dinero(
                funciones.stream().mapToDouble(Funcion::calcularPrecioEntrada).min().orElse(0)));
        otras.getStyleClass().add("movie-card-meta");

        Button comprar = new Button("🎟️ Comprar Entradas");
        comprar.setMaxWidth(Double.MAX_VALUE);
        comprar.getStyleClass().add("btn-gold-card");
        comprar.setOnAction(e -> seleccionarAsientos(e, p, funciones));

        VBox info = new VBox(4, titulo, detalle, horario, otras, comprar);
        info.setStyle("-fx-padding: 8 12 12 12;");
        VBox tarjeta = new VBox(8, poster, info);
        tarjeta.setPrefWidth(220);
        tarjeta.getStyleClass().add("movie-card");
        Tooltip.install(tarjeta, new Tooltip(p.getSinopsis()));
        return tarjeta;
    }

    // ---------------- Acciones ----------------

    private void seleccionarAsientos(ActionEvent event, Pelicula pelicula, List<Funcion> funciones) {
        elegirFuncion(pelicula, funciones).ifPresent(funcion -> {
            AsientosController asientos = Navigator.navigate(event, Navigator.ASIENTOS);
            if (asientos != null) {
                asientos.mostrarFuncion(funcion);
            }
        });
    }

    private static Optional<Funcion> elegirFuncion(Pelicula pelicula, List<Funcion> funciones) {
        Map<String, Funcion> opciones = new LinkedHashMap<>();
        for (Funcion f : funciones) {
            opciones.put(f.getFechaHora().format(FORMATO_FUNCION) + "  ·  " + f.getSala().getNombre() + " (" + formato(f)
                    + ")  ·  " + Reporte.dinero(f.calcularPrecioEntrada()) + "  ·  "
                    + f.obtenerAsientos().stream().filter(a -> a.estaDisponible()).count() + " libres", f);
        }
        ChoiceDialog<String> dialogo = Dialogos.conDueno(new ChoiceDialog<>(opciones.keySet().iterator().next(), opciones.keySet()));
        dialogo.setTitle("CinemaUQ");
        dialogo.setHeaderText(pelicula.getTitulo() + " (" + pelicula.getClasificacion() + ", "
                + pelicula.getDuracionMinutos() + " min)\n" + pelicula.getSinopsis());
        dialogo.setContentText("Función:");
        return dialogo.showAndWait().map(opciones::get);
    }

    /** Botón "Recarga Tarjeta CinemaUQ": saldo, puntos, movimientos, compras, notificaciones y recarga. */
    @FXML
    public void recargarTarjeta(ActionEvent event) {
        new TarjetaClienteVentana(fachada).mostrar();
        renderizarPeliculas(); // una cancelación libera asientos
    }

    @FXML
    public void cerrarSesion(ActionEvent event) {
        fachada.cerrarSesion();
        Navigator.navigate(event, Navigator.LOGIN);
    }
}
