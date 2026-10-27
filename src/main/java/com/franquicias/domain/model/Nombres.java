package com.franquicias.domain.model;

import com.franquicias.domain.exception.DatoInvalidoException;

/** Reglas comunes para los nombres de franquicias, sucursales y productos. */
final class Nombres {

    private Nombres() {
    }

    /** Quita espacios sobrantes y rechaza nombres nulos o vacíos. */
    static String limpiar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatoInvalidoException("El nombre no puede estar vacío");
        }
        return nombre.trim();
    }
}
