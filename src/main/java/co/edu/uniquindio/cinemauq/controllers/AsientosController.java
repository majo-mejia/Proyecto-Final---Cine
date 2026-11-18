package co.edu.uniquindio.cinemauq.controllers;

import co.edu.uniquindio.cinemauq.controllers.cliente.ConfiteriaDialogo;
import co.edu.uniquindio.cinemauq.controllers.cliente.SeleccionConfiteria;
import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.controllers.comun.Vistas;
import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.exceptions.SaldoInsuficienteException;
import co.edu.uniquindio.cinemauq.model.Asiento;
import co.edu.uniquindio.cinemauq.model.AsientoFuncion;
import co.edu.uniquindio.cinemauq.model.Compra;
import co.edu.uniquindio.cinemauq.model.Funcion;
import co.edu.uniquindio.cinemauq.model.Reporte;
import co.edu.uniquindio.cinemauq.model.SolicitudRecarga;
import co.edu.uniquindio.cinemauq.model.enums.MetodoPago;
import co.edu.uniquindio.cinemauq.model.enums.TipoSala;
import co.edu.uniquindio.cinemauq.patrones.creacionales.singleton.ConfiguracionCine;
import co.edu.uniquindio.cinemauq.patrones.estructurales.facade.CineFacade;
import co.edu.uniquindio.cinemauq.util.CinemaUQApp;
import co.edu.uniquindio.cinemauq.util.Navigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Selección de asientos y pago (Asientos.fxml). El mapa se arma con los asientos reales de la
 * función (ocupados, libres y accesibles) y la compra se hace con CineFacade.comprar, que valida
 * todas las reglas (asientos libres, saldo, puntos, stock) y lanza la excepción de dominio si algo falla.
 */
public class AsientosController {

    private static final Locale COLOMBIA = Locale.of("es", "CO");
    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("EEE d MMM", COLOMBIA);
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("h:mm a", COLOMBIA);

    @FXML private GridPane gridAsientos;
    @FXML private FlowPane panelAsientosSeleccionados;
    @FXML private Label lblCantidad;
    @FXML private Label lblSubtotalEntradas;
    @FXML private Label lblSubtotal;
    @FXML private Label lblServiceFee;
    @FXML private Label lblTotal;
    @FXML private Label lblPuntos;

    private final CineFacade fachada = CinemaUQApp.obtenerFachada();
    private final ConfiguracionCine config = ConfiguracionCine.getInstancia();
    private final Set<String> seleccionados = new LinkedHashSet<>();
    private Funcion funcion;
    private Parent raiz;

    @FXML
    public void initialize() {
        raiz = Vistas.raiz(gridAsientos);
        // El botón de pago no tiene fx:id ni onAction en el FXML: se ubica por su clase CSS.
        if (raiz.lookup(".btn-gold") instanceof Button botonPago) {
            botonPago.setOnAction(e -> continuarAlPago());
        }
        actualizarResumen();
    }

    /** La cartelera llama a este método después de cargar la vista. */
    public void mostrarFuncion(Funcion funcion) {
        this.funcion = funcion;
        mostrarDatosDeLaFuncion();
        construirMapa();
        actualizarResumen();
    }

    @FXML
    public void volver(ActionEvent event) {
        Navigator.navigate(event, Navigator.CARTELERA);
    }

    // ---------------- Vista ----------------

    /** Reemplaza los textos de ejemplo del FXML (película, fecha, hora y sala) por los de la función. */
    private void mostrarDatosDeLaFuncion() {
        String titulo = funcion.getPelicula().getTitulo();
        String dia = "📅 " + funcion.getFechaHora().format(FORMATO_DIA);
        String hora = "🕒 " + funcion.getFechaHora().format(FORMATO_HORA);
        String sala = "🚪 " + funcion.getSala().getNombre() + (funcion.getSala().getTipo() == TipoSala.SALA_3D ? " (3D)" : "");
        Vistas.ponerTextos(raiz, ".meta-item", "🎬 " + titulo, dia, hora, sala);
        Vistas.ponerTextos(raiz, ".summary-movie-title", titulo);
        Vistas.ponerTextos(raiz, ".summary-meta", dia, hora, sala);
        Node poster = raiz.lookup(".poster-placeholder");
        if (poster instanceof Parent p && !p.getChildrenUnmodifiable().isEmpty()
                && p.getChildrenUnmodifiable().get(0) instanceof Label etiqueta) {
            etiqueta.setText(titulo.toUpperCase().replace(" ", "\n"));
        }
    }

