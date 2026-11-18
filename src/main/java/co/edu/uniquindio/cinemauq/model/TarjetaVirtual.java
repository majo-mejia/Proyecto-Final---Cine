package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.exceptions.SaldoInsuficienteException;
import co.edu.uniquindio.cinemauq.exceptions.TarjetaInactivaException;
import co.edu.uniquindio.cinemauq.model.enums.EstadoTarjeta;
import co.edu.uniquindio.cinemauq.model.enums.TipoMovimiento;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TarjetaVirtual {

    private final String numero;
    private double saldo;
    private EstadoTarjeta estado = EstadoTarjeta.ACTIVA;
    private final List<MovimientoTarjeta> listaMovimientos = new ArrayList<>();

    public TarjetaVirtual(String numero, double saldoInicial) {
        this.numero = numero;
        this.saldo = saldoInicial;
    }

    public boolean tieneSaldoSuficiente(double monto) {
        return saldo >= monto;
    }

    public boolean estaActiva() {
        return estado == EstadoTarjeta.ACTIVA;
    }

    public void validarPago(double monto) throws TarjetaInactivaException, SaldoInsuficienteException {
        if (!estaActiva()) {
            throw new TarjetaInactivaException("La tarjeta " + numero + " está inactiva.");
        }
        if (!tieneSaldoSuficiente(monto)) {
            throw new SaldoInsuficienteException(String.format(
                    "Saldo insuficiente: se requieren $%,.0f y hay $%,.0f. Puede solicitar una recarga.", monto, saldo));
        }
    }

    /** Saca dinero de la tarjeta (pago de una compra). Registra un movimiento PAGO. */
    public MovimientoTarjeta debitar(double monto, String descripcion, LocalDateTime fechaHora)
            throws TarjetaInactivaException, SaldoInsuficienteException, DatosInvalidosException {
        validarMontoPositivo(monto);
        validarPago(monto);
        saldo -= monto;
        return registrar(TipoMovimiento.PAGO, monto, descripcion, fechaHora);
    }

    /**
     * Mete dinero en la tarjeta. El tipo dice por qué entra:
     * RECARGA exige la tarjeta activa; REEMBOLSO se acepta aunque esté inactiva porque el dinero
     * es del cliente. PAGO no es un crédito, así que se rechaza.
     */
    public MovimientoTarjeta acreditar(double monto, TipoMovimiento tipo, String descripcion, LocalDateTime fechaHora)
            throws TarjetaInactivaException, DatosInvalidosException {
        validarMontoPositivo(monto);
        if (tipo == TipoMovimiento.PAGO) {
            throw new DatosInvalidosException("Un pago no se acredita; use debitar.");
        }
        if (tipo == TipoMovimiento.RECARGA && !estaActiva()) {
            throw new TarjetaInactivaException("No se puede recargar la tarjeta inactiva " + numero + ".");
        }
        saldo += monto;
        return registrar(tipo, monto, descripcion, fechaHora);
    }

    public List<MovimientoTarjeta> obtenerMovimientos() { return Collections.unmodifiableList(listaMovimientos); }

    private MovimientoTarjeta registrar(TipoMovimiento tipo, double monto, String descripcion, LocalDateTime fechaHora) {
        MovimientoTarjeta m = new MovimientoTarjeta(fechaHora, tipo, monto, saldo, descripcion);
        listaMovimientos.add(m);
        return m;
    }

    private static void validarMontoPositivo(double monto) throws DatosInvalidosException {
        if (monto <= 0) {
            throw new DatosInvalidosException("El monto debe ser mayor que cero.");
        }
    }

    public void activar() { estado = EstadoTarjeta.ACTIVA; }
    public void bloquear() { estado = EstadoTarjeta.INACTIVA; }

    public String getNumero() { return numero; }
    public double getSaldo() { return saldo; }
    public EstadoTarjeta getEstado() { return estado; }
}
