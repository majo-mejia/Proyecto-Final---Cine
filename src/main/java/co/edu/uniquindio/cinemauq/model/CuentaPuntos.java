package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.exceptions.PuntosInsuficientesException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CuentaPuntos {

    private final String id;
    private final List<LotePuntos> listaLotes = new ArrayList<>();
    private int totalRedimido;
    private int totalAnulado;

    public CuentaPuntos(String id) {
        this.id = id;
    }

    /**
     * Registra puntos ganados en la compra idCompra. Si son 0 (compra pequeña) no crea lote y
     * devuelve null.
     */
    public LotePuntos acumular(int puntos, LocalDate fecha, int vigenciaAnios, String idCompra) {
        if (puntos <= 0) {
            return null;
        }
        LotePuntos lote = new LotePuntos(puntos, fecha, vigenciaAnios, idCompra);
        listaLotes.add(lote);
        return lote;
    }

    /** Puntos que no vienen de una compra (devolución de puntos al cancelar una compra pagada con puntos). */
    public LotePuntos acumular(int puntos, LocalDate fecha, int vigenciaAnios) {
        return acumular(puntos, fecha, vigenciaAnios, null);
    }

    /**
     * La compra idCompra se canceló: anula lo que quede disponible de los puntos que ganó.
     * Devuelve cuántos puntos se anularon (0 si ya los había gastado todos o no ganó puntos).
     */
    public int anularPuntosDeCompra(String idCompra) {
        int anulados = 0;
        for (LotePuntos lote : listaLotes) {
            if (lote.esDeCompra(idCompra)) {
                anulados += lote.anularPuntosDisponibles();
            }
        }
        totalAnulado += anulados;
        return anulados;
    }

    public int obtenerPuntosDisponibles(LocalDate hoy) {
        return listaLotes.stream()
                .filter(l -> !l.estanVencido(hoy))
                .mapToInt(LotePuntos::getPuntosDisponibles)
                .sum();
    }

    public int obtenerPuntosVencidos(LocalDate hoy) {
        return listaLotes.stream()
                .filter(l -> l.estanVencido(hoy))
                .mapToInt(LotePuntos::getPuntosDisponibles)
                .sum();
    }

    /**
     * Regla 5: valida que alcancen y consume primero los lotes más próximos a vencer.
     * Nunca toca puntos vencidos.
     */
    public void redimir(int puntos, LocalDate hoy) throws PuntosInsuficientesException, DatosInvalidosException {
        if (puntos <= 0) {
            throw new DatosInvalidosException("La cantidad de puntos a redimir debe ser mayor que cero.");
        }
        int disponibles = obtenerPuntosDisponibles(hoy);
        if (puntos > disponibles) {
            throw new PuntosInsuficientesException(
                    "Se necesitan " + puntos + " puntos y solo hay " + disponibles + " vigentes.");
        }
        List<LotePuntos> vigentes = listaLotes.stream()
                .filter(l -> !l.estanVencido(hoy) && l.getPuntosDisponibles() > 0)
                .sorted(Comparator.comparing(LotePuntos::getFechaVencimiento))
                .toList();
        int pendientes = puntos;
        for (LotePuntos lote : vigentes) {
            if (pendientes == 0) break;
            pendientes -= lote.consumirPuntos(pendientes);
        }
        totalRedimido += puntos;
    }

    public int obtenerTotalAcumulado() {
        return listaLotes.stream().mapToInt(LotePuntos::getPuntosObtenidos).sum();
    }

    public int obtenerTotalRedimido() { return totalRedimido; }

    public int obtenerTotalAnulado() { return totalAnulado; }

    public String getId() { return id; }
    public List<LotePuntos> getListaLotes() { return Collections.unmodifiableList(listaLotes); }
}
