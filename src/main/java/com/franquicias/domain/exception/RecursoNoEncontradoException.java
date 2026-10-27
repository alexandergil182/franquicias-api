package com.franquicias.domain.exception;

/** El recurso pedido no existe (se traduce a HTTP 404). */
public class RecursoNoEncontradoException extends DominioException {

    private RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static RecursoNoEncontradoException franquicia(String id) {
        return new RecursoNoEncontradoException("No existe la franquicia con id " + id);
    }

    public static RecursoNoEncontradoException sucursal(String id) {
        return new RecursoNoEncontradoException("No existe la sucursal con id " + id);
    }

    public static RecursoNoEncontradoException producto(String id) {
        return new RecursoNoEncontradoException("No existe el producto con id " + id);
    }
}
