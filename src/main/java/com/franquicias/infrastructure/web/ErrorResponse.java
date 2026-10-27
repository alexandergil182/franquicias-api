package com.franquicias.infrastructure.web;

import java.time.Instant;
import java.util.List;

/** Formato único de error para toda la API. */
public record ErrorResponse(Instant fecha, int estado, String error, String mensaje, String ruta,
                            List<String> detalles) {
}
