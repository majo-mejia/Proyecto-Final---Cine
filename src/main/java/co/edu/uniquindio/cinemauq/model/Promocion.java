package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;

import java.time.LocalDate;
import java.util.Objects;

public class Promocion {

    private final String id;
    private String nombre;
    /** 0.20 = 20 %. */
    private double porcentajeDescuento;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    public Promocion(String id, String nombre, double porcentajeDescuento, LocalDate fechaInicio, LocalDate fechaFin)
            throws DatosInvalidosException {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new DatosInvalidosException("La fecha final de la promoción no puede ser anterior a la inicial.");
        }
        this.id = id;
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        setPorcentajeDescuento(porcentajeDescuento);
    }

    public boolean estaVigente(LocalDate fecha) {
        return !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }

    public double calcularDescuento(double monto) {
        return monto * porcentajeDescuento;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getPorcentajeDescuento() { return porcentajeDescuento; }

    public final void setPorcentajeDescuento(double porcentajeDescuento) throws DatosInvalidosException {
        if (porcentajeDescuento <= 0 || porcentajeDescuento > 1) {
            throw new DatosInvalidosException("El descuento debe estar entre 0 y 1 (por ejemplo 0.2 = 20 %).");
        }
        this.porcentajeDescuento = porcentajeDescuento;
    }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Promocion p && id.equals(p.id));
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return nombre; }
}
