package co.edu.uniquindio.cinemauq.patrones.creacionales.singleton;

/**
 * Patrón SINGLETON.
 * Problema: las reglas numéricas del negocio (tarifa de servicio, valor del punto, vigencia,
 * horas y porcentajes de reembolso, límites por compra) deben ser las mismas en todo el sistema.
 * Una única instancia global evita que cada clase tenga sus propios valores "quemados".
 * Los valores por defecto son provisionales y el grupo puede ajustarlos.
 */
public final class ConfiguracionCine {

    private static ConfiguracionCine instancia;

    /** Tarifa fija de servicio que se suma a cada compra. */
    private double serviceFee = 2_000;
    /** Pesos gastados que equivalen a 1 punto ganado. */
    private double valorPorPuntos = 1_000;
    private int vigenciaPuntosAnios = 1;
    private int costoEntradaEnPuntos = 120;
    private int maxAsientosPorCompra = 10;
    private int maxPromocionesPorCompra = 1;
    private int horasReembolsoTotal = 24;
    private int horasMinimasCancelacion = 2;
    private double porcentajeReembolsoParcial = 0.8;

    private ConfiguracionCine() {
    }

    public static synchronized ConfiguracionCine getInstancia() {
        if (instancia == null) {
            instancia = new ConfiguracionCine();
        }
        return instancia;
    }

    public double getServiceFee() { return serviceFee; }
    public void setServiceFee(double serviceFee) { this.serviceFee = serviceFee; }
    public double getValorPorPuntos() { return valorPorPuntos; }
    public void setValorPorPuntos(double valorPorPuntos) { this.valorPorPuntos = valorPorPuntos; }
    public int getVigenciaPuntosAnios() { return vigenciaPuntosAnios; }
    public void setVigenciaPuntosAnios(int vigenciaPuntosAnios) { this.vigenciaPuntosAnios = vigenciaPuntosAnios; }
    public int getCostoEntradaEnPuntos() { return costoEntradaEnPuntos; }
    public void setCostoEntradaEnPuntos(int costoEntradaEnPuntos) { this.costoEntradaEnPuntos = costoEntradaEnPuntos; }
    public int getMaxAsientosPorCompra() { return maxAsientosPorCompra; }
    public void setMaxAsientosPorCompra(int maxAsientosPorCompra) { this.maxAsientosPorCompra = maxAsientosPorCompra; }
    public int getMaxPromocionesPorCompra() { return maxPromocionesPorCompra; }
    public void setMaxPromocionesPorCompra(int maxPromocionesPorCompra) { this.maxPromocionesPorCompra = maxPromocionesPorCompra; }
    public int getHorasReembolsoTotal() { return horasReembolsoTotal; }
    public void setHorasReembolsoTotal(int horasReembolsoTotal) { this.horasReembolsoTotal = horasReembolsoTotal; }
    public int getHorasMinimasCancelacion() { return horasMinimasCancelacion; }
    public void setHorasMinimasCancelacion(int horasMinimasCancelacion) { this.horasMinimasCancelacion = horasMinimasCancelacion; }
    public double getPorcentajeReembolsoParcial() { return porcentajeReembolsoParcial; }
    public void setPorcentajeReembolsoParcial(double porcentajeReembolsoParcial) { this.porcentajeReembolsoParcial = porcentajeReembolsoParcial; }
}
