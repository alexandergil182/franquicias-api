package com.franquicias.domain.model;

/** Resultado de la consulta "producto con más stock": el producto y la sucursal a la que pertenece. */
public record ProductoDeSucursal(String sucursalId, String sucursalNombre, Producto producto) {
}
