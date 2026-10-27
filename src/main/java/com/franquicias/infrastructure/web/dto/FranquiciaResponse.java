package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.Franquicia;
import java.util.List;

public record FranquiciaResponse(String id, String nombre, List<SucursalResponse> sucursales) {

    public static FranquiciaResponse desde(Franquicia f) {
        return new FranquiciaResponse(f.id(), f.nombre(), f.sucursales().stream().map(SucursalResponse::desde).toList());
    }
}
