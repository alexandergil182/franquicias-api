package com.franquicias.domain.exception;

/** La operación choca con el estado actual (nombre repetido, edición concurrente...). Se traduce a HTTP 409. */
public class ConflictoException extends DominioException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }

    public static ConflictoException franquiciaDuplicada(String nombre) {
        return new ConflictoException("Ya existe una franquicia llamada '" + nombre + "'");
    }

    public static ConflictoException sucursalDuplicada(String nombre) {
        return new ConflictoException("La franquicia ya tiene una sucursal llamada '" + nombre + "'");
    }

    public static ConflictoException productoDuplicado(String nombre) {
        return new ConflictoException("La sucursal ya tiene un producto llamado '" + nombre + "'");
    }
}
