package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.ProductoDeSucursal;

/** Un producto con más stock y la sucursal a la que pertenece. */
public record ProductoSucursalResponse(String sucursalId, String sucursalNombre, ProductoResponse producto) {

    public static ProductoSucursalResponse desde(ProductoDeSucursal p) {
        return new ProductoSucursalResponse(p.sucursalId(), p.sucursalNombre(), ProductoResponse.desde(p.producto()));
    }
}
