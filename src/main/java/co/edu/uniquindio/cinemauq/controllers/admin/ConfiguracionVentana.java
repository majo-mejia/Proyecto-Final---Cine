package co.edu.uniquindio.cinemauq.controllers.admin;

import co.edu.uniquindio.cinemauq.controllers.comun.Dialogos;
import co.edu.uniquindio.cinemauq.model.Reporte;
import co.edu.uniquindio.cinemauq.patrones.creacionales.singleton.ConfiguracionCine;

/** Muestra los valores de negocio de ConfiguracionCine (Singleton). */
public class ConfiguracionVentana {

    public void mostrar() {
        ConfiguracionCine c = ConfiguracionCine.getInstancia();
        Dialogos.info("Configuración del cine", String.join("\n",
                "Tarifa de servicio por compra: " + Reporte.dinero(c.getServiceFee()),
                "Puntos: 1 por cada " + Reporte.dinero(c.getValorPorPuntos()) + ", vigencia "
                        + c.getVigenciaPuntosAnios() + " año(s)",
                "Entrada pagada con puntos: " + c.getCostoEntradaEnPuntos() + " puntos",
                "Máximo por compra: " + c.getMaxAsientosPorCompra() + " asientos y "
                        + c.getMaxPromocionesPorCompra() + " promoción",
                "Reembolso total con más de " + c.getHorasReembolsoTotal() + " h de anticipación",
                "Reembolso del " + Math.round(c.getPorcentajeReembolsoParcial() * 100) + " % entre "
                        + c.getHorasMinimasCancelacion() + " y " + c.getHorasReembolsoTotal() + " h",
                "Sin cancelación a menos de " + c.getHorasMinimasCancelacion() + " h"));
    }
}