    private void construirMapa() {
        gridAsientos.getChildren().clear();
        Collection<AsientoFuncion> asientos = fachada.consultarAsientos(funcion);
        Set<Character> filas = new TreeSet<>();
        asientos.forEach(a -> filas.add(a.getAsiento().getFila()));
        List<Character> ordenFilas = new ArrayList<>(filas);

        for (int f = 0; f < ordenFilas.size(); f++) {
            Label lblFila = new Label(String.valueOf(ordenFilas.get(f)));
            lblFila.getStyleClass().add("row-label");
            lblFila.setPrefSize(25, 36);
            lblFila.setAlignment(Pos.CENTER);
            gridAsientos.add(lblFila, 0, f);
        }
        for (AsientoFuncion af : asientos) {
            Asiento asiento = af.getAsiento();
            String codigo = asiento.obtenerIdentificador();
            ToggleButton btn = new ToggleButton(asiento.esAccesible() ? "♿" : String.valueOf(asiento.getNumero()));
            btn.setPrefSize(38, 36);
            btn.setFocusTraversable(false);
            String estiloBase = asiento.esAccesible() ? "asiento-accesible" : "asiento-disponible";
            if (!af.estaDisponible()) {
                btn.getStyleClass().add("asiento-ocupado");
                btn.setDisable(true);
            } else {
                btn.getStyleClass().add(estiloBase);
                btn.setOnAction(e -> alternar(btn, codigo, estiloBase));
            }
            gridAsientos.add(btn, asiento.getNumero(), ordenFilas.indexOf(asiento.getFila()));
        }
    }

    private void alternar(ToggleButton btn, String codigo, String estiloBase) {
        if (btn.isSelected()) {
            if (seleccionados.size() >= config.getMaxAsientosPorCompra()) {
                btn.setSelected(false);
                Dialogos.advertencia("Límite de asientos",
                        "Puede elegir máximo " + config.getMaxAsientosPorCompra() + " asientos por compra.");
                return;
            }
            btn.getStyleClass().remove(estiloBase);
            btn.getStyleClass().add("asiento-seleccionado");
            seleccionados.add(codigo);
        } else {
            btn.getStyleClass().remove("asiento-seleccionado");
            btn.getStyleClass().add(estiloBase);
            seleccionados.remove(codigo);
        }
        actualizarResumen();
    }

    private void actualizarResumen() {
        panelAsientosSeleccionados.getChildren().clear();
        for (String codigo : seleccionados) {
            Label badge = new Label(codigo);
            badge.getStyleClass().add("selected-badge");
            panelAsientosSeleccionados.getChildren().add(badge);
        }
        double precio = funcion == null ? 0 : funcion.calcularPrecioEntrada();
        int cantidad = seleccionados.size();
        double subtotal = cantidad * precio;
        double fee = cantidad > 0 ? config.getServiceFee() : 0;
        double total = subtotal + fee;

        lblCantidad.setText(cantidad + " x " + Reporte.dinero(precio));
        lblSubtotalEntradas.setText(Reporte.dinero(subtotal));
        lblSubtotal.setText(Reporte.dinero(subtotal));
        lblServiceFee.setText(Reporte.dinero(fee));
        lblTotal.setText(Reporte.dinero(total));
        lblPuntos.setText((int) (total / config.getValorPorPuntos()) + " pts ⭐");
    }

    // ---------------- Compra ----------------

    private void continuarAlPago() {
        if (funcion == null || seleccionados.isEmpty()) {
            Dialogos.advertencia("Sin asientos", "Seleccione al menos un asiento para continuar.");
            return;
        }
        Optional<SeleccionConfiteria> extras = new ConfiteriaDialogo(fachada).mostrar();
        if (extras.isEmpty()) {
            return;
        }
        Optional<MetodoPago> metodo = elegirMetodoDePago(extras.get());
        if (metodo.isEmpty() || !confirmar(extras.get(), metodo.get())) {
            return;
        }
        try {
            Compra compra = fachada.comprar(funcion, new ArrayList<>(seleccionados), extras.get().items(),
                    extras.get().promociones(), metodo.get());
            mostrarCompraExitosa(compra);
            seleccionados.clear();
            construirMapa();
            actualizarResumen();
        } catch (SaldoInsuficienteException e) {
            ofrecerRecarga(e.getMessage());
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo completar la compra", e.getMessage());
        }
    }

