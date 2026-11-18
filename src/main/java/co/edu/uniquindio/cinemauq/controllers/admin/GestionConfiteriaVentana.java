package co.edu.uniquindio.cinemauq.controllers.admin;

import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.controllers.comun.Tablas;
import co.edu.uniquindio.cinemauq.exceptions.CinemaUQException;
import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.model.Reporte;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Combo;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Producto;
import co.edu.uniquindio.cinemauq.patrones.estructurales.proxy.ServicioAdministracion;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.stream.Collectors;

import static co.edu.uniquindio.cinemauq.controllers.comun.Tablas.columna;

/**
 * Confitería: productos (con stock) y combos (Composite: agrupan productos y toman su stock).
 * Se pueden crear, editar (nombre, precio, puntos y, en productos, stock) y eliminar.
 */
public class GestionConfiteriaVentana {

    private final ServicioAdministracion servicio;
    private final TableView<ItemVendible> tabla = Tablas.tabla(
            columna("Id", ItemVendible::obtenerId),
            columna("Tipo", i -> i instanceof Combo ? "Combo" : "Producto"),
            columna("Nombre", ItemVendible::obtenerNombre),
            columna("Precio", i -> Reporte.dinero(i.obtenerPrecio())),
            columna("Stock", i -> i instanceof Producto p ? String.valueOf(p.getStock()) : "según contenido"),
            columna("Puntos", i -> i.obtenerCostoEnPuntos() == 0 ? "No se paga con puntos" : String.valueOf(i.obtenerCostoEnPuntos())),
            columna("Contenido", i -> i instanceof Combo c
                    ? c.obtenerItems().stream().map(ItemVendible::obtenerNombre).collect(Collectors.joining(", ")) : ""));

    public GestionConfiteriaVentana(ServicioAdministracion servicio) {
        this.servicio = servicio;
    }

    public void mostrar() {
        recargar();
        Tablas.mostrarVentana("Confitería: productos y combos", Tablas.panel(tabla,
                Tablas.boton("Agregar producto", () -> { agregarProducto(); recargar(); }),
                Tablas.boton("Agregar combo", () -> { agregarCombo(); recargar(); }),
                Tablas.botonConSeleccion("Editar", tabla, "un producto o combo", this::editar, this::recargar),
                Tablas.botonConSeleccion("Eliminar", tabla, "un producto o combo", this::eliminar, this::recargar)));
    }

    private void recargar() {
        Tablas.cargar(tabla, servicio::consultarItems);
    }

    private void eliminar(ItemVendible item) {
        if (Dialogos.confirmar("Eliminar", "Se quitará \"" + item.obtenerNombre() + "\" de la confitería.")) {
            Tablas.ejecutar("No se pudo eliminar", () -> servicio.eliminarItem(item.obtenerId()));
        }
    }

    private String nuevoId(String prefijo) throws CinemaUQException {
        return Tablas.siguienteId(prefijo, servicio.consultarItems().stream().map(ItemVendible::obtenerId).toList());
    }

    private void agregarProducto() {
        TextField nombre = new TextField();
        TextField precio = new TextField("8000");
        TextField stock = new TextField("100");
        TextField puntos = new TextField("0");
        if (!Tablas.pedir("Agregar producto", Tablas.formulario(
                new Label("Nombre"), nombre, new Label("Precio"), precio,
                new Label("Stock"), stock, new Label("Costo en puntos (0 = no se paga con puntos)"), puntos))) {
            return;
        }
        try {
            servicio.agregarItem(new Producto(nuevoId("PR"), nombre.getText().trim(), Tablas.decimal(precio.getText(), "Precio"),
                    Tablas.entero(stock.getText(), "Stock"), Tablas.entero(puntos.getText(), "Puntos")));
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo agregar el producto", e.getMessage());
        }
    }

    private void agregarCombo() {
        try {
            List<ItemVendible> productos = servicio.consultarItems().stream().filter(i -> i instanceof Producto).toList();
            if (productos.isEmpty()) {
                Dialogos.advertencia("Sin productos", "Cree primero los productos que llevará el combo.");
                return;
            }
            TextField nombre = new TextField();
            TextField precio = new TextField("25000");
            TextField puntos = new TextField("0");
            ListView<ItemVendible> lista = new ListView<>(FXCollections.observableArrayList(productos));
            lista.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            lista.setPrefHeight(150);
            Spinner<Integer> unidades = new Spinner<>(1, 10, 1);
            if (!Tablas.pedir("Agregar combo", Tablas.formulario(
                    new Label("Nombre"), nombre, new Label("Precio"), precio,
                    new Label("Costo en puntos (0 = no)"), puntos,
                    new Label("Productos (Ctrl para varios)"), lista,
                    new Label("Unidades de cada uno"), unidades))) {
                return;
            }
            if (lista.getSelectionModel().getSelectedItems().isEmpty()) {
                throw new DatosInvalidosException("Elija al menos un producto para el combo.");
            }
            Combo combo = new Combo(nuevoId("CO"), nombre.getText().trim(), Tablas.decimal(precio.getText(), "Precio"),
                    Tablas.entero(puntos.getText(), "Puntos"));
            for (ItemVendible producto : lista.getSelectionModel().getSelectedItems()) {
                for (int i = 0; i < unidades.getValue(); i++) {
                    combo.agregarItem(producto);
                }
            }
            servicio.agregarItem(combo);
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo agregar el combo", e.getMessage());
        }
    }

    private void editar(ItemVendible item) {
        TextField nombre = new TextField(item.obtenerNombre());
        TextField precio = new TextField(String.valueOf((long) item.obtenerPrecio()));
        TextField puntos = new TextField(String.valueOf(item.obtenerCostoEnPuntos()));
        TextField stock = new TextField(item instanceof Producto p ? String.valueOf(p.getStock()) : "0");
        stock.setDisable(!(item instanceof Producto));
        if (!Tablas.pedir("Editar " + item.obtenerNombre(), Tablas.formulario(
                new Label("Nombre"), nombre, new Label("Precio"), precio,
                new Label("Costo en puntos"), puntos, new Label("Stock (solo productos)"), stock))) {
            return;
        }
        try {
            servicio.actualizarItem(item.obtenerId(), nombre.getText(), Tablas.decimal(precio.getText(), "Precio"),
                    Tablas.entero(puntos.getText(), "Puntos"), Tablas.entero(stock.getText(), "Stock"));
        } catch (CinemaUQException e) {
            Dialogos.error("No se pudo editar", e.getMessage());
        }
    }
}
