package co.edu.uniquindio.cinemauq.patrones.comportamiento.observer;

import co.edu.uniquindio.cinemauq.model.Funcion;

/**
 * Patrón OBSERVER.
 * Problema: cuando el cine cancela una función hay que avisar a todos los clientes que compraron.
 * Funcion (el sujeto) guarda una lista de observadores y solo les avisa del cambio; no necesita
 * saber quiénes son ni qué hacen con el aviso. Cliente implementa esta interfaz.
 */
public interface Observador {

    void actualizar(Funcion funcion);
}
