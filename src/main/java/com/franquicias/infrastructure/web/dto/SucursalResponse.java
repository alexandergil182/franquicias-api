package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.Sucursal;
import java.util.List;

public record SucursalResponse(String id, String nombre, List<ProductoResponse> productos) {

    public static SucursalResponse desde(Sucursal s) {
        return new SucursalResponse(s.id(), s.nombre(), s.productos().stream().map(ProductoResponse::desde).toList());
    }
}
