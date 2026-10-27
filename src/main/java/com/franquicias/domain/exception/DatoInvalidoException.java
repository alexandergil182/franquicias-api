package com.franquicias.domain.exception;

/** Un dato incumple una regla de negocio (nombre vacío, stock negativo...). Se traduce a HTTP 400. */
public class DatoInvalidoException extends DominioException {

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
