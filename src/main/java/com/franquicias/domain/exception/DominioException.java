package com.franquicias.domain.exception;

/** Base de los errores de negocio. No depende de HTTP: la capa web decide qué código devolver. */
public abstract class DominioException extends RuntimeException {

    protected DominioException(String mensaje) {
        super(mensaje);
    }
}
