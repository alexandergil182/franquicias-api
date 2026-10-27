package com.franquicias.domain.model;

import com.franquicias.domain.exception.DatoInvalidoException;
import java.util.UUID;

/** Producto ofertado en una sucursal. Es inmutable: cada cambio devuelve una copia nueva. */
public record Producto(String id, String nombre, int stock) {

    public Producto {
        nombre = Nombres.limpiar(nombre);
        if (stock < 0) {
            throw new DatoInvalidoException("El stock no puede ser negativo");
        }
    }

    /** Crea un producto nuevo con un id generado. */
    public static Producto nuevo(String nombre, int stock) {
        return new Producto(UUID.randomUUID().toString(), nombre, stock);
    }

    public Producto conNombre(String nuevoNombre) {
        return new Producto(id, nuevoNombre, stock);
    }

    public Producto conStock(int nuevoStock) {
        return new Producto(id, nombre, nuevoStock);
    }

    public boolean tieneNombre(String otroNombre) {
        return nombre.equalsIgnoreCase(otroNombre.trim());
    }
}
