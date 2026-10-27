package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.Producto;

public record ProductoResponse(String id, String nombre, int stock) {

    public static ProductoResponse desde(Producto p) {
        return new ProductoResponse(p.id(), p.nombre(), p.stock());
    }
}
