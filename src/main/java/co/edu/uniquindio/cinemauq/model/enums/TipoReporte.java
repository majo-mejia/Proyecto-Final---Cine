package co.edu.uniquindio.cinemauq.model.enums;

public enum TipoReporte {
    VENTAS("Ventas por periodo"),
    ENTRADAS_POR_PELICULA("Entradas por película y función"),
    OCUPACION("Ocupación de salas"),
    PRODUCTOS_Y_COMBOS("Productos y combos vendidos"),
    RECARGAS("Recargas"),
    CANCELACIONES_Y_REEMBOLSOS("Cancelaciones y reembolsos"),
    PUNTOS("Puntos acumulados y redimidos");

    private final String titulo;

    TipoReporte(String titulo) { this.titulo = titulo; }

    public String getTitulo() { return titulo; }

    @Override
    public String toString() { return titulo; }
}