    private Optional<MetodoPago> elegirMetodoDePago(SeleccionConfiteria extras) {
        try {
            int puntosNecesarios = seleccionados.size() * config.getCostoEntradaEnPuntos() + extras.costoEnPuntos();
            String tarjeta = "Tarjeta virtual (saldo " + Reporte.dinero(fachada.consultarSaldo()) + ")";
            String puntos = "Puntos CinemaUQ (tiene " + fachada.consultarPuntos() + ", necesita " + puntosNecesarios + ")";
            ChoiceDialog<String> dialogo = Dialogos.conDueno(new ChoiceDialog<>(tarjeta, tarjeta, puntos));
            dialogo.setTitle("CinemaUQ");
            dialogo.setHeaderText("¿Cómo desea pagar?");
            dialogo.setContentText("Método de pago:");
            return dialogo.showAndWait().map(op -> op.equals(tarjeta) ? MetodoPago.TARJETA : MetodoPago.PUNTOS);
        } catch (CinemaUQException e) {
            Dialogos.error("Sesión no válida", e.getMessage());
            return Optional.empty();
        }
    }

    private boolean confirmar(SeleccionConfiteria extras, MetodoPago metodo) {
        double entradas = seleccionados.size() * funcion.calcularPrecioEntrada();
        StringBuilder resumen = new StringBuilder()
                .append(funcion.getPelicula().getTitulo()).append(" - ")
                .append(funcion.getFechaHora().format(CarteleraController.FORMATO_FUNCION)).append("\n")
                .append("Asientos: ").append(String.join(", ", seleccionados)).append("\n\n")
                .append("Entradas: ").append(Reporte.dinero(entradas)).append("\n");
        if (!extras.items().isEmpty()) {
            resumen.append("Confitería: ").append(Reporte.dinero(extras.valor())).append("\n");
        }
        if (!extras.promociones().isEmpty()) {
            resumen.append("Promoción: ").append(extras.promociones().get(0).getNombre()).append("\n");
        }
        resumen.append("Tarifa de servicio: ").append(Reporte.dinero(config.getServiceFee())).append("\n\n")
                .append(metodo == MetodoPago.TARJETA ? "Se cobrará a su tarjeta virtual." : "Se pagará con puntos.");
        return Dialogos.confirmar("Confirmar compra", resumen.toString());
    }

    private void mostrarCompraExitosa(Compra compra) throws CinemaUQException {
        StringBuilder detalle = new StringBuilder("Compra ").append(compra.getId()).append("\n")
                .append("Asientos: ").append(String.join(", ", seleccionados)).append("\n");
        if (compra.getMetodo() == MetodoPago.TARJETA) {
            detalle.append("Total pagado: ").append(Reporte.dinero(compra.getTotal())).append("\n")
                    .append("Puntos ganados: ").append((int) (compra.getTotal() / config.getValorPorPuntos())).append("\n");
        } else {
            detalle.append("Puntos usados: ").append(compra.getPuntosUtilizados()).append("\n");
        }
        detalle.append("Saldo actual: ").append(Reporte.dinero(fachada.consultarSaldo())).append("\n")
                .append("Puntos disponibles: ").append(fachada.consultarPuntos());
        Dialogos.info("¡Compra realizada!", detalle.toString());
    }

    /** Regla 3: sin saldo, el cliente puede pedir una recarga que aprueba el administrador. */
    private void ofrecerRecarga(String motivo) {
        if (!Dialogos.confirmar("Saldo insuficiente", motivo + "\n\n¿Desea solicitar una recarga de $100.000?")) {
            return;
        }
        try {
            SolicitudRecarga solicitud = fachada.solicitarRecarga(100_000);
            Dialogos.info("Solicitud enviada", "Su solicitud " + solicitud.getId()
                    + " quedó pendiente. El administrador debe aprobarla.");
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo solicitar la recarga", e.getMessage());
        }
    }
}
