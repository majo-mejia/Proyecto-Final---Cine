package co.edu.uniquindio.cinemauq.exceptions;

public class CinemaUQException extends Exception {

    public CinemaUQException(String mensaje) {
        super(mensaje);
    }

    public CinemaUQException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
