package co.edu.uniquindio.cinemauq.controllers.cliente;

import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.model.Promocion;
import co.edu.uniquindio.cinemauq.model.Reporte;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;
import co.edu.uniquindio.cinemauq.patrones.estructurales.facade.CineFacade;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Paso de confitería de la compra: productos y combos con stock (todos se tratan igual gracias al
 * Composite ItemVendible) y una promoción vigente opcional, tomados de CineFacade.
 */
public class ConfiteriaDialogo {

    private static final String SIN_PROMOCION = "Sin promoción";

    private final CineFacade fachada;

    public ConfiteriaDialogo(CineFacade fachada) {
        this.fachada = fachada;
    }

    /** Vacío si el cliente cancela la compra en este paso. */
    public Optional<SeleccionConfiteria> mostrar() {
        Dialog<SeleccionConfiteria> dialogo = Dialogos.conDueno(new Dialog<>());
        dialogo.setTitle("CinemaUQ");
        dialogo.setHeaderText("Confitería y promociones (opcional)");
        ButtonType continuar = new ButtonType("Continuar", ButtonType.OK.getButtonData());
        dialogo.getDialogPane().getButtonTypes().addAll(continuar, ButtonType.CANCEL);

        GridPane tabla = new GridPane();
        tabla.setHgap(14);
        tabla.setVgap(8);
        tabla.setPadding(new Insets(10));
        tabla.addRow(0, new Label("Producto / combo"), new Label("Precio"), new Label("Puntos"), new Label("Cantidad"));
        Map<ItemVendible, Spinner<Integer>> cantidades = new LinkedHashMap<>();
        int fila = 1;
        for (ItemVendible item : fachada.consultarProductos()) {
            Spinner<Integer> spinner = new Spinner<>(0, 10, 0);
            spinner.setPrefWidth(80);
            cantidades.put(item, spinner);
            String puntos = item.obtenerCostoEnPuntos() > 0 ? String.valueOf(item.obtenerCostoEnPuntos()) : "—";
            tabla.addRow(fila++, new Label(item.obtenerNombre()), new Label(Reporte.dinero(item.obtenerPrecio())),
                    new Label(puntos), spinner);
        }
        ComboBox<Object> promocion = new ComboBox<>();
        promocion.getItems().add(SIN_PROMOCION);
        promocion.getItems().addAll(fachada.consultarPromocionesVigentes());
        promocion.getSelectionModel().selectFirst();
        tabla.add(new Label("Promoción"), 0, fila);
        tabla.add(promocion, 1, fila, 3, 1);
        dialogo.getDialogPane().setContent(tabla);

        dialogo.setResultConverter(boton -> {
            if (boton != continuar) return null;
            Map<ItemVendible, Integer> items = new LinkedHashMap<>();
            cantidades.forEach((item, spinner) -> {
                if (spinner.getValue() > 0) items.put(item, spinner.getValue());
            });
            List<Promocion> promos = promocion.getValue() instanceof Promocion p ? List.of(p) : List.of();
            return new SeleccionConfiteria(items, promos);
        });
        return dialogo.showAndWait();
    }
}
